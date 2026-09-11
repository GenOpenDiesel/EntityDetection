package de.themoep.entitydetection.commands;

import de.themoep.entitydetection.EntityDetection;
import de.themoep.entitydetection.searcher.EntitySearch;
import de.themoep.entitydetection.searcher.SearchType;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.TileState;
import org.bukkit.block.data.BlockData;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
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
public class SearchSubCommand extends SubCommand {
    private final Method createBlockStateMethod;

    public SearchSubCommand(EntityDetection plugin) {
        super(plugin, plugin.getName().toLowerCase(), "search",
                "[monster|passive|misc|block|tile|entity|all|<type>]"
        );
        createBlockStateMethod = findCreateBlockStateMethod();
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        EntitySearch search = new EntitySearch(getPlugin(), sender);
        if(args.length > 0) {
            for(String arg : args) {
                if ("--regions".equalsIgnoreCase(arg)) {
                    Plugin plugin = Bukkit.getPluginManager().getPlugin("WorldGuard");
                    if (plugin != null && plugin.isEnabled() && plugin.getDescription().getVersion().startsWith("7"))
                        search.setWorldGuardRegion(true);
                    else {
                        sender.sendMessage(ChatColor.RED + "Unable to start WorldGuard search. WorldGuard not enabled or outdated!");
                        return true;
                    }
                    if (args.length == 1) search.setType(SearchType.MONSTER);
                    continue;
                }
                if (arg.endsWith("s")) {
                    arg = arg.substring(0, arg.length() - 1);
                }
                boolean found = false;
                if (!found) {
                    try {
                        search.addSearchedType(EntityType.valueOf(arg.toUpperCase()));
                        found = true;
                    } catch (IllegalArgumentException ignored) {}
                }
                if (!found) {
                    try {
                        search.addSearchedBlockState(Class.forName("org.bukkit.block." + arg, false, getPlugin().getServer().getClass().getClassLoader())); //TODO: This is case sensitive :(
                        found = true;
                    } catch (ClassNotFoundException ignored) {}
                }
                if (!found) {
                    try {
                        search.setType(SearchType.valueOf(arg.toUpperCase()));
                        found = true;
                    } catch (IllegalArgumentException ignored) {}
                }
                if (!found) {
                    try {
                        search.setType(SearchType.getByAlias(arg.toUpperCase()));
                        found = true;
                    } catch(IllegalArgumentException ignored) {}
                }
                if (!found) {
                    try {
                        search.addSearchedMaterial(Material.valueOf(arg.toUpperCase())); //TODO: This doesn't check for tile entities
                        found = true;
                    } catch (IllegalArgumentException ignored) {}
                }
                if (!found) {
                    return false;
                }
            }
        } else {
            search.setType(SearchType.MONSTER);
        }
        if(!getPlugin().startSearch(search)) {
            sender.sendMessage(ChatColor.YELLOW + search.getOwner() + ChatColor.RED + " already started a search!");
        }
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        String current = args.length == 0 ? "" : args[args.length - 1].toLowerCase(Locale.ROOT);
        Set<String> candidates = new LinkedHashSet<>();

        candidates.add("--regions");
        for (SearchType type : SearchType.values()) {
            if (type != SearchType.CUSTOM) {
                candidates.add(type.name().toLowerCase(Locale.ROOT));
            }
            for (String alias : type.getAliases()) {
                candidates.add(alias.toLowerCase(Locale.ROOT));
            }
        }
        for (EntityType type : EntityType.values()) {
            candidates.add(type.name().toLowerCase(Locale.ROOT));
        }
        for (Material material : Material.values()) {
            if (material.isBlock() && !material.isLegacy() && isTileEntityMaterial(material)) {
                candidates.add(material.name().toLowerCase(Locale.ROOT));
            }
        }

        if (args.length > 1) {
            for (int i = 0; i < args.length - 1; i++) {
                candidates.remove(args[i].toLowerCase(Locale.ROOT));
            }
        }

        List<String> suggestions = new ArrayList<>();
        for (String candidate : candidates) {
            if (candidate.startsWith(current)) {
                suggestions.add(candidate);
            }
        }
        suggestions.sort(Comparator.naturalOrder());
        return suggestions;
    }

    private Method findCreateBlockStateMethod() {
        try {
            return BlockData.class.getMethod("createBlockState");
        } catch (NoSuchMethodException ignored) {
            // BlockData#createBlockState was added after the oldest supported Paper version.
            return null;
        }
    }

    private boolean isTileEntityMaterial(Material material) {
        if (createBlockStateMethod == null) {
            // Preserve completion support on older servers where the API cannot expose this information.
            return true;
        }

        try {
            return createBlockStateMethod.invoke(material.createBlockData()) instanceof TileState;
        } catch (IllegalAccessException | InvocationTargetException | IllegalArgumentException ignored) {
            // Do not hide a valid material if another server implementation cannot create its state here.
            return true;
        }
    }
}
