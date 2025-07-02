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

    public static void setManagedAreas(Collection<ManagedArea> areas) {
        managedAreasByDimension.clear();
        for (ManagedArea area : areas) {
            managedAreasByDimension.computeIfAbsent(area.getDimension().location(), k -> new ArrayList<>()).add(area);
        }
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
        long msRemaining = (long) (ticksRemaining / 20.0 * 1000);
        return lastCombatSyncTime + msRemaining;
    }

    public static ManagedArea getPlayerArea(Player player) {
        if (player == null) return null;
        List<ManagedArea> areasInDim = managedAreasByDimension.get(player.level().dimension().location());
        if (areasInDim == null) {
            return null;
        }
        return areasInDim.stream()
                .filter(area -> area.contains(player.blockPosition()))
                .findFirst()
                .orElse(null);
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
} 