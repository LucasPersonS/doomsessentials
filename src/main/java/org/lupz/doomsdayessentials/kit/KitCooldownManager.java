package org.lupz.doomsdayessentials.kit;

import net.minecraft.server.level.ServerPlayer;

/**
 * Manages cooldowns for kit claims using player persistent data.
 */
public final class KitCooldownManager {

    private KitCooldownManager() {
    }

    /**
     * Gets the NBT key for a specific tier and cooldown type.
     */
    private static String getCooldownKey(String tier, Kit.CooldownType cooldownType) {
        return "kit_cooldown_" + tier.toLowerCase() + "_" + cooldownType.getId();
    }

    /**
     * Checks if a player can claim a kit (cooldown has expired).
     */
    public static boolean canClaim(ServerPlayer player, String tier, Kit.CooldownType cooldownType) {
        String key = getCooldownKey(tier, cooldownType);
        long lastClaim = player.getPersistentData().getLong(key);

        if (lastClaim == 0)
            return true; // Never claimed before

        long now = System.currentTimeMillis();
        long cooldownDuration = cooldownType.getCooldownMillis();

        return (now - lastClaim) >= cooldownDuration;
    }

    /**
     * Records that a player has claimed a kit.
     */
    public static void setClaimed(ServerPlayer player, String tier, Kit.CooldownType cooldownType) {
        String key = getCooldownKey(tier, cooldownType);
        player.getPersistentData().putLong(key, System.currentTimeMillis());
    }

    /**
     * Gets the remaining cooldown time in milliseconds.
     * Returns 0 if the cooldown has expired or never been set.
     */
    public static long getRemainingCooldown(ServerPlayer player, String tier, Kit.CooldownType cooldownType) {
        String key = getCooldownKey(tier, cooldownType);
        long lastClaim = player.getPersistentData().getLong(key);

        if (lastClaim == 0)
            return 0;

        long now = System.currentTimeMillis();
        long cooldownDuration = cooldownType.getCooldownMillis();
        long elapsed = now - lastClaim;

        if (elapsed >= cooldownDuration)
            return 0;

        return cooldownDuration - elapsed;
    }

    /**
     * Formats remaining cooldown as a human-readable string.
     */
    public static String formatCooldown(long milliseconds) {
        if (milliseconds <= 0)
            return "Disponível";

        long seconds = milliseconds / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;

        if (days > 0) {
            long remainingHours = hours % 24;
            return String.format("%dd %dh", days, remainingHours);
        } else if (hours > 0) {
            long remainingMinutes = minutes % 60;
            return String.format("%dh %dm", hours, remainingMinutes);
        } else if (minutes > 0) {
            long remainingSeconds = seconds % 60;
            return String.format("%dm %ds", minutes, remainingSeconds);
        } else {
            return String.format("%ds", seconds);
        }
    }

    /**
     * Resets cooldown for a player (admin command).
     */
    public static void resetCooldown(ServerPlayer player, String tier, Kit.CooldownType cooldownType) {
        String key = getCooldownKey(tier, cooldownType);
        player.getPersistentData().remove(key);
    }

    /**
     * Resets all kit cooldowns for a player.
     */
    public static void resetAllCooldowns(ServerPlayer player) {
        for (String tier : KitManager.TIERS) {
            for (Kit.CooldownType cooldownType : Kit.CooldownType.values()) {
                resetCooldown(player, tier, cooldownType);
            }
        }
    }
}
