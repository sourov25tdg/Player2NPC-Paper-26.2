package com.dksourov.player2npc;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class TelegramService {

    private final Player2NPCPlugin plugin;
    private final CompanionManager manager;

    private final HttpClient http =
            HttpClient.newBuilder()
                    .connectTimeout(
                            Duration.ofSeconds(15)
                    )
                    .build();

    private final ExecutorService executor =
            Executors.newSingleThreadExecutor(
                    runnable -> {

                        Thread thread =
                                new Thread(
                                        runnable,
                                        "Player2NPC-Telegram"
                                );

                        thread.setDaemon(true);

                        return thread;
                    }
            );

    private final Map<String, UUID> pairCodes =
            new ConcurrentHashMap<>();

    private final Map<Long, UUID> linkedChats =
            new ConcurrentHashMap<>();

    private volatile boolean running;
    private volatile long offset;

    private String token;

    public TelegramService(
            Player2NPCPlugin plugin,
            CompanionManager manager
    ) {

        this.plugin = plugin;
        this.manager = manager;
    }

    public void start() {

        token =
                plugin.getConfig()
                        .getString(
                                "telegram.bot-token",
                                ""
                        )
                        .trim();

        boolean enabled =
                plugin.getConfig()
                        .getBoolean(
                                "telegram.enabled",
                                false
                        );

        if (!enabled || token.isEmpty()) {

            plugin.getLogger().info(
                    "Telegram integration disabled."
            );

            return;
        }

        running = true;

        executor.submit(
                this::pollLoop
        );

        plugin.getLogger().info(
                "Telegram bot integration started."
        );
    }

    public void stop() {

        running = false;

        executor.shutdownNow();
    }

    public String createPairCode(
            Player player
    ) {

        String code =
                String.format(
                        Locale.ROOT,
                        "%06d",
                        new Random()
                                .nextInt(1_000_000)
                );

        pairCodes.put(
                code,
                player.getUniqueId()
        );

        return code;
    }

    public String linkedName(
            long chatId
    ) {

        UUID uuid =
                linkedChats.get(chatId);

        if (uuid == null) {
            return "offline";
        }

        Player player =
                Bukkit.getPlayer(uuid);

        if (player == null) {
            return "offline";
        }

        return player.getName();
    }

    private void pollLoop() {

        while (running) {

            try {

                JsonObject body =
                        new JsonObject();

                body.addProperty(
                        "timeout",
                        25
                );

                body.addProperty(
                        "offset",
                        offset
                );

                JsonObject response =
                        call(
                                "getUpdates",
                                body
                        );

                JsonArray updates =
                        response
                                .getAsJsonArray(
                                        "result"
                                );

                for (
                        JsonElement element :
                        updates
                ) {

                    JsonObject update =
                            element.getAsJsonObject();

                    offset =
                            update
                                    .get("update_id")
                                    .getAsLong() + 1;

                    handle(update);
                }

            } catch (Exception e) {

                plugin.getLogger().warning(
                        "Telegram polling error: " +
                        e.getMessage()
                );

                try {

                    Thread.sleep(3000);

                } catch (
                        InterruptedException ignored
                ) {

                    return;
                }
            }
        }
    }

    private void handle(
            JsonObject update
    ) {

        if (!update.has("message")) {
            return;
        }

        JsonObject message =
                update.getAsJsonObject(
                        "message"
                );

        long chatId =
                message
                        .getAsJsonObject("chat")
                        .get("id")
                        .getAsLong();

        String text =
                message.has("text")
                        ? message.get("text")
                                .getAsString()
                                .trim()
                        : "";

        if (text.isEmpty()) {
            return;
        }

        if (text.startsWith("/pair ")) {

            String code =
                    text.substring(6).trim();

            UUID owner =
                    pairCodes.remove(code);

            if (owner == null) {

                send(
                        chatId,
                        "❌ Invalid or expired pairing code."
                );

                return;
            }

            linkedChats.put(
                    chatId,
                    owner
            );

            send(
                    chatId,
                    "✅ Telegram linked to Minecraft player **" +
                    linkedName(chatId) +
                    "**."
            );

            return;
        }

        UUID owner =
                linkedChats.get(chatId);

        if (
                text.equals("/start") ||
                text.equals("/help")
        ) {

            send(
                    chatId,
                    """
                    Player2NPC Bot
                    
                    /p2npc create <name>
                    /p2npc list
                    /p2npc follow <name>
                    /p2npc stop <name>
                    /p2npc remove <name>
                    /p2npc task <name> <task>
                    /p2npc waypoint <name> <waypoint>
                    /p2npc memory <name> <text>
                    
                    In Minecraft:
                    /p2npc telegram pair
                    
                    Then send:
                    /pair CODE
                    """
            );

            return;
        }

        if (owner == null) {

            send(
                    chatId,
                    "🔒 Link this chat first. " +
                    "In Minecraft run " +
                    "/p2npc telegram pair"
            );

            return;
        }

        if (text.startsWith("/p2npc ")) {

            execute(
                    chatId,
                    owner,
                    text.substring(7).trim()
            );

        } else if (text.startsWith("/npc ")) {

            execute(
                    chatId,
                    owner,
                    text.substring(5).trim()
            );
        }
    }

    private void execute(
            long chatId,
            UUID owner,
            String command
    ) {

        String[] args =
                command.split("\\s+");

        if (args.length == 0) {
            return;
        }

        Bukkit.getScheduler()
                .runTask(
                        plugin,
                        () -> {

                            Player player =
                                    Bukkit.getPlayer(owner);

                            String output;

                            try {

                                switch (
                                        args[0]
                                                .toLowerCase(
                                                        Locale.ROOT
                                                )
                                ) {

                                    case "create":

                                        if (player == null) {

                                            output =
                                                    "❌ Player is offline.";

                                            break;
                                        }

                                        if (args.length < 2) {

                                            output =
                                                    "Usage: /p2npc create <name>";

                                            break;
                                        }

                                        manager.create(
                                                player,
                                                args[1]
                                        );

                                        output =
                                                "✅ Created NPC **" +
                                                args[1] +
                                                "**.";

                                        break;

                                    case "list":

                                        output =
                                                manager.all()
                                                        .stream()
                                                        .map(
                                                                c ->
                                                                        c.name +
                                                                        " (" +
                                                                        c.task +
                                                                        ")"
                                                        )
                                                        .reduce(
                                                                (a, b) ->
                                                                        a + ", " + b
                                                        )
                                                        .orElse(
                                                                "No NPCs."
                                                        );

                                        break;

                                    case "follow":

                                        if (args.length < 2) {

                                            output =
                                                    "Usage: /p2npc follow <name>";

                                            break;
                                        }

                                        manager.task(
                                                args[1],
                                                Companion.Task.FOLLOW
                                        );

                                        output =
                                                "▶️ " +
                                                args[1] +
                                                " is following.";

                                        break;

                                    case "stop":

                                        if (args.length < 2) {

                                            output =
                                                    "Usage: /p2npc stop <name>";

                                            break;
                                        }

                                        manager.task(
                                                args[1],
                                                Companion.Task.IDLE
                                        );

                                        output =
                                                "⏹ " +
                                                args[1] +
                                                " stopped.";

                                        break;

                                    case "remove":

                                        if (args.length < 2) {

                                            output =
                                                    "Usage: /p2npc remove <name>";

                                            break;
                                        }

                                        manager.remove(
                                                args[1]
                                        );

                                        output =
                                                "🗑 Removed **" +
                                                args[1] +
                                                "**.";

                                        break;

                                    case "task":

                                        if (args.length < 3) {

                                            output =
                                                    "Usage: /p2npc task <name> <task>";

                                            break;
                                        }

                                        Companion.Task task =
                                                Companion.Task.valueOf(
                                                        args[2]
                                                                .toUpperCase(
                                                                        Locale.ROOT
                                                                )
                                                );

                                        manager.task(
                                                args[1],
                                                task
                                        );

                                        output =
                                                "⚙️ Task set to " +
                                                task.name() +
                                                ".";

                                        break;

                                    case "waypoint":

                                        if (
                                                args.length < 3 ||
                                                player == null
                                        ) {

                                            output =
                                                    "Usage: /p2npc waypoint <name> <waypoint>";

                                            break;
                                        }

                                        manager.waypoint(
                                                args[1],
                                                args[2],
                                                player.getLocation()
                                        );

                                        output =
                                                "📍 Waypoint saved.";

                                        break;

                                    case "memory":

                                        if (args.length < 3) {

                                            output =
                                                    "Usage: /p2npc memory <name> <text>";

                                            break;
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

                                        output =
                                                "🧠 Memory saved.";

                                        break;

                                    default:

                                        output =
                                                "❌ Unknown command.";
                                }

                            } catch (Exception e) {

                                output =
                                        "❌ " +
                                        e.getMessage();
                            }

                            send(
                                    chatId,
                                    output
                            );
                        }
                );
    }

    private JsonObject call(
            String method,
            JsonObject body
    ) throws Exception {

        HttpRequest request =
                HttpRequest.newBuilder(
                                URI.create(
                                        "https://api.telegram.org/bot" +
                                        token +
                                        "/" +
                                        method
                                )
                        )
                        .timeout(
                                Duration.ofSeconds(35)
                        )
                        .header(
                                "Content-Type",
                                "application/json"
                        )
                        .POST(
                                HttpRequest.BodyPublishers
                                        .ofString(
                                                body.toString()
                                        )
                        )
                        .build();

        HttpResponse<String> response =
                http.send(
                        request,
                        HttpResponse.BodyHandlers
                                .ofString(
                                        StandardCharsets.UTF_8
                                )
                );

        JsonObject result =
                new Gson().fromJson(
                        response.body(),
                        JsonObject.class
                );

        if (
                !result
                        .get("ok")
                        .getAsBoolean()
        ) {

            throw new IOException(
                    result.toString()
            );
        }

        return result;
    }

    private void send(
            long chatId,
            String text
    ) {

        try {

            JsonObject body =
                    new JsonObject();

            body.addProperty(
                    "chat_id",
                    chatId
            );

            body.addProperty(
                    "text",
                    text
            );

            body.addProperty(
                    "parse_mode",
                    "Markdown"
            );

            call(
                    "sendMessage",
                    body
            );

        } catch (Exception e) {

            plugin.getLogger().warning(
                    "Telegram send failed: " +
                    e.getMessage()
            );
        }
    }
}
