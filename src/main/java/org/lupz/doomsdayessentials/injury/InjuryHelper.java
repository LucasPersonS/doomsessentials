package org.lupz.doomsdayessentials.injury;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.util.LazyOptional;
import org.lupz.doomsdayessentials.config.EssentialsConfig;
import org.lupz.doomsdayessentials.injury.capability.InjuryCapability;
import org.lupz.doomsdayessentials.injury.capability.InjuryCapabilityProvider;
import org.lupz.doomsdayessentials.injury.network.InjuryNetwork;
import org.lupz.doomsdayessentials.injury.network.UpdateDownedStatePacket;
import org.lupz.doomsdayessentials.injury.network.UpdateHealingProgressPacket;
import org.lupz.doomsdayessentials.injury.network.UpdateInjuryLevelPacket;
import org.lupz.doomsdayessentials.professions.MedicalHelpManager;
import org.lupz.doomsdayessentials.block.ModBlocks;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.lupz.doomsdayessentials.block.MedicalBedBlock;

import javax.annotation.Nullable;

public class InjuryHelper {

    public static LazyOptional<InjuryCapability> getCapability(Player player) {
        return player.getCapability(InjuryCapabilityProvider.INJURY_CAPABILITY);
    }

    public static InjuryCapability getCapabilityUnwrap(Player player) {
        return player.getCapability(InjuryCapabilityProvider.INJURY_CAPABILITY).orElseThrow(() -> new IllegalStateException("Injury capability not present"));
    }

    public static void setInjuryLevel(Player player, int level) {
        getCapability(player).ifPresent(cap -> {
            int maxLevel = EssentialsConfig.MAX_INJURY_LEVEL.get();
            int newLevel = Math.max(0, Math.min(level, maxLevel));
            cap.setInjuryLevel(newLevel);
            if (player instanceof ServerPlayer sp) {
                InjuryNetwork.sendToPlayer(new UpdateInjuryLevelPacket(newLevel), sp);
            }
            if (newLevel > 0) {
                applyEffects(player);
            } else {
                revivePlayer(player);
            }
        });
    }

    public static void downPlayer(Player player, @Nullable Player attacker) {
        getCapability(player).ifPresent(cap -> {
            if (cap.isDowned()) return;
            long downedUntil = System.currentTimeMillis() + (long)EssentialsConfig.DOWNED_BLEED_OUT_SECONDS.get() * 1000;
            cap.setDowned(true, attacker != null ? attacker.getUUID() : null);
            // Initialize the remaining health a downed player can take before being finished off
            cap.setDownedHealth((float) EssentialsConfig.DOWNED_HEALTH_POOL.get().doubleValue());
            cap.setDownedUntil(downedUntil);
            // Cancel the automatic vanilla death statistic that was already awarded
            if (player instanceof net.minecraft.server.level.ServerPlayer sp) {
                sp.awardStat(net.minecraft.stats.Stats.CUSTOM.get(net.minecraft.stats.Stats.DEATHS), -1);
            }
            if (player instanceof ServerPlayer sp) {
                InjuryNetwork.sendToPlayer(new UpdateDownedStatePacket(true, downedUntil), sp);
            }
        });
    }

    public static void revivePlayer(Player player) {
        getCapability(player).ifPresent(cap -> {
            if (!cap.isDowned()) return;
            cap.setDowned(false, null);
            player.clearFire();
            player.removeAllEffects();
            if (player instanceof ServerPlayer sp) {
                InjuryNetwork.sendToPlayer(new UpdateDownedStatePacket(false, 0), sp);
                InjuryNetwork.sendToPlayer(new UpdateInjuryLevelPacket(cap.getInjuryLevel()), sp);
            }
        });
    }

    public static void applyEffects(Player player) {
        if (!EssentialsConfig.INJURY_SYSTEM_ENABLED.get()) return;
        getCapability(player).ifPresent(cap -> {
            int level = cap.getInjuryLevel();
            if (level <= 0) return;

            if (level >= 1 && EssentialsConfig.ENABLE_SLOWNESS_EFFECT.get()) {
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 0, false, false, true));
            }
            if (level >= 2 && EssentialsConfig.ENABLE_MINING_FATIGUE_EFFECT.get()) {
                player.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 60, 0, false, false, true));
            }
            if (level >= 3) {
                player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 80, 0, false, false, true));
            }
            if (level >= 4 && EssentialsConfig.ENABLE_BLINDNESS_EFFECT.get()) {
                player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 40, 0, false, false, true));
            }
        });
    }

    public static void onHealingBed(Player player) {
        if (!EssentialsConfig.INJURY_SYSTEM_ENABLED.get() || !(player instanceof ServerPlayer serverPlayer)) {
            return;
        }

        getCapability(player).ifPresent(cap -> {
            if (cap.getInjuryLevel() <= 0) {
                // Player is not injured, release them
                releaseFromHealingBed(player);
                return;
            }

            long startTime = player.getPersistentData().getLong("healingBedStartTime");
            if (startTime == 0) {
                // If start time is not set, set it now.
                startTime = player.level().getGameTime();
                player.getPersistentData().putLong("healingBedStartTime", startTime);
            }

            long healingTimeMinutes = EssentialsConfig.HEALING_BED_TIME_MINUTES.get();
            long healingTimeTicks = healingTimeMinutes * 60 * 20;
            long elapsedTimeTicks = player.level().getGameTime() - startTime;

            // Send progress updates and particles roughly every second
            if (player.tickCount % 20 == 0) {
                if (player.level() instanceof ServerLevel serverLevel) {
                    serverLevel.sendParticles(ParticleTypes.HEART, player.getX(), player.getY() + 1.0, player.getZ(), 2, 0.5, 0.5, 0.5, 0.0);
                }

                float progress = (float) elapsedTimeTicks / (float) healingTimeTicks;
                InjuryNetwork.sendToPlayer(new UpdateHealingProgressPacket(progress), serverPlayer);

                long ticksRemaining = healingTimeTicks - elapsedTimeTicks;
                int secondsRemaining = (int) (ticksRemaining / 20);
                int minutes = Math.max(0, secondsRemaining / 60);
                int secs = Math.max(0, secondsRemaining % 60);
                Component bar = Component.literal(String.format("§eRecuperação: %02d:%02d", minutes, secs));
                serverPlayer.displayClientMessage(bar, true);
            }

            if (elapsedTimeTicks >= healingTimeTicks) {
                healPlayer(player, 1);
                player.getPersistentData().putLong("healingBedStartTime", player.level().getGameTime()); // Reset timer for next level

                if (cap.getInjuryLevel() <= 0) {
                    releaseFromHealingBed(player);
                }
            }
        });
    }

    public static void releaseFromHealingBed(Player player) {
        BlockPos bedPos = new BlockPos(
                player.getPersistentData().getInt("healingBedX"),
                player.getPersistentData().getInt("healingBedY"),
                player.getPersistentData().getInt("healingBedZ")
        );

        player.getPersistentData().remove("healingBedLock");
        player.getPersistentData().remove("healingBedStartTime");
        player.getPersistentData().remove("healingBedX");
        player.getPersistentData().remove("healingBedY");
        player.getPersistentData().remove("healingBedZ");

        if (player instanceof ServerPlayer sp) {
            InjuryNetwork.sendToPlayer(new UpdateHealingProgressPacket(-1f), sp); // Hide progress bar

            // Set bed to not occupied
            Level level = player.level();
            if(level.getBlockState(bedPos).is(ModBlocks.MEDICAL_BED.get())) {
                BlockState bedState = level.getBlockState(bedPos);
                level.setBlock(bedPos, bedState.setValue(MedicalBedBlock.OCCUPIED, false), 3);

                Direction bedDirection = bedState.getValue(MedicalBedBlock.FACING);
                BlockPos otherPartPos = bedPos.relative(bedDirection.getOpposite());
                BlockState otherPartState = level.getBlockState(otherPartPos);
                if(otherPartState.is(ModBlocks.MEDICAL_BED.get())) {
                    level.setBlock(otherPartPos, otherPartState.setValue(MedicalBedBlock.OCCUPIED, false), 3);
                }
            }
        }
        player.sendSystemMessage(Component.translatable("injury.healed.full"));
    }

    public static void healPlayer(Player player, int amount) {
        if (EssentialsConfig.INJURY_SYSTEM_ENABLED.get()) {
            getCapability(player).ifPresent(cap -> {
                int oldLevel = cap.getInjuryLevel();
                if (oldLevel > 0) {
                    int newLevel = cap.decrementInjuryLevel(amount);
                    if (player instanceof ServerPlayer sp)
                        InjuryNetwork.sendToPlayer(new org.lupz.doomsdayessentials.injury.network.UpdateInjuryLevelPacket(newLevel), sp);

                    if (newLevel <= 0) {
                        player.sendSystemMessage(Component.translatable("injury.healed.full"));
                    } else {
                        player.sendSystemMessage(Component.translatable("injury.healed.partial", newLevel));
                    }
                    player.playSound(SoundEvents.PLAYER_LEVELUP, 1.0F, 1.5F);
                }
            });
        }
    }

    public static void medicoHeal(Player healer, Player target) {
        if (EssentialsConfig.INJURY_SYSTEM_ENABLED.get()) {
            getCapability(target).ifPresent(cap -> {
                int oldLevel = cap.getInjuryLevel();
                if (oldLevel <= 0) {
                    healer.sendSystemMessage(Component.translatable("medico.heal.not_injured"));
                } else {
                    int healAmount = EssentialsConfig.MEDICO_HEAL_AMOUNT.get();
                    int newLevel = cap.decrementInjuryLevel(healAmount);
                    if (newLevel <= 0) {
                        target.sendSystemMessage(Component.translatable("medico.heal.target.full"));
                        healer.sendSystemMessage(Component.translatable("medico.heal.healer.full"));
                    } else {
                        target.sendSystemMessage(Component.translatable("medico.heal.target.partial", newLevel));
                        healer.sendSystemMessage(Component.translatable("medico.heal.healer.partial", newLevel));
                    }
                    target.playSound(SoundEvents.PLAYER_LEVELUP, 1.0F, 1.5F);

                    java.util.UUID medicoUuid = MedicalHelpManager.completeRequest(target.getUUID());
                    if (medicoUuid != null && healer.getUUID().equals(medicoUuid)) {
                        healer.getPersistentData().remove("medicoHelpTarget");
                        healer.sendSystemMessage(Component.literal("§aChamado concluído!"));
                        target.sendSystemMessage(Component.literal("§aVocê foi atendido pelo médico."));
                    }
                }
            });
        }
    }

    public static void incrementInjuryLevel(Player player) {
        if (EssentialsConfig.INJURY_SYSTEM_ENABLED.get()) {
            getCapability(player).ifPresent(cap -> {
                int oldLevel = cap.getInjuryLevel();
                int maxLevel = EssentialsConfig.MAX_INJURY_LEVEL.get();
                if (oldLevel < maxLevel) {
                    int newLevel = cap.incrementInjuryLevel();
                    player.sendSystemMessage(Component.translatable("injury.increased", newLevel));
                }
            });
        }
    }
} 