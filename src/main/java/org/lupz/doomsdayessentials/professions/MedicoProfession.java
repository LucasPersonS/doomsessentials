package org.lupz.doomsdayessentials.professions;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.item.ItemStack;
import org.lupz.doomsdayessentials.config.ProfessionConfig;
import org.lupz.doomsdayessentials.injury.InjuryHelper;
import org.lupz.doomsdayessentials.injury.InjuryItems;

/**
 * Behaviour executed when a player becomes / leaves the Medico profession and for its active abilities.
 */
public final class MedicoProfession {

    private static final String TAG_IS_MEDICO = "isMedico";
    private static final String TAG_HEAL_COOLDOWN = "medicoHealCooldown";
    private static final String TAG_GLOBAL_COOLDOWN = "medicoGlobalCooldown";

    private MedicoProfession() {}

    /* -------------------------------------------------------------------------
     * Profession join / leave
     * ---------------------------------------------------------------------- */
    public static void onBecomeMedico(Player player) {
        if (player.level().isClientSide) return;
        if (!ProfessionConfig.MEDICO_ENABLED.get()) {
            player.sendSystemMessage(Component.translatable("profession.medico.disabled"));
            return;
        }

        player.getPersistentData().putBoolean(TAG_IS_MEDICO, true);
        player.sendSystemMessage(Component.translatable("profession.medico.become"));

        if (ProfessionConfig.MEDICO_ANNOUNCE_JOIN.get()) {
            for (Player other : player.level().players()) {
                if (other != player) {
                    other.sendSystemMessage(Component.translatable("profession.medico.announce", player.getDisplayName()));
                }
            }
        }

        if (ProfessionConfig.MEDICO_AUTO_RECEIVE_KIT.get()) {
            player.getInventory().add(new ItemStack(InjuryItems.MEDIC_KIT.get()));
        }
    }

    public static void onLeaveMedico(Player player) {
        if (player.level().isClientSide) return;
        player.getPersistentData().putBoolean(TAG_IS_MEDICO, false);
        player.sendSystemMessage(Component.translatable("profession.medico.leave"));

        // Remove any medic kits from inventory
        player.getInventory().items.removeIf(stack -> stack.getItem() == InjuryItems.MEDIC_KIT.get());
    }

    /* -------------------------------------------------------------------------
     * Active ability – /medico heal
     * ---------------------------------------------------------------------- */
    public static boolean useHealingAbility(ServerPlayer player) {
        if (!ProfessionConfig.MEDICO_ENABLED.get()) {
            player.sendSystemMessage(Component.translatable("profession.medico.disabled"));
            return false;
        }

        if (!player.getPersistentData().getBoolean(TAG_IS_MEDICO)) {
            player.sendSystemMessage(Component.translatable("profession.medico.not_medico"));
            return false;
        }

        int personalCd = player.getPersistentData().getInt(TAG_HEAL_COOLDOWN);
        if (personalCd > 0) {
            int seconds = personalCd / 20;
            player.sendSystemMessage(Component.translatable("profession.medico.cooldown", seconds));
            return false;
        }

        int globalCd = player.getPersistentData().getInt(TAG_GLOBAL_COOLDOWN);
        if (globalCd > 0) {
            int minutes = globalCd / 1200; // 20 ticks * 60 = 1200 per minute
            int seconds = (globalCd / 20) % 60;
            player.sendSystemMessage(Component.translatable("profession.medico.global_cooldown", minutes, seconds));
            return false;
        }

        double range = ProfessionConfig.MEDICO_HEAL_RANGE.get();
        Player target = findNearestInjuredPlayer(player, range);
        if (target == null) {
            player.sendSystemMessage(Component.translatable("profession.medico.no_target"));
            return false;
        }

        InjuryHelper.medicoHeal(player, target);

        Level lvl = player.level();
        if (lvl instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.HEART, target.getX(), target.getY() + 1.0, target.getZ(), 15, 0.5, 0.5, 0.5, 0.02);
        }

        // Apply cooldowns
        int personalTicks = ProfessionConfig.MEDICO_HEAL_COOLDOWN_SECONDS.get() * 20;
        player.getPersistentData().putInt(TAG_HEAL_COOLDOWN, personalTicks);
        int globalTicks = ProfessionConfig.MEDICO_GLOBAL_COOLDOWN_MINUTES.get() * 1200; // minutes to ticks
        player.getPersistentData().putInt(TAG_GLOBAL_COOLDOWN, globalTicks);
        return true;
    }

    /* -------------------------------------------------------------------------
     * Tick processing (called from PlayerTickEvent)
     * ---------------------------------------------------------------------- */
    public static void tickMedico(Player player) {
        if (player.level().isClientSide) return;
        if (!player.getPersistentData().getBoolean(TAG_IS_MEDICO)) return;

        var tag = player.getPersistentData();
        // Personal cooldown
        if (tag.contains(TAG_HEAL_COOLDOWN)) {
            int cd = tag.getInt(TAG_HEAL_COOLDOWN);
            if (cd > 0) {
                cd--;
                tag.putInt(TAG_HEAL_COOLDOWN, cd);
                if (cd == 0) {
                    player.sendSystemMessage(Component.translatable("profession.medico.cooldown_ready"));
                }
            }
        }
        // Global cooldown
        if (tag.contains(TAG_GLOBAL_COOLDOWN)) {
            int cd = tag.getInt(TAG_GLOBAL_COOLDOWN);
            if (cd > 0) {
                cd--;
                tag.putInt(TAG_GLOBAL_COOLDOWN, cd);
                if (cd == 0) {
                    player.sendSystemMessage(Component.translatable("profession.medico.global_cooldown_ready"));
                } else if (cd % 6000 == 0) { // every 5 minutes
                    int minutesRemaining = cd / 1200;
                    player.sendSystemMessage(Component.translatable("profession.medico.global_cooldown_update", minutesRemaining));
                }
            }
        }
    }

    /* -------------------------------------------------------------------------
     * Helpers
     * ---------------------------------------------------------------------- */
    private static Player findNearestInjuredPlayer(Player source, double range) {
        Player nearest = null;
        double nearestDistSq = Double.MAX_VALUE;
        for (Player candidate : source.level().players()) {
            if (candidate == source) continue;
            if (candidate.distanceTo(source) > range) continue;
            boolean injured = InjuryHelper.getCapability(candidate)
                    .map(cap -> cap.getInjuryLevel() > 0)
                    .orElse(false);
            if (!injured) continue;
            double distSq = candidate.distanceToSqr(source);
            if (distSq < nearestDistSq) {
                nearestDistSq = distSq;
                nearest = candidate;
            }
        }
        return nearest;
    }
} 