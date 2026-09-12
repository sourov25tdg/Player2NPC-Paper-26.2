package com.dksourov.player2npc;

import io.papermc.paper.datacomponent.item.ResolvableProfile;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Mannequin;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.profile.PlayerProfile;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public final class CompanionManager {

    private final Player2NPCPlugin plugin;
    private final Storage storage;

    private final Map<UUID, Companion> data =
            new LinkedHashMap<>();

    private final Map<UUID, Mannequin> entities =
            new HashMap<>();

    private final NamespacedKey idKey;

    public CompanionManager(
            Player2NPCPlugin plugin,
            Storage storage
    ) {

        this.plugin = plugin;
        this.storage = storage;

        this.idKey =
                new NamespacedKey(
                        plugin,
                        "companion_id"
                );
    }

    public void load() {

        data.clear();

        data.putAll(
                storage.load()
        );
    }

    public void save() {

        for (Companion c : data.values()) {

            Mannequin npc =
                    entities.get(c.id);

            if (npc != null && npc.isValid()) {

                c.location =
                        npc.getLocation().clone();
            }
        }

        storage.save(
                data.values()
        );
    }

    public Collection<Companion> all() {

        return Collections.unmodifiableCollection(
                data.values()
        );
    }

    public Companion create(
            Player player,
            String name
    ) {

        Companion c =
                new Companion(
                        UUID.randomUUID(),
                        player.getUniqueId(),
                        name,
                        player.getLocation().clone()
                );

        c.skin =
                player.getUniqueId().toString();

        data.put(
                c.id,
                c
        );

        spawn(
                c,
                player.getPlayerProfile()
        );

        save();

        return c;
    }

    public Companion find(String name) {

        return data.values()
                .stream()
                .filter(c ->
                        c.name.equalsIgnoreCase(name))
                .findFirst()
                .orElse(null);
    }

    public void remove(String name) {

        Companion c = find(name);

        if (c == null) {
            return;
        }

        despawn(c);

        data.remove(c.id);

        save();
    }

    public void task(
            String name,
            Companion.Task task
    ) {

        Companion c = find(name);

        if (c == null) {
            return;
        }

        c.task = task;

        save();
    }

    public void waypoint(
            String name,
            String waypoint,
            Location location
    ) {

        Companion c = find(name);

        if (c == null) {
            return;
        }

        c.waypoints.put(
                waypoint,
                location.clone()
        );

        save();
    }

    public void memory(
            String name,
            String text
    ) {

        Companion c = find(name);

        if (c == null) {
            return;
        }

        c.memory.add(text);

        while (c.memory.size() > 100) {
            c.memory.remove(0);
        }

        save();
    }

    public void spawnLoaded() {

        for (Companion c : data.values()) {

            Player owner =
                    Bukkit.getPlayer(c.owner);

            if (owner != null) {

                spawn(
                        c,
                        owner.getPlayerProfile()
                );

            } else {

                spawn(
                        c,
                        null
                );
            }
        }
    }

    private void spawn(
            Companion c,
            PlayerProfile ownerProfile
    ) {

        despawn(c);

        if (c.location == null ||
                c.location.getWorld() == null) {
            return;
        }

        Location loc =
                c.location.clone();

        Mannequin npc =
                loc.getWorld().spawn(
                        loc,
                        Mannequin.class,
                        mannequin -> {

                            mannequin.setCustomName(
                                    c.name
                            );

                            mannequin.setCustomNameVisible(
                                    true
                            );

                            mannequin.setImmovable(
                                    false
                            );

                            mannequin
                                    .getPersistentDataContainer()
                                    .set(
                                            idKey,
                                            PersistentDataType.STRING,
                                            c.id.toString()
                                    );

                            if (ownerProfile != null) {

                                mannequin.setProfile(
                                        ResolvableProfile
                                                .resolvableProfile(
                                                        ownerProfile
                                                )
                                );

                            } else if (
                                    c.skin != null &&
                                    !c.skin.isBlank()
                            ) {

                                try {

                                    UUID skinUUID =
                                            UUID.fromString(
                                                    c.skin
                                            );

                                    PlayerProfile profile =
                                            Bukkit.createProfile(
                                                    skinUUID
                                            );

                                    mannequin.setProfile(
                                            ResolvableProfile
                                                    .resolvableProfile(
                                                            profile
                                                    )
                                    );

                                } catch (Exception ignored) {
                                }
                            }
                        }
                );

        entities.put(
                c.id,
                npc
        );
    }

    private void despawn(
            Companion c
    ) {

        Mannequin npc =
                entities.remove(c.id);

        if (npc != null &&
                npc.isValid()) {

            npc.remove();
        }
    }

    public Mannequin entity(
            Companion c
    ) {

        return entities.get(c.id);
    }

    public void tick() {

        for (Companion c :
                data.values()) {

            Mannequin npc =
                    entities.get(c.id);

            if (npc == null ||
                    !npc.isValid()) {

                Player owner =
                        Bukkit.getPlayer(c.owner);

                if (owner != null) {
                    spawn(
                            c,
                            owner.getPlayerProfile()
                    );
                } else {
                    spawn(
                            c,
                            null
                    );
                }

                continue;
            }

            Player owner =
                    Bukkit.getPlayer(c.owner);

            if (owner == null) {
                continue;
            }

            if (
                    c.task == Companion.Task.FOLLOW &&
                    owner.getWorld().equals(
                            npc.getWorld()
                    )
            ) {

                double distance =
                        npc.getLocation()
                                .distanceSquared(
                                        owner.getLocation()
                                );

                if (distance > 9) {

                    Location target =
                            owner.getLocation()
                                    .clone();

                    target.setYaw(
                            npc.getLocation().getYaw()
                    );

                    target.setPitch(0);

                    npc.teleport(target);
                }
            }

            // Keep the NPC skin synchronized
            // with its owner.
            try {

                npc.setProfile(
                        ResolvableProfile
                                .resolvableProfile(
                                        owner.getPlayerProfile()
                                )
                );

            } catch (Exception ignored) {
            }
        }
    }

    public NamespacedKey idKey() {
        return idKey;
    }
}
