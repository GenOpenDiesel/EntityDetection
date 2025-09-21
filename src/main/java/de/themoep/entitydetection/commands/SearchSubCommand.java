package de.themoep.entitydetection.commands;

import de.themoep.entitydetection.EntityDetection;
import de.themoep.entitydetection.searcher.EntitySearch;
import de.themoep.entitydetection.searcher.SearchType;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
    public SearchSubCommand(EntityDetection plugin) {
        super(plugin, plugin.getName().toLowerCase(), "search",
                "[monster|passive|misc|block|tile|entity|all|<type>] [--world <worldname>] [--exclude <type> ...]"
        );
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        EntitySearch search = new EntitySearch(getPlugin(), sender);
        List<String> searchArgs = new ArrayList<>();
        List<String> excludeArgs = new ArrayList<>();
        String worldName = null;
        boolean exclude = false;
        boolean world = false;
        for (String arg : args) {
            if (arg.equalsIgnoreCase("--exclude")) {
                exclude = true;
                world = false;
                continue;
            }
            if (arg.equalsIgnoreCase("--world")) {
                world = true;
                exclude = false;
                continue;
            }
            if(arg.equalsIgnoreCase("--regions")) {
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

            if (exclude) {
                excludeArgs.add(arg);
            } else if (world) {
                worldName = arg;
                world = false;
            } else {
                searchArgs.add(arg);
            }
        }

        for (String arg : excludeArgs) {
            if (arg.endsWith("s")) {
                arg = arg.substring(0, arg.length() - 1);
            }
            boolean found = false;
            if (!found) {
                try {
                    search.addExcludedType(EntityType.valueOf(arg.toUpperCase()));
                    found = true;
                } catch (IllegalArgumentException ignored) {}
            }
            if (!found) {
                try {
                    search.addExcludedBlockState(Class.forName("org.bukkit.block." + arg, false, getPlugin().getServer().getClass().getClassLoader()));
                    found = true;
                } catch (ClassNotFoundException ignored) {}
            }
            if (!found) {
                try {
                    search.addExcludedMaterial(Material.valueOf(arg.toUpperCase()));
                    found = true;
                } catch (IllegalArgumentException ignored) {}
            }
            if (!found) {
                sender.sendMessage(ChatColor.RED + "Could not find a type to exclude for '" + arg + "'!");
            }
        }


        if (worldName != null) {
            if (getPlugin().getServer().getWorld(worldName) != null) {
                search.setWorld(worldName);
            } else {
                sender.sendMessage(ChatColor.RED + "World '" + worldName + "' not found!");
                return true;
            }
        }


        if(searchArgs.size() > 0) {
            for(String arg : searchArgs) {
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
}
