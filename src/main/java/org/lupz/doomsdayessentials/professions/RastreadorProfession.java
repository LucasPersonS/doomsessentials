package org.lupz.doomsdayessentials.professions;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import org.lupz.doomsdayessentials.config.EssentialsConfig;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import org.lupz.doomsdayessentials.professions.items.ProfessionItems;
import java.util.List;
import net.minecraft.ChatFormatting;
import org.lupz.doomsdayessentials.professions.capability.TrackerCapabilityProvider;

public final class RastreadorProfession {
    private static final String TAG_IS_TRACKER = "isRastreador";

    private RastreadorProfession() {}

    public static void onBecome(Player player) {
        if (player.level().isClientSide) return;
        if (!EssentialsConfig.RASTREADOR_ENABLED.get()) {
            player.sendSystemMessage(Component.translatable("profession.rastreador.disabled"));
            return;
        }
        player.getPersistentData().putBoolean(TAG_IS_TRACKER, true);
        player.sendSystemMessage(Component.translatable("profession.rastreador.become"));

        // Give tracker compass
        player.getInventory().add(new ItemStack(ProfessionItems.TRACKING_COMPASS.get()));

        // Apply passive bonuses (night vision)
        applyBonuses(player);
    }

    public static void onLeave(Player player) {
        if (player.level().isClientSide) return;
        player.getPersistentData().putBoolean(TAG_IS_TRACKER, false);
        player.sendSystemMessage(Component.translatable("profession.rastreador.leave"));

        // Remove tracker compass from inventory
        player.getInventory().items.removeIf(item -> item.getItem() == ProfessionItems.TRACKING_COMPASS.get());

        // Remove passive effects
        MobEffectInstance effect = player.getEffect(MobEffects.NIGHT_VISION);
        if (effect != null) player.removeEffect(MobEffects.NIGHT_VISION);
    }

    public static boolean isTracker(Player player) {
        return player.getPersistentData().getBoolean(TAG_IS_TRACKER);
    }

    /**
     * Apply passive bonuses (called on become and on login/respawn).
     */
    public static void applyBonuses(Player player) {
        if (!player.hasEffect(MobEffects.NIGHT_VISION)) {
            player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, Integer.MAX_VALUE, 0, true, false, false));
        }
    }

    /* -------------------------------------------------------------------------
     * Active ability – /rastreador glow
     * ---------------------------------------------------------------------- */
    private static final String TAG_GLOW_COOLDOWN = "rastreadorGlowCooldown";

    public static boolean useGlowAbility(ServerPlayer player) {
        if (!EssentialsConfig.RASTREADOR_ENABLED.get()) {
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

        // Find players in radius and apply glowing if not on whitelist
        double radius = EssentialsConfig.RASTREADOR_SCAN_RADIUS.get();
        int duration = EssentialsConfig.RASTREADOR_SCAN_DURATION_SECONDS.get() * 20;

        final boolean[] found = {false};
        player.getCapability(TrackerCapabilityProvider.TRACKER_CAPABILITY).ifPresent(cap -> {
            java.util.List<ServerPlayer> nearbyPlayers = player.level().getEntitiesOfClass(ServerPlayer.class, player.getBoundingBox().inflate(radius),
                    p -> p != player && !cap.isWhitelisted(p.getUUID()));

            if (nearbyPlayers.isEmpty()) {
                player.sendSystemMessage(Component.literal("Nenhum jogador encontrado por perto.").withStyle(ChatFormatting.YELLOW));
            } else {
                for (ServerPlayer target : nearbyPlayers) {
                    target.addEffect(new MobEffectInstance(MobEffects.GLOWING, duration, 0));
                }
                player.sendSystemMessage(Component.literal("Revelados " + nearbyPlayers.size() + " jogadores próximos!").withStyle(ChatFormatting.GREEN));
                found[0] = true;
            }
        });

        if (found[0]) {
            // Set cooldown from config only when something was found
            player.getPersistentData().putInt(TAG_GLOW_COOLDOWN, EssentialsConfig.RASTREADOR_SCAN_COOLDOWN_MINUTES.get() * 1200);
        }

        return found[0];
    }

    public static void tickTracker(Player player) {
        if (player.level().isClientSide) return;

        if (isTracker(player)) {
            // Permanent Night Vision I
            if (!player.hasEffect(MobEffects.NIGHT_VISION)) {
                player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, Integer.MAX_VALUE, 0, true, false, false));
            }
        } else {
            // Remove effect if not tracker anymore
            MobEffectInstance effect = player.getEffect(MobEffects.NIGHT_VISION);
            if (effect != null) {
                player.removeEffect(MobEffects.NIGHT_VISION);
            }
        }

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