package com.dksourov.player2npc;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.Locale;

public final class P2NPCCommand
        implements CommandExecutor {

    private final Player2NPCPlugin plugin;
    private final CompanionManager manager;

    public P2NPCCommand(
            Player2NPCPlugin plugin,
            CompanionManager manager
    ) {
        this.plugin = plugin;
        this.manager = manager;
    }

    private void msg(
            CommandSender sender,
            String text
    ) {

        sender.sendMessage(
                "§6[Player2NPC] §f" + text
        );
    }

    @Override
    public boolean onCommand(
            CommandSender sender,
            Command command,
            String label,
            String[] args
    ) {

        if (args.length == 0) {

            msg(
                    sender,
                    "/p2npc create <name> | " +
                    "remove <name> | list | " +
                    "follow <name> | stop <name> | " +
                    "task <name> <task> | " +
                    "waypoint <name> <wp> | " +
                    "memory <name> <text> | " +
                    "telegram pair"
            );

            return true;
        }

        switch (args[0].toLowerCase(Locale.ROOT)) {

            case "create":

                if (!(sender instanceof Player player)) {
                    msg(sender, "Player only.");
                    return true;
                }

                if (args.length < 2) {
                    msg(sender, "Name required.");
                    return true;
                }

                manager.create(
                        player,
                        args[1]
                );

                msg(
                        sender,
                        "Created companion §e" +
                        args[1]
                );

                return true;

            case "remove":

                if (args.length < 2) {
                    msg(
                            sender,
                            "Usage: /p2npc remove <name>"
                    );
                    return true;
                }

                manager.remove(args[1]);

                msg(
                        sender,
                        "Removed companion if it existed."
                );

                return true;

            case "list":

                if (manager.all().isEmpty()) {

                    msg(
                            sender,
                            "No companions found."
                    );

                    return true;
                }

                manager.all().forEach(
                        companion ->
                                msg(
                                        sender,
                                        companion.name +
                                        " §7(" +
                                        companion.task +
                                        ")"
                                )
                );

                return true;

            case "follow":

                if (args.length < 2) {
                    msg(
                            sender,
                            "Usage: /p2npc follow <name>"
                    );
                    return true;
                }

                manager.task(
                        args[1],
                        Companion.Task.FOLLOW
                );

                msg(
                        sender,
                        "Follow enabled."
                );

                return true;

            case "stop":

                if (args.length < 2) {
                    msg(
                            sender,
                            "Usage: /p2npc stop <name>"
                    );
                    return true;
                }

                manager.task(
                        args[1],
                        Companion.Task.IDLE
                );

                msg(
                        sender,
                        "Companion stopped."
                );

                return true;

            case "task":

                if (args.length < 3) {

                    msg(
                            sender,
                            "Usage: /p2npc task <name> " +
                            "<combat|mining|farming|crafting|smelting>"
                    );

                    return true;
                }

                try {

                    Companion.Task task =
                            Companion.Task.valueOf(
                                    args[2].toUpperCase(
                                            Locale.ROOT
                                    )
                            );

                    manager.task(
                            args[1],
                            task
                    );

                    msg(
                            sender,
                            "Task set to §e" +
                            task.name()
                    );

                } catch (IllegalArgumentException e) {

                    msg(
                            sender,
                            "Unknown task."
                    );
                }

                return true;

            case "waypoint":

                if (!(sender instanceof Player player)) {
                    msg(sender, "Player only.");
                    return true;
                }

                if (args.length < 3) {

                    msg(
                            sender,
                            "Usage: /p2npc waypoint <name> <waypoint>"
                    );

                    return true;
                }

                manager.waypoint(
                        args[1],
                        args[2],
                        player.getLocation()
                );

                msg(
                        sender,
                        "Waypoint saved."
                );

                return true;

            case "memory":

                if (args.length < 3) {

                    msg(
                            sender,
                            "Usage: /p2npc memory <name> <text>"
                    );

                    return true;
                }

                manager.memory(
                        args[1],
                        String.join(
                                " ",
                                Arrays.copyOfRange(
                                        args,
                                        2,
                                        args.length
                                )
                        )
                );

                msg(
                        sender,
                        "Memory saved."
                );

                return true;

            case "telegram":

                if (!(sender instanceof Player player)) {
                    msg(sender, "Player only.");
                    return true;
                }

                if (args.length < 2 ||
                        !args[1].equalsIgnoreCase("pair")) {

                    msg(
                            sender,
                            "Usage: /p2npc telegram pair"
                    );

                    return true;
                }

                if (plugin.getTelegramService() == null) {

                    msg(
                            sender,
                            "Telegram integration is disabled."
                    );

                    return true;
                }

                String code =
                        plugin.getTelegramService()
                                .createPairCode(player);

                msg(
                        sender,
                        "Your Telegram pairing code is §e" +
                        code
                );

                msg(
                        sender,
                        "Send §e/pair " +
                        code +
                        "§f to your Telegram bot."
                );

                return true;

            case "reload":

                plugin.reloadConfig();

                msg(
                        sender,
                        "Config reloaded."
                );

                return true;

            default:

                msg(
                        sender,
                        "Unknown command."
                );

                return true;
        }
    }
}
