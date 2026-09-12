package com.dksourov.player2npc;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.Collections;
import java.util.List;

public final class P2NPCTabCompleter
        implements TabCompleter {

    @Override
    public List<String> onTabComplete(
            CommandSender sender,
            Command command,
            String alias,
            String[] args
    ) {

        if (args.length == 1) {

            return List.of(
                    "create",
                    "remove",
                    "list",
                    "follow",
                    "stop",
                    "task",
                    "waypoint",
                    "memory",
                    "telegram",
                    "reload"
            );
        }

        if (
                args.length == 2 &&
                args[0].equalsIgnoreCase("telegram")
        ) {

            return List.of("pair");
        }

        if (
                args.length == 3 &&
                args[0].equalsIgnoreCase("task")
        ) {

            return List.of(
                    "combat",
                    "mining",
                    "farming",
                    "crafting",
                    "smelting"
            );
        }

        return Collections.emptyList();
    }
}
