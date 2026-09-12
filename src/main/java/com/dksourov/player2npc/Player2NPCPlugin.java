package com.dksourov.player2npc;

import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public final class Player2NPCPlugin
        extends JavaPlugin {

    private CompanionManager companions;
    private Storage storage;
    private TelegramService telegramService;

    @Override
    public void onEnable() {

        saveDefaultConfig();

        File dataFolder =
                getDataFolder();

        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }

        storage =
                new Storage(
                        new File(
                                dataFolder,
                                "companions.yml"
                        )
                );

        companions =
                new CompanionManager(
                        this,
                        storage
                );

        companions.load();

        companions.spawnLoaded();

        getServer()
                .getPluginManager()
                .registerEvents(
                        new CompanionListener(
                                companions
                        ),
                        this
                );

        if (getCommand("p2npc") != null) {

            getCommand("p2npc")
                    .setExecutor(
                            new P2NPCCommand(
                                    this,
                                    companions
                            )
                    );

            getCommand("p2npc")
                    .setTabCompleter(
                            new P2NPCTabCompleter()
                    );
        }

        telegramService =
                new TelegramService(
                        this,
                        companions
                );

        telegramService.start();

        long saveTicks =
                Math.max(
                        20L,
                        getConfig().getLong(
                                "storage.save-interval-seconds",
                                30L
                        ) * 20L
                );

        getServer()
                .getScheduler()
                .runTaskTimer(
                        this,
                        companions::tick,
                        5L,
                        5L
                );

        getServer()
                .getScheduler()
                .runTaskTimer(
                        this,
                        companions::save,
                        saveTicks,
                        saveTicks
                );

        getLogger().info(
                "Player2NPC Paper 26.2 enabled."
        );
    }

    @Override
    public void onDisable() {

        if (telegramService != null) {
            telegramService.stop();
        }

        if (companions != null) {
            companions.save();
        }
    }

    public CompanionManager companions() {
        return companions;
    }

    public TelegramService getTelegramService() {
        return telegramService;
    }
}
