package org.lupz.doomsdayessentials.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import org.lupz.doomsdayessentials.combat.ManagedArea;
import org.lupz.doomsdayessentials.combat.AreaType;
import net.minecraft.resources.ResourceLocation;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;

public class ClientCombatState {

    private static Map<ResourceLocation, List<ManagedArea>> managedAreasByDimension = new HashMap<>();
    private static Map<UUID, Integer> playersInCombat = new HashMap<>();
    private static long lastCombatSyncTime = 0;

    // Simple cache for local player's area per tick and block position
    private static long cachedTick = -1;
    private static net.minecraft.core.BlockPos cachedPos = null;
    private static ManagedArea cachedArea = null;

    public static void setManagedAreas(Collection<ManagedArea> areas) {
        managedAreasByDimension.clear();
        for (ManagedArea area : areas) {
            managedAreasByDimension.computeIfAbsent(area.getDimension().location(), k -> new ArrayList<>()).add(area);
        }
        // Invalidate cache when areas list changes
        cachedTick = -1;
        cachedPos = null;
        cachedArea = null;
    }

    public static void setPlayersInCombat(Map<UUID, Integer> combatState) {
        playersInCombat = new HashMap<>(combatState);
        lastCombatSyncTime = System.currentTimeMillis();
    }

    public static Map<UUID, Integer> getPlayersInCombat() {
        return playersInCombat;
    }

    public static boolean isPlayerInCombat(UUID uuid) {
        return playersInCombat.containsKey(uuid);
    }
    
    public static long getCombatEndTime(UUID playerUUID) {
        if (!playersInCombat.containsKey(playerUUID)) {
            return 0;
        }
        int ticksRemaining = playersInCombat.get(playerUUID);
        if (ticksRemaining < 0) {
            return 0; // permanent combat, treat as no countdown
        }
        long msRemaining = (long) (ticksRemaining / 20.0 * 1000);
        return lastCombatSyncTime + msRemaining;
    }

    public static boolean isPlayerAlwaysActive(UUID uuid) {
        Integer v = playersInCombat.get(uuid);
        return v != null && v < 0;
    }

    public static ManagedArea getPlayerArea(Player player) {
        if (player == null) return null;
        // Fast path: for local player, cache by tick and block pos
        Minecraft mc = Minecraft.getInstance();
        if (player == mc.player && mc.level != null) {
            long gameTime = mc.level.getGameTime();
            net.minecraft.core.BlockPos pos = player.blockPosition();
            if (cachedTick == gameTime && cachedPos != null && cachedPos.equals(pos)) {
                return cachedArea;
            }
            ManagedArea resolved = resolvePlayerArea(player);
            cachedTick = gameTime;
            cachedPos = pos;
            cachedArea = resolved;
            return resolved;
        }
        // For other players, resolve directly
        return resolvePlayerArea(player);
    }

    private static ManagedArea resolvePlayerArea(Player player) {
        List<ManagedArea> areasInDim = managedAreasByDimension.get(player.level().dimension().location());
        if (areasInDim == null) {
            return null;
        }
        ManagedArea best = null;
        int bestPriority = Integer.MAX_VALUE;
        for (ManagedArea area : areasInDim) {
            if (!area.contains(player.blockPosition())) continue;
            if (!area.isCurrentlyOpen()) continue;
            int prio;
            switch (area.getType()) {
                case DANGER -> prio = 0;
                case SAFE -> prio = 1;
                case FREQUENCY -> prio = 2;
                case NEUTRAL -> prio = 3;
                default -> prio = 4;
            }
            if (prio < bestPriority) {
                bestPriority = prio;
                best = area;
            }
        }
        return best;
    }

    public static boolean isInDangerArea() {
        Player player = Minecraft.getInstance().player;
        if (player == null) return false;
        ManagedArea area = getPlayerArea(player);
        return area != null && area.getType() == AreaType.DANGER;
    }

    public static boolean isInSafeArea() {
        Player player = Minecraft.getInstance().player;
        if (player == null) return false;
        ManagedArea area = getPlayerArea(player);
        return area != null && area.getType() == AreaType.SAFE;
    }

    public static ManagedArea getAreaByName(String name) {
        if (name == null) return null;
        for (List<ManagedArea> list : managedAreasByDimension.values()) {
            for (ManagedArea a : list) {
                if (a.getName().equalsIgnoreCase(name)) {
                    return a;
                }
            }
        }
        return null;
    }
} 