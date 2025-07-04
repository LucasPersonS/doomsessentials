package org.lupz.doomsdayessentials.combat.event;

import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
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
import org.lupz.doomsdayessentials.combat.AreaManager;
import org.lupz.doomsdayessentials.combat.AreaType;
import org.lupz.doomsdayessentials.combat.ManagedArea;
import org.lupz.doomsdayessentials.combat.CombatManager;
import net.minecraftforge.event.entity.EntityMobGriefingEvent;
import net.minecraft.server.level.ServerLevel;
import org.lupz.doomsdayessentials.effect.ModEffects;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.core.BlockPos;

import java.util.Map;
import java.util.UUID;
import java.util.HashMap;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE)
public class AreaEvents {

    private static final Map<UUID, ManagedArea> lastArea = new HashMap<>();
    private static final Map<UUID, Boolean> flightGrantedByMod = new HashMap<>();

    /** Count of consecutive blocked attack messages per player to avoid chat spam. */
    private static final java.util.Map<java.util.UUID, Integer> blockedMessageCounter = new java.util.HashMap<>();

    /**
     * Sends a system message to a player, but only every 5 blocked attempts to reduce spam.
     */
    private static void sendThrottled(net.minecraft.world.entity.player.Player target, Component text) {
        UUID id = target.getUUID();
        int c = blockedMessageCounter.getOrDefault(id, 0) + 1;
        blockedMessageCounter.put(id, c);
        if (c % 5 == 1) { // 1st, 6th, 11th ...
            target.sendSystemMessage(text);
        }
        // Simple reset to keep map small
        if (c > 100) blockedMessageCounter.put(id, 1);
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent e) {
        if (!(e.player instanceof ServerPlayer player)) return;
        if (e.phase != TickEvent.Phase.END) return;

        // First: if zone closed, eject immediately for snappy response
        ManagedArea rawArea = AreaManager.get().getAreaAtIncludingClosed(player.serverLevel(), player.blockPosition());
        if (rawArea != null && !rawArea.isCurrentlyOpen()) {
            ejectFromClosedArea(player, rawArea);
            return; // skip rest
        }

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
        // Put player in combat if in a DANGER zone
        if (area.getType() == AreaType.DANGER) {
            CombatManager.get().tagPlayer(player);
        }

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

        // Guild alliance / war PvP rules ----------------------------------
        if (e.getSource().getEntity() instanceof net.minecraft.world.entity.player.Player attacker && e.getEntity() instanceof net.minecraft.world.entity.player.Player victim) {
            org.lupz.doomsdayessentials.guild.GuildsManager gman = org.lupz.doomsdayessentials.guild.GuildsManager.get(level);
            org.lupz.doomsdayessentials.guild.Guild gAtt = gman.getGuildByMember(attacker.getUUID());
            org.lupz.doomsdayessentials.guild.Guild gVic = gman.getGuildByMember(victim.getUUID());

            if (gAtt != null && gVic != null) {
                // Friendly fire block when allied (or same guild) and not at war
                boolean allied = gman.areAllied(gAtt.getName(), gVic.getName());
                boolean atWar = gman.getWar(gAtt.getName(), gVic.getName()) != null;
                if (allied && !atWar) {
                    e.setCanceled(true);
                    sendThrottled(attacker, Component.literal("§eVocê não pode atacar aliados."));
                    return;
                }
                // If at war, allow regardless of neutral zone restrictions later
                if (atWar) {
                    // skip further neutral-zone-only checks by not returning here (just allow down the method but flag)
                }
            }
        }

        ManagedArea area = AreaManager.get().getAreaAt(level, e.getEntity().blockPosition());
        if (area == null) return;

        // SAFE zones: completely block damage.
        if (area.getType() == AreaType.SAFE) {
            e.setCanceled(true);
            return;
        }

        // Custom NEUTRAL zone logic – PvP only allowed if at least one player is in combat.
        if (area.getType() == AreaType.NEUTRAL) {
            boolean neutralWarOverride = false;
            if (e.getSource().getEntity() instanceof net.minecraft.world.entity.player.Player attackerP && e.getEntity() instanceof net.minecraft.world.entity.player.Player victimP) {
                var gm = org.lupz.doomsdayessentials.guild.GuildsManager.get(level);
                var gA = gm.getGuildByMember(attackerP.getUUID());
                var gV = gm.getGuildByMember(victimP.getUUID());

                // Check if the two guilds are at war with each other
                if (gA != null && gV != null && gm.getWar(gA.getName(), gV.getName()) != null) {
                    neutralWarOverride = true; // They may fight freely
                } else {
                    // If either player belongs to a guild that is currently at war (with someone else), block combat
                    boolean attackerInWar = gA != null && gm.getActiveWarForGuild(gA.getName()) != null;
                    boolean victimInWar   = gV != null && gm.getActiveWarForGuild(gV.getName()) != null;

                    if (attackerInWar || victimInWar) {
                        e.setCanceled(true);
                        sendThrottled(attackerP, Component.literal("§eVocê não pode atacar jogadores fora da guerra na zona amarela."));
                        return;
                    }
                }
            }

            if (!neutralWarOverride) {
                if (e.getSource().getEntity() instanceof Player attacker && e.getEntity() instanceof Player victim) {
                    var cm = org.lupz.doomsdayessentials.combat.CombatManager.get();
                    boolean attackerCombat = cm.isInCombat(attacker.getUUID());
                    boolean victimCombat = cm.isInCombat(victim.getUUID());

                    // Cancel if NEITHER is in combat (temporary tag or permanent activate)
                    if (!attackerCombat && !victimCombat) {
                        e.setCanceled(true);
                        sendThrottled(attacker, Component.literal("§eVocê não está em combate nesta zona. Entre em uma zona de perigo ou use /combat activate."));
                        // Victim gets a hint only if not in combat
                        if (!victimCombat) {
                            sendThrottled(victim, Component.literal("§eVocê não está em combate nesta zona. PvP bloqueado."));
                        }
                        return;
                    }
                }
            }
        }

        // PvP Flag: specific override within an area
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
    public static void onExplosion(ExplosionEvent.Detonate event) {
        if (event.getLevel() instanceof ServerLevel level) {
            // Remove any blocks that are inside an area with prevent_explosions enabled
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

    /**
     * Teleports the player just outside the bounds of a closed area. Chooses the
     * nearest edge along X and Z axes and keeps Y unchanged.
     */
    private static void ejectFromClosedArea(ServerPlayer player, ManagedArea area) {
        BlockPos p = player.blockPosition();
        int newX = p.getX();
        int newZ = p.getZ();

        boolean insideX = p.getX() >= area.getPos1().getX() && p.getX() <= area.getPos2().getX();
        boolean insideZ = p.getZ() >= area.getPos1().getZ() && p.getZ() <= area.getPos2().getZ();

        // Decide the minimal shift needed (only one axis)
        if (insideX && insideZ) {
            int distLeft  = p.getX() - area.getPos1().getX();
            int distRight = area.getPos2().getX() - p.getX();
            int distNorth = p.getZ() - area.getPos1().getZ();
            int distSouth = area.getPos2().getZ() - p.getZ();

            int minDist = Math.min(Math.min(distLeft, distRight), Math.min(distNorth, distSouth));

            if (minDist == distLeft) newX = area.getPos1().getX() - 1;
            else if (minDist == distRight) newX = area.getPos2().getX() + 1;
            else if (minDist == distNorth) newZ = area.getPos1().getZ() - 1;
            else newZ = area.getPos2().getZ() + 1;
        } else if (insideX) {
            int distLeft  = p.getX() - area.getPos1().getX();
            int distRight = area.getPos2().getX() - p.getX();
            newX = (distLeft < distRight) ? area.getPos1().getX() - 1 : area.getPos2().getX() + 1;
        } else if (insideZ) {
            int distNorth = p.getZ() - area.getPos1().getZ();
            int distSouth = area.getPos2().getZ() - p.getZ();
            newZ = (distNorth < distSouth) ? area.getPos1().getZ() - 1 : area.getPos2().getZ() + 1;
        }

        double y = player.getY();
        player.teleportTo(newX + 0.5, y, newZ + 0.5);

        // Determine next opening time for message
        java.time.LocalTime now = java.time.LocalTime.now(java.time.ZoneId.of("America/Sao_Paulo"));
        java.time.LocalTime next = null;
        for (ManagedArea.TimeWindow tw : area.getOpenWindows()) {
            java.time.LocalTime start = tw.start();
            if (start.isAfter(now)) {
                if (next == null || start.isBefore(next)) next = start;
            }
        }
        // If none left today, pick earliest tomorrow
        if (next == null && !area.getOpenWindows().isEmpty()) {
            next = area.getOpenWindows().stream()
                    .map(ManagedArea.TimeWindow::start)
                    .min(java.time.LocalTime::compareTo).orElse(null);
        }

        String timeStr = next != null ? String.format("%02d:%02d", next.getHour(), next.getMinute()) : "--:--";
        Component chatMsg = Component.literal("§c§l[ZONA FECHADA] §eEsta área abrirá às §a" + timeStr + "§e.");
        player.sendSystemMessage(chatMsg);

        Component title = Component.literal("§4§lZONA FECHADA");
        Component subtitle = Component.literal("§eAbrirá às §a" + timeStr);
        try {
            net.minecraft.server.network.ServerGamePacketListenerImpl conn = player.connection;
            conn.send(new net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket(title));
            conn.send(new net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket(subtitle));
        } catch (Exception ignored) {}
    }
} 