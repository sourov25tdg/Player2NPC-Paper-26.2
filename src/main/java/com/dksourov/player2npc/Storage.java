package com.dksourov.player2npc;

import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public final class Storage {

    private final File file;

    public Storage(File file) {
        this.file = file;
    }

    public Map<UUID, Companion> load() {

        Map<UUID, Companion> out = new LinkedHashMap<>();

        if (!file.exists()) {
            return out;
        }

        YamlConfiguration y =
                YamlConfiguration.loadConfiguration(file);

        ConfigurationSection cs =
                y.getConfigurationSection("companions");

        if (cs == null) {
            return out;
        }

        for (String key : cs.getKeys(false)) {

            try {

                UUID id = UUID.fromString(key);

                ConfigurationSection s =
                        cs.getConfigurationSection(key);

                if (s == null) {
                    continue;
                }

                String ownerString = s.getString("owner");

                if (ownerString == null) {
                    continue;
                }

                UUID owner = UUID.fromString(ownerString);

                Location loc = s.getLocation("location");

                if (loc == null) {
                    continue;
                }

                Companion c = new Companion(
                        id,
                        owner,
                        s.getString("name", "Companion"),
                        loc
                );

                c.skin = s.getString("skin", "");

                try {
                    c.task = Companion.Task.valueOf(
                            s.getString("task", "IDLE")
                    );
                } catch (Exception ignored) {
                    c.task = Companion.Task.IDLE;
                }

                c.memory.addAll(
                        s.getStringList("memory")
                );

                c.inventory.addAll(
                        s.getStringList("inventory")
                );

                ConfigurationSection wp =
                        s.getConfigurationSection("waypoints");

                if (wp != null) {

                    for (String name : wp.getKeys(false)) {

                        Location waypoint =
                                wp.getLocation(name);

                        if (waypoint != null) {
                            c.waypoints.put(
                                    name,
                                    waypoint
                            );
                        }
                    }
                }

                out.put(id, c);

            } catch (Exception ignored) {
            }
        }

        return out;
    }

    public void save(Collection<Companion> list) {

        YamlConfiguration y =
                new YamlConfiguration();

        for (Companion c : list) {

            String path =
                    "companions." + c.id;

            y.set(
                    path + ".owner",
                    c.owner.toString()
            );

            y.set(
                    path + ".name",
                    c.name
            );

            y.set(
                    path + ".skin",
                    c.skin
            );

            y.set(
                    path + ".task",
                    c.task.name()
            );

            y.set(
                    path + ".location",
                    c.location
            );

            y.set(
                    path + ".memory",
                    c.memory
            );

            y.set(
                    path + ".inventory",
                    c.inventory
            );

            for (Map.Entry<String, Location> entry :
                    c.waypoints.entrySet()) {

                y.set(
                        path + ".waypoints." + entry.getKey(),
                        entry.getValue()
                );
            }
        }

        try {

            File parent = file.getParentFile();

            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }

            y.save(file);

        } catch (IOException e) {

            throw new RuntimeException(
                    "Could not save companions.yml",
                    e
            );
        }
    }
}
