package org.lupz.doomsdayessentials.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import org.lupz.doomsdayessentials.combat.ManagedArea;
import org.lupz.doomsdayessentials.combat.AreaType;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class ClientCombatState {

    private static Map<String, ManagedArea> managedAreas = new ConcurrentHashMap<>();
    private static Map<UUID, Integer> playersInCombat = new ConcurrentHashMap<>();
    private static long lastCombatSyncTime = 0;

    public static void setManagedAreas(Collection<ManagedArea> areas) {
        managedAreas = areas.stream().collect(Collectors.toMap(ManagedArea::getName, area -> area));
    }

    public static void setPlayersInCombat(Map<UUID, Integer> combatState) {
        playersInCombat = new ConcurrentHashMap<>(combatState);
        lastCombatSyncTime = System.currentTimeMillis();
    }

    public static Map<String, ManagedArea> getManagedAreas() {
        return managedAreas;
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
        return managedAreas.values().stream()
                .filter(area -> area.getDimension().location().equals(player.level().dimension().location()))
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