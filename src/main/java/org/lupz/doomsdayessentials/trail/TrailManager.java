package org.lupz.doomsdayessentials.trail;

import net.minecraft.core.particles.ParticleOptions;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class TrailManager {
    private static final TrailManager INSTANCE = new TrailManager();
    private final Map<UUID, TrailData> activeTrails = new ConcurrentHashMap<>();

    private TrailManager() {}

    public static TrailManager getInstance() {
        return INSTANCE;
    }

    public void setTrail(UUID playerUuid, TrailData trailData) {
        activeTrails.put(playerUuid, trailData);
    }

    public void removeTrail(UUID playerUuid) {
        activeTrails.remove(playerUuid);
    }

    public Map<UUID, TrailData> getActiveTrails() {
        return activeTrails;
    }
} 