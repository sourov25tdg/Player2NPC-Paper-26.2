package com.dksourov.player2npc;

import org.bukkit.entity.Mannequin;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.persistence.PersistentDataType;

import java.util.UUID;

public final class CompanionListener
        implements Listener {

    private final CompanionManager manager;

    public CompanionListener(
            CompanionManager manager
    ) {
        this.manager = manager;
    }

    @EventHandler
    public void onInteract(
            PlayerInteractEntityEvent event
    ) {

        if (!(event.getRightClicked()
                instanceof Mannequin mannequin)) {

            return;
        }

        String id =
                mannequin
                        .getPersistentDataContainer()
                        .get(
                                manager.idKey(),
                                PersistentDataType.STRING
                        );

        if (id == null) {
            return;
        }

        try {

            UUID uuid =
                    UUID.fromString(id);

            Companion companion =
                    manager.all()
                            .stream()
                            .filter(c ->
                                    c.id.equals(uuid))
                            .findFirst()
                            .orElse(null);

            if (companion != null) {

                event.getPlayer().sendRichMessage(
                        "<gold>" +
                        companion.name +
                        "</gold> <gray>Task: <white>" +
                        companion.task.name() +
                        "</white>"
                );
            }

        } catch (Exception ignored) {
        }
    }
}
