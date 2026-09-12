package com.dksourov.player2npc;

import org.bukkit.Location;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class Companion {

    public enum Task {
        IDLE,
        FOLLOW,
        COMBAT,
        MINING,
        FARMING,
        CRAFTING,
        SMELTING
    }

    public final UUID id;
    public UUID owner;
    public String name;
    public String skin;
    public Task task = Task.IDLE;
    public Location location;

    public final List<String> memory = new ArrayList<>();
    public final Map<String, Location> waypoints = new LinkedHashMap<>();
    public final List<String> inventory = new ArrayList<>();

    public Companion(UUID id, UUID owner, String name, Location location) {
        this.id = id;
        this.owner = owner;
        this.name = name;
        this.location = location;
    }
}
