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
            cap.setDownedUntil(downedUntil);
            if (player instanceof ServerPlayer sp) {
                InjuryNetwork.sendToPlayer(new UpdateDownedStatePacket(true, downedUntil), sp);
            }
        });
    }

    public static void revivePlayer(Player player) {
        getCapability(player).ifPresent(cap -> {
            if (!cap.isDowned()) return;
            cap.setDowned(false, null);
            cap.setInjuryLevel(0);
            player.clearFire();
            player.removeAllEffects();
            if (player instanceof ServerPlayer sp) {
                InjuryNetwork.sendToPlayer(new UpdateDownedStatePacket(false, 0), sp);
                InjuryNetwork.sendToPlayer(new UpdateInjuryLevelPacket(0), sp);
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
        if (EssentialsConfig.INJURY_SYSTEM_ENABLED.get()) {
            getCapability(player).ifPresent(cap -> {
                if (cap.getInjuryLevel() > 0) {
                    cap.setHealCooldown(cap.getHealCooldown() + 1);
                    int cooldown = cap.getHealCooldown();
                    int healingTimeMinutes = EssentialsConfig.HEALING_BED_TIME_MINUTES.get();
                    int healingTimeTicks = healingTimeMinutes * 20 * 60;

                    if (player.level() instanceof ServerLevel serverLevel) {
                        if (cooldown % 20 == 0) {
                            serverLevel.sendParticles(ParticleTypes.HEART, player.getX(), player.getY() + 1.0, player.getZ(), 2, 0.5, 0.5, 0.5, 0.0);
                        }
                    }

                    if (player instanceof ServerPlayer serverPlayer) {
                        if (cooldown % 20 == 0) {
                            float progress = (float) cooldown / (float) healingTimeTicks;
                            InjuryNetwork.sendToPlayer(new UpdateHealingProgressPacket(progress), serverPlayer);

                            // Send action bar with remaining time
                            int secondsRemaining = (healingTimeTicks - cooldown) / 20;
                            int minutes = secondsRemaining / 60;
                            int secs = secondsRemaining % 60;
                            Component bar = Component.literal(String.format("§eRecuperação: %02d:%02d", minutes, secs));
                            serverPlayer.displayClientMessage(bar, true);
                        }
                    }

                    if (cooldown + 1 >= healingTimeTicks) {
                        healPlayer(player, 1);
                        cap.setHealCooldown(0);

                        // If player fully healed, release from bed
                        if (cap.getInjuryLevel() <= 0) {
                            if (player.isSleeping()) {
                                player.stopSleepInBed(true, true);
                            }
                            player.getPersistentData().remove("healingBedLock");
                            player.getPersistentData().remove("healingBedX");
                            player.getPersistentData().remove("healingBedY");
                            player.getPersistentData().remove("healingBedZ");

                            if (player instanceof ServerPlayer sp2) {
                                InjuryNetwork.sendToPlayer(new UpdateHealingProgressPacket(-1f), sp2);
                            }
                        }

                        if (player instanceof ServerPlayer sp) {
                            InjuryNetwork.sendToPlayer(new org.lupz.doomsdayessentials.injury.network.UpdateInjuryLevelPacket(cap.getInjuryLevel()), sp);
                        }
                    }
                }
            });
        }
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

    // This was empty in the decompiled code.
    public static void tryInflictInjury(Player player, float damage) {
    }
} 