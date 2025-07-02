package org.lupz.doomsdayessentials.professions;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import org.lupz.doomsdayessentials.config.ProfessionConfig;
import net.minecraft.server.level.ServerPlayer;

public final class RastreadorProfession {
    private static final String TAG_IS_TRACKER = "isRastreador";

    private RastreadorProfession() {}

    public static void onBecome(Player player) {
        if (player.level().isClientSide) return;
        if (!ProfessionConfig.RASTREADOR_ENABLED.get()) {
            player.sendSystemMessage(Component.translatable("profession.rastreador.disabled"));
            return;
        }
        player.getPersistentData().putBoolean(TAG_IS_TRACKER, true);
        player.sendSystemMessage(Component.translatable("profession.rastreador.become"));

        // Apply passive bonuses (night vision)
        applyBonuses(player);
    }

    public static void onLeave(Player player) {
        if (player.level().isClientSide) return;
        player.getPersistentData().putBoolean(TAG_IS_TRACKER, false);
        player.sendSystemMessage(Component.translatable("profession.rastreador.leave"));

        // Remove night vision when leaving
        player.removeEffect(MobEffects.NIGHT_VISION);
    }

    public static boolean isTracker(Player player) {
        return player.getPersistentData().getBoolean(TAG_IS_TRACKER);
    }

    /**
     * Apply passive bonuses (called on become and on login/respawn).
     */
    public static void applyBonuses(Player player) {
        // Permanent Night Vision I
        if (!player.hasEffect(MobEffects.NIGHT_VISION)) {
            player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, Integer.MAX_VALUE, 0, true, false, false));
        }
    }

    /* -------------------------------------------------------------------------
     * Active ability – /rastreador glow
     * ---------------------------------------------------------------------- */
    private static final String TAG_GLOW_COOLDOWN = "rastreadorGlowCooldown";

    public static boolean useGlowAbility(ServerPlayer player) {
        if (!ProfessionConfig.RASTREADOR_ENABLED.get()) {
            player.sendSystemMessage(Component.translatable("profession.rastreador.disabled"));
            return false;
        }

        if (!isTracker(player)) {
            player.sendSystemMessage(Component.translatable("profession.rastreador.not_tracker"));
            return false;
        }

        int cd = player.getPersistentData().getInt(TAG_GLOW_COOLDOWN);
        if (cd > 0) {
            int minutes = cd / 1200;
            int seconds = (cd / 20) % 60;
            player.sendSystemMessage(Component.translatable("profession.rastreador.cooldown", minutes, seconds));
            return false;
        }

        // Find players in 30 block radius and apply glowing
        double radius = 30.0;
        var level = player.level();
        for (Player other : level.players()) {
            if (other == player) continue;
            if (other.distanceTo(player) > radius) continue;
            other.addEffect(new MobEffectInstance(MobEffects.GLOWING, 200, 0, false, false));
        }

        player.sendSystemMessage(Component.translatable("profession.rastreador.glow_used"));

        // Set cooldown (1 hour = 72000 ticks)
        player.getPersistentData().putInt(TAG_GLOW_COOLDOWN, 72000);
        return true;
    }

    public static void tickTracker(Player player) {
        if (player.level().isClientSide) return;
        if (!isTracker(player)) return;

        var tag = player.getPersistentData();
        if (tag.contains(TAG_GLOW_COOLDOWN)) {
            int cd = tag.getInt(TAG_GLOW_COOLDOWN);
            if (cd > 0) {
                cd--;
                tag.putInt(TAG_GLOW_COOLDOWN, cd);
                if (cd == 0) {
                    player.sendSystemMessage(Component.translatable("profession.rastreador.cooldown_ready"));
                }
            }
        }
    }
} 