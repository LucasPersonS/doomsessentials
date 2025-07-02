package org.lupz.doomsdayessentials.professions;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import org.lupz.doomsdayessentials.config.ProfessionConfig;
import org.lupz.doomsdayessentials.injury.InjuryHelper;
import org.lupz.doomsdayessentials.injury.InjuryItems;
import org.lupz.doomsdayessentials.professions.MedicalHelpManager;
import org.lupz.doomsdayessentials.block.MedicalBedBlock;
import org.lupz.doomsdayessentials.professions.ProfissaoManager;

import java.util.UUID;

/**
 * Behaviour executed when a player becomes / leaves the Medico profession and for its active abilities.
 */
public final class MedicoProfession {

    private static final String TAG_IS_MEDICO = "isMedico";
    private static final String TAG_HEAL_COOLDOWN = "medicoHealCooldown";
    private static final String TAG_GLOBAL_COOLDOWN = "medicoGlobalCooldown";
    private static final String TAG_BED_COOLDOWN = "medicoBedCooldown";

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

        // Inform about abilities
        player.sendSystemMessage(Component.translatable("profession.medico.info.bed", ProfessionConfig.MEDICO_BED_COOLDOWN_SECONDS.get()));

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

        if (!"medico".equalsIgnoreCase(ProfissaoManager.getProfession(player.getUUID()))) {
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
     * Active ability – /medico bed <player>
     * ---------------------------------------------------------------------- */
    public static boolean putPatientInBed(ServerPlayer medico, ServerPlayer target) {
        if (!ProfessionConfig.MEDICO_ENABLED.get()) {
            medico.sendSystemMessage(Component.translatable("profession.medico.disabled"));
            return false;
        }

        if (!"medico".equalsIgnoreCase(ProfissaoManager.getProfession(medico.getUUID()))) {
            medico.sendSystemMessage(Component.translatable("profession.medico.not_medico"));
            return false;
        }

        // Personal cooldown check
        int bedCd = medico.getPersistentData().getInt(TAG_BED_COOLDOWN);
        if (bedCd > 0) {
            int seconds = bedCd / 20;
            medico.sendSystemMessage(Component.translatable("profession.medico.bed_cooldown", seconds));
            return false;
        }

        // Ensure target is injured
        boolean injured = InjuryHelper.getCapability(target)
                .map(cap -> cap.getInjuryLevel() > 0)
                .orElse(false);
        if (!injured) {
            medico.sendSystemMessage(Component.translatable("profession.medico.no_target"));
            return false;
        }

        // Find nearest medical bed around the medico (radius 8)
        BlockPos bedPos = findNearestMedicalBed(medico.blockPosition(), medico.level(), 8);
        if (bedPos == null) {
            medico.sendSystemMessage(Component.translatable("profession.medico.no_bed"));
            return false;
        }

        // Teleport target onto the bed foot position
        double x = bedPos.getX() + 0.5;
        double y = bedPos.getY() + 0.5;
        double z = bedPos.getZ() + 0.5;
        target.teleportTo(x, y, z);

        // Attempt to start sleeping – ignore potential problems for now
        try {
            var res = target.startSleepInBed(bedPos);
        } catch (Exception ignored) {
        }

        // Send initial HUD progress (0%) so the client overlay shows instantly
        org.lupz.doomsdayessentials.injury.network.InjuryNetwork.sendToPlayer(
                new org.lupz.doomsdayessentials.injury.network.UpdateHealingProgressPacket(0.0F),
                medico.getServer() != null ? medico : target);

        // Mark locked to bed until healed
        target.getPersistentData().putBoolean("healingBedLock", true);
        target.getPersistentData().putInt("healingBedX", bedPos.getX());
        target.getPersistentData().putInt("healingBedY", bedPos.getY());
        target.getPersistentData().putInt("healingBedZ", bedPos.getZ());

        medico.sendSystemMessage(Component.translatable("profession.medico.bed.success", target.getDisplayName()));
        target.sendSystemMessage(Component.translatable("profession.medico.bed.target", medico.getDisplayName()));

        int bedTicks = ProfessionConfig.MEDICO_BED_COOLDOWN_SECONDS.get() * 20;
        medico.getPersistentData().putInt(TAG_BED_COOLDOWN, bedTicks);
        return true;
    }

    private static BlockPos findNearestMedicalBed(BlockPos origin, Level level, int radius) {
        BlockPos.MutableBlockPos mut = new BlockPos.MutableBlockPos();
        int yStart = origin.getY() - radius;
        int yEnd = origin.getY() + radius;
        for (int y = yStart; y <= yEnd; y++) {
            for (int x = -radius; x <= radius; x++) {
                for (int z = -radius; z <= radius; z++) {
                    mut.set(origin.getX() + x, y, origin.getZ() + z);
                    BlockState state = level.getBlockState(mut);
                    if (!(state.getBlock() instanceof MedicalBedBlock)) continue;
                    if (state.getValue(MedicalBedBlock.PART) != BedPart.FOOT) continue;
                    return mut.immutable();
                }
            }
        }
        return null;
    }

    /* -------------------------------------------------------------------------
     * Tick processing (called from PlayerTickEvent)
     * ---------------------------------------------------------------------- */
    public static void tickMedico(Player player) {
        if (player.level().isClientSide) return;
        if (!"medico".equalsIgnoreCase(ProfissaoManager.getProfession(player.getUUID()))) return;

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

        // Action bar update every second for active help target
        if (player.tickCount % 20 == 0 && player.getPersistentData().hasUUID("medicoHelpTarget")) {
            UUID targetId = player.getPersistentData().getUUID("medicoHelpTarget");
            Player target = player.level().getPlayerByUUID(targetId);
            if (target != null) {
                int dist = (int) player.distanceTo(target);
                Component bar = Component.literal("§ePaciente: §f" + (int) target.getX() + " " + (int) target.getY() + " " + (int) target.getZ() + " §7d=" + dist);
                ((ServerPlayer) player).displayClientMessage(bar, true);
            }
        }

        // Bed cooldown
        if (tag.contains(TAG_BED_COOLDOWN)) {
            int cd = tag.getInt(TAG_BED_COOLDOWN);
            if (cd > 0) {
                cd--;
                tag.putInt(TAG_BED_COOLDOWN, cd);
                if (cd == 0) {
                    player.sendSystemMessage(Component.translatable("profession.medico.bed_cooldown_ready"));
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