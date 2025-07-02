package org.lupz.doomsdayessentials.combat.event;

import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.level.ExplosionEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lupz.doomsdayessentials.EssentialsMod;
import org.lupz.doomsdayessentials.combat.AreaManager;
import org.lupz.doomsdayessentials.combat.AreaType;
import org.lupz.doomsdayessentials.combat.ManagedArea;
import org.lupz.doomsdayessentials.combat.CombatManager;
import org.lupz.doomsdayessentials.network.PacketHandler;
import net.minecraftforge.event.entity.EntityMobGriefingEvent;
import org.lupz.doomsdayessentials.sound.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import org.lupz.doomsdayessentials.effect.ModEffects;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.effect.MobEffectInstance;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.HashMap;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE)
public class AreaEvents {

    private static final Map<UUID, ManagedArea> lastArea = new HashMap<>();
    private static final Map<UUID, Boolean> flightGrantedByMod = new HashMap<>();

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent e) {
        if (!(e.player instanceof ServerPlayer player)) return;
        if (e.phase != TickEvent.Phase.END) return;

        ManagedArea currentArea = AreaManager.get().getAreaAt(player.serverLevel(), player.blockPosition());
        ManagedArea previousArea = lastArea.get(player.getUUID());

        if (currentArea != previousArea) {
            handleAreaChange(player, previousArea, currentArea);
            if (currentArea == null) {
                lastArea.remove(player.getUUID());
            } else {
                lastArea.put(player.getUUID(), currentArea);
            }
        }
        
        if (currentArea != null) {
            applyAreaEffects(player, currentArea);
        }
    }
    
    private static void handleAreaChange(ServerPlayer player, ManagedArea from, ManagedArea to) {
        // Leaving an area
        if (from != null) {
            // Send exit message
            if (from.getExitMessage() != null && !from.getExitMessage().isEmpty()) {
                player.connection.send(new ClientboundSetTitleTextPacket(Component.literal(from.getExitMessage())));
            }
            // Revoke flight if it was granted by the mod
            if (from.isAllowFlight() && flightGrantedByMod.getOrDefault(player.getUUID(), false)) {
                if (!player.isCreative() && !player.isSpectator()) {
                    player.getAbilities().mayfly = false;
                    player.onUpdateAbilities();
                }
                flightGrantedByMod.remove(player.getUUID());
            }
            // Trigger combat countdown if leaving a hazard zone (danger / frequency)
            boolean fromHazard = from.getType() == AreaType.DANGER;
            boolean toHazard = to != null && to.getType() == AreaType.DANGER;
            if (fromHazard && !toHazard) {
                CombatManager.get().tagPlayer(player);
            }
        }
        
        // Entering an area
        if (to != null) {
            // Send entry message
            if (to.getEntryMessage() != null && !to.getEntryMessage().isEmpty()) {
                player.connection.send(new ClientboundSetTitleTextPacket(Component.literal(to.getEntryMessage())));
            }
            // Grant flight if applicable
            if (to.isAllowFlight() && !player.getAbilities().mayfly) {
                 if (!player.isCreative() && !player.isSpectator()) {
                    player.getAbilities().mayfly = true;
                    player.onUpdateAbilities();
                    flightGrantedByMod.put(player.getUUID(), true);
                }
            }
        }
    }

    private static void applyAreaEffects(ServerPlayer player, ManagedArea area) {
        // Heal players
        if (area.isHealPlayers()) {
            if (player.getHealth() < player.getMaxHealth() && player.tickCount % 20 == 0) { // every second
                player.heal(1.0f);
            }
        }
        
        // Radiation damage (once per second) unless player has the Frequency effect
        if (area.getRadiationDamage() > 0) {
            if (!player.hasEffect(ModEffects.FREQUENCY.get()) && player.tickCount % 20 == 0) {
                player.hurt(player.damageSources().wither(), area.getRadiationDamage());
            }
        }
        
        // Prevent hunger loss
        if (area.isPreventHungerLoss()) {
            if (player.getFoodData().getFoodLevel() < 20) {
                player.getFoodData().setFoodLevel(20);
            }
            if (player.getFoodData().getSaturationLevel() < 5.0f) {
                player.getFoodData().setSaturation(5.0f);
            }
        }

        // Darkness effect inside Frequency zones (unless immune)
        if (area.getType() == AreaType.FREQUENCY) {
            if (!player.hasEffect(ModEffects.FREQUENCY.get())) {
                // Apply Darkness for 2 seconds (will be refreshed each tick)
                player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 40, 0, false, false));
            }
        }
    }

    @SubscribeEvent
    public static void onLivingAttack(LivingAttackEvent e) {
        if (!(e.getEntity().level() instanceof net.minecraft.server.level.ServerLevel level)) return;

        ManagedArea area = AreaManager.get().getAreaAt(level, e.getEntity().blockPosition());
        if (area == null) return;

        // Generic SAFE zone protection
        if (area.getType() == AreaType.SAFE) {
            e.setCanceled(true);
            return;
        }

        // PvP Flag
        if (area.isPreventPvp() && e.getSource().getEntity() instanceof Player && e.getEntity() instanceof Player) {
            e.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer player)) return;

        ManagedArea area = AreaManager.get().getAreaAt(player.serverLevel(), player.blockPosition());
        if (area == null) return;
        
        // Generic SAFE zone protection
        if (area.getType() == AreaType.SAFE) {
            e.setCanceled(true);
            e.setAmount(0);
            return;
        }

        // Disable Fall Damage Flag
        if (area.isDisableFallDamage() && e.getSource().is(DamageTypes.FALL)) {
            e.setCanceled(true);
        }

        // Prevent Hunger Damage
        if (area.isPreventHungerLoss() && e.getSource().is(DamageTypes.STARVE)) {
            e.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onExplosion(ExplosionEvent.Start event) {
        if (event.getLevel() instanceof ServerLevel level) {
            // We can't just check the explosion's origin. We need to check every block.
            event.getExplosion().getToBlow().removeIf(blockPos -> {
                ManagedArea area = AreaManager.get().getAreaAt(level, blockPos);
                return area != null && area.isPreventExplosions();
            });
        }
    }

    @SubscribeEvent
    public static void onMobGrief(EntityMobGriefingEvent e) {
        if (!(e.getEntity().level() instanceof net.minecraft.server.level.ServerLevel level)) return;
        if (!(e.getEntity() instanceof Mob)) return; // Only prevent grief from mobs

        ManagedArea area = AreaManager.get().getAreaAt(level, e.getEntity().blockPosition());
        if (area != null && area.isPreventMobGriefing()) {
            e.setResult(Event.Result.DENY);
        }
    }

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (event.getPlayer() instanceof ServerPlayer player) {
            if (player.isCreative() || player.isSpectator()) {
                return;
            }

            ManagedArea area = AreaManager.get().getAreaAt((ServerLevel) event.getLevel(), event.getPos());
            if (area != null && area.isPreventBlockModification()) {
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent
    public static void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            if (player.isCreative() || player.isSpectator()) {
                return;
            }

            ManagedArea area = AreaManager.get().getAreaAt((ServerLevel) event.getLevel(), event.getPos());
            if (area != null && area.isPreventBlockModification()) {
                event.setCanceled(true);
            }
        }
    }
} 