package de.themoep.entitydetection.searcher;

import org.bukkit.block.BlockState;
import org.bukkit.entity.Ambient;
import org.bukkit.entity.Animals;
import org.bukkit.entity.Boat;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Golem;
import org.bukkit.entity.Hanging;
import org.bukkit.entity.Item;
import org.bukkit.entity.Minecart;
import org.bukkit.entity.Monster;
import org.bukkit.entity.NPC;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Slime;
import org.bukkit.entity.WaterMob;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * Copyright 2016 Max Lee (https://github.com/Phoenix616/)
 * <p/>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Mozilla Public License as published by
 * the Mozilla Foundation, version 2.
 * <p/>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Mozilla Public License v2.0 for more details.
 * <p/>
 * You should have received a copy of the Mozilla Public License v2.0
 * along with this program. If not, see <http://mozilla.org/MPL/2.0/>.
 */
public enum SearchType {
    MONSTER(
            new Class[]{
                    Monster.class,
                    Slime.class
            }
    ),
    PASSIVE(
            new String[]{"ANIMAL"},
            new Class[]{
                    Animals.class,
                    Ambient.class,
                    NPC.class,
                    WaterMob.class,
                    Golem.class
            }
    ),
    MISC(
            // Names changed over the versions: FIREWORK -> FIREWORK_ROCKET, ENDER_SIGNAL -> EYE_OF_ENDER
            types("FIREWORK_ROCKET", "FIREWORK", "EYE_OF_ENDER", "ENDER_SIGNAL"),
            new Class[]{
                    Projectile.class,
                    Minecart.class,
                    Item.class,
                    Boat.class // Covers the per-wood boat types that replaced EntityType.BOAT in 1.21.3
            }
    ),
    BLOCK(
            // ENDER_CRYSTAL was renamed to END_CRYSTAL in 1.20.5
            types("ARMOR_STAND", "FALLING_BLOCK", "END_CRYSTAL", "ENDER_CRYSTAL"),
            new Class[]{Hanging.class}
    ),
    DISPLAY(
            new String[]{"DISPLAYS", "HOLOGRAM", "DECORATION"},
            // Display entities only exist since 1.19.4, interactions/markers since 1.19.4/1.17
            types("BLOCK_DISPLAY", "ITEM_DISPLAY", "TEXT_DISPLAY", "INTERACTION", "MARKER"),
            classes("Display", "Interaction", "Marker")
    ),
    ENTITY(
            new String[]{"ENTITIES"},
            EntityType.values()
    ),
    TILE(
            new String[]{"BLOCKSTATE"},
            new Class[]{BlockState.class}
    ),
    ALL(
            EntityType.values(),
            new Class[]{BlockState.class}
    ),
    CUSTOM;

    private String[] aliases;

    private EntityType[] entityTypes;
    private Class<?>[] blockStates;

    SearchType(String[] aliases, EntityType[] eTypes, Class[] classes) {
        this.aliases = aliases;
        Set<EntityType> typeSet = new HashSet<EntityType>();
        Collections.addAll(typeSet, eTypes);

        List<Class> classList = new LinkedList<Class>(Arrays.asList(classes));

        if(classes.length > 0) {
            for(EntityType et : EntityType.values()) {
                if(typeSet.contains(et)) {
                    continue;
                }
                Class<? extends Entity> e = et.getEntityClass();
                if(e == null) {
                    continue;
                }
                for (Class eClass : classList) {
                    if (eClass.isAssignableFrom(e)) {
                        typeSet.add(et);
                        break;
                    }
                }
            }
        }
        entityTypes = typeSet.toArray(new EntityType[typeSet.size()]);

        // Only actual block state classes are of use when scanning tile entities. Keeping the entity
        // interfaces in here would make every search walk all tile entities of all loaded chunks
        // without a single one of them ever being able to match.
        List<Class> stateList = new LinkedList<Class>();
        for (Class eClass : classList) {
            if (BlockState.class.isAssignableFrom(eClass)) {
                stateList.add(eClass);
            }
        }
        blockStates = stateList.toArray(new Class[stateList.size()]);
    }

    SearchType(Class<?>[] classes) {
        this(new String[]{}, new EntityType[]{}, classes);
    }

    SearchType(EntityType[] types) {
        this(new String[]{}, types, new Class[]{});
    }

    SearchType(String[] aliases, Class<?>[] classes) {
        this(aliases, new EntityType[]{}, classes);
    }

    SearchType(EntityType[] types, Class<?>[] classes) {
        this(new String[]{}, types, classes);
    }

    SearchType(String[] aliases, EntityType[] entityTypes) {
        this(aliases, entityTypes, new Class[]{});
    }

    SearchType() {
        this(new String[]{}, new EntityType[]{}, new Class[]{});
    }

    /**
     * Look up entity types by name, silently skipping the ones that don't exist on the running server.
     * This plugin is built against an old API version while entity type constants get renamed
     * (e.g. FIREWORK -&gt; FIREWORK_ROCKET) or added (e.g. BLOCK_DISPLAY) in newer versions, so
     * referencing them directly would either not compile or blow up with a NoSuchFieldError.
     *
     * @param names The names to look up, list renamed constants next to each other
     * @return An array of all entity types that exist on this server
     */
    private static EntityType[] types(String... names) {
        List<EntityType> found = new ArrayList<EntityType>();
        for (String name : names) {
            try {
                found.add(EntityType.valueOf(name));
            } catch (IllegalArgumentException ignored) {
                // Doesn't exist on this version
            }
        }
        return found.toArray(new EntityType[found.size()]);
    }

    /**
     * Look up entity interfaces by their simple name, silently skipping the ones that don't exist
     * on the running server. Same reasoning as {@link #types(String...)}.
     *
     * @param names The simple names of interfaces in the org.bukkit.entity package
     * @return An array of all classes that exist on this server
     */
    private static Class<?>[] classes(String... names) {
        List<Class<?>> found = new ArrayList<Class<?>>();
        for (String name : names) {
            try {
                found.add(Class.forName("org.bukkit.entity." + name));
            } catch (ClassNotFoundException ignored) {
                // Doesn't exist on this version
            }
        }
        return found.toArray(new Class<?>[found.size()]);
    }

    /**
     * Get a sub command by its alias
     * @param alias The alias to search for
     * @return The sub command
     * @throws IllegalArgumentException Thrown when there is no sub command with this alias
     */
    public static SearchType getByAlias(String alias) throws IllegalArgumentException{
        for(SearchType type : SearchType.values()) {
            for(String a : type.aliases) {
                if(a.equals(alias)) {
                    return type;
                }
            }
        }
        throw new IllegalArgumentException(alias + " is not an alias of any SearchType.");
    }

    /**
     * Get all the entity types that belong to this search type
     *
     * @return An Array of EntityTypes, CUSTOM's list is empty and should be filled by you per search
     */
    public EntityType[] getEntities() {
        return entityTypes;
    }

    /**
     * Get all the classes of tile entity blockstates that belong to this search type
     *
     * @return An Array of Classes, CUSTOM's list is empty and should be filled by you per search
     */
    public Class<?>[] getBlockStates() {
        return blockStates;
    }
}
