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
    private static final java.util.Map<java.util.UUID, Long> lastClosedEjectMs = new java.util.HashMap<>();
    private static final java.util.Map<java.util.UUID, String> lastClosedZoneName = new java.util.HashMap<>();
    private static final java.util.Map<java.util.UUID, Long> lastCloseMsgMs = new java.util.HashMap<>();
    private static final java.util.Map<java.util.UUID, Integer> lastCloseMsgMinute = new java.util.HashMap<>();
    private static final java.util.Map<java.util.UUID, Long> lastClosedHintMs = new java.util.HashMap<>();

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

        ManagedArea rawArea = AreaManager.get().getAreaAtIncludingClosed(player.serverLevel(), player.blockPosition());
        if (rawArea != null && !rawArea.isCurrentlyOpen()) {
            long nowMs = System.currentTimeMillis();
            String currName = rawArea.getName();
            String lastName = lastClosedZoneName.get(player.getUUID());
            Long lastMs = lastClosedEjectMs.get(player.getUUID());
            boolean shouldEject = lastMs == null || nowMs - lastMs > 5000 || lastName == null || !lastName.equals(currName);
            lastClosedZoneName.put(player.getUUID(), currName);
            if (shouldEject) {
                lastClosedEjectMs.put(player.getUUID(), nowMs);
                ejectFromClosedArea(player, rawArea);
            }
            Long lastHint = lastClosedHintMs.get(player.getUUID());
            if (lastHint == null || nowMs - lastHint >= 300000L) {
                lastClosedHintMs.put(player.getUUID(), nowMs);
                org.lupz.doomsdayessentials.network.PacketHandler.CHANNEL.send(
                        net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> player),
                        new org.lupz.doomsdayessentials.airdrop.network.AirdropNoticePacket(
                                "hud.doomsdayessentials.zone_closed",
                                org.lupz.doomsdayessentials.airdrop.network.AirdropNoticePacket.STATE_DESPAWNED
                        )
                );
            }
            return;
        } else {
            lastClosedZoneName.remove(player.getUUID());
            lastClosedEjectMs.remove(player.getUUID());
            lastClosedHintMs.remove(player.getUUID());
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
            if (!currentArea.getOpenWindows().isEmpty()) {
                Integer mins = minutesUntilClose(currentArea);
                if (mins != null && mins > 0) {
                    long nowMs = System.currentTimeMillis();
                    long period = mins <= 5 ? 60000L : 300000L;
                    Long lastMs = lastCloseMsgMs.get(player.getUUID());
                    Integer lastMin = lastCloseMsgMinute.get(player.getUUID());
                    boolean send = lastMs == null || nowMs - lastMs >= period || (mins <= 5 && (lastMin == null || !mins.equals(lastMin)));
                    if (send) {
                        lastCloseMsgMs.put(player.getUUID(), nowMs);
                        lastCloseMsgMinute.put(player.getUUID(), mins);
                        org.lupz.doomsdayessentials.network.PacketHandler.CHANNEL.send(
                                net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> player),
                                new org.lupz.doomsdayessentials.airdrop.network.AirdropNoticePacket(
                                        "hud.doomsdayessentials.zone_close_minutes",
                                        org.lupz.doomsdayessentials.airdrop.network.AirdropNoticePacket.STATE_OPENED,
                                        Integer.toString(mins))
                        );
                    }
                }
            }
        }

        // Always render marker for CLOSED areas if player is within 30 blocks of center
        for (ManagedArea area : org.lupz.doomsdayessentials.combat.AreaManager.get().getAreas()) {
            if (area.isCurrentlyOpen()) continue; // Only for closed areas
            BlockPos center = new BlockPos(
                (area.getPos1().getX() + area.getPos2().getX()) / 2,
                area.getPos2().getY() + 1,
                (area.getPos1().getZ() + area.getPos2().getZ()) / 2
            );
            double distSq = player.blockPosition().distSqr(center);
            if (distSq <= 30 * 30) {
                org.lupz.doomsdayessentials.network.PacketHandler.CHANNEL.send(
                    net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> player),
                    new org.lupz.doomsdayessentials.network.packet.s2c.TerritoryMarkerPacket(
                        area.getName(),
                        center.getX() + 0.5,
                        center.getY(),
                        center.getZ() + 0.5,
                        (byte)4 // status 4 for closed/always marker
                    )
                );
            }
        }
    }
    
    private static void handleAreaChange(ServerPlayer player, ManagedArea from, ManagedArea to) {
        // Leaving an area
        if (from != null) {
            if (from.getType() == AreaType.ARENA) {
                player.sendSystemMessage(Component.literal("§cVocê saiu da arena."));
            }
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
            boolean fromHazard = from.getType() == AreaType.DANGER;
            boolean toHazard = to != null && to.getType() == AreaType.DANGER;
            if (fromHazard && !toHazard) {
                boolean enteringOverlaySafe = to != null && to.getType() == AreaType.SAFE && to.isOverlayPreferred();
                if (!enteringOverlaySafe) {
                    CombatManager.get().tagPlayer(player);
                }
            }
        }
        
        // Entering an area
        if (to != null) {
            if (to.getType() == AreaType.ARENA) {
                player.sendSystemMessage(Component.literal("§aVocê entrou na arena."));
            }
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
            if (!to.getOpenWindows().isEmpty() && to.getType() == AreaType.DANGER) {
                Integer mins = minutesUntilClose(to);
                if (mins != null && mins > 0 && mins <= 5) {
                    org.lupz.doomsdayessentials.network.PacketHandler.CHANNEL.send(
                            net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> player),
                            new org.lupz.doomsdayessentials.airdrop.network.AirdropNoticePacket(
                                    "hud.doomsdayessentials.zone_close_minutes",
                                    org.lupz.doomsdayessentials.airdrop.network.AirdropNoticePacket.STATE_OPENED,
                                    Integer.toString(mins))
                    );
                }
            }
        }
    }

    private static void applyAreaEffects(ServerPlayer player, ManagedArea area) {
        // Maintain combat while inside DANGER zones
        if (area.getType() == AreaType.DANGER) {
            if (!player.isCreative() && !player.isSpectator()) {
                CombatManager.get().tagPlayer(player);
            }
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

        // SAFE zones: completely block damage.
        if (area != null && area.getType() == AreaType.SAFE) {
            e.setCanceled(true);
            return;
        }

        // Default NEUTRAL semantics: anywhere not DANGER or SAFE behaves as NEUTRAL
        boolean defaultNeutral = (area == null) || (area.getType() != AreaType.DANGER && area.getType() != AreaType.SAFE && area.getType() != AreaType.ARENA && area.getType() != AreaType.FREQUENCY && area.getType() != AreaType.RESOURCE && area.getType() != AreaType.PRISON);
        if (defaultNeutral) {
            boolean neutralWarOverride = false;
            if (e.getSource().getEntity() instanceof net.minecraft.world.entity.player.Player attackerP && e.getEntity() instanceof net.minecraft.world.entity.player.Player victimP) {
                var gm = org.lupz.doomsdayessentials.guild.GuildsManager.get(level);
                var gA = gm.getGuildByMember(attackerP.getUUID());
                var gV = gm.getGuildByMember(victimP.getUUID());

                if (gA != null && gV != null) {
                    boolean atWar = gm.getWar(gA.getName(), gV.getName()) != null;
                    boolean allied = gm.areAllied(gA.getName(), gV.getName());
                    if (allied && !atWar) {
                        e.setCanceled(true);
                        sendThrottled(attackerP, Component.literal("§eVocê não pode atacar aliados."));
                        return;
                    }
                    if (atWar) {
                        neutralWarOverride = true;
                    } else {
                        boolean attackerInWar = gm.getActiveWarForGuild(gA.getName()) != null;
                        boolean victimInWar   = gm.getActiveWarForGuild(gV.getName()) != null;
                        if (attackerInWar || victimInWar) {
                            e.setCanceled(true);
                            sendThrottled(attackerP, Component.literal("§eVocê não pode atacar jogadores fora da guerra na zona amarela."));
                            return;
                        }
                    }
                }
            }

            if (!neutralWarOverride) {
                if (e.getSource().getEntity() instanceof ServerPlayer attacker && e.getEntity() instanceof ServerPlayer victim) {
                    if (!attacker.isCreative() && !attacker.isSpectator() && !victim.isCreative() && !victim.isSpectator()) {
                        org.lupz.doomsdayessentials.combat.CombatManager.get().tagPlayer(attacker);
                        // Victim só entra em combate se revidar (tratado em CombatManager.onPlayerAttack)
                    }
                }
            }
        }

        // PvP Flag: specific override within an area
        if (area != null && area.isPreventPvp() && e.getSource().getEntity() instanceof Player && e.getEntity() instanceof Player) {
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
        final int OFFSET = 4; // blocks to push out
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

            if (minDist == distLeft) newX = area.getPos1().getX() - OFFSET;
            else if (minDist == distRight) newX = area.getPos2().getX() + OFFSET;
            else if (minDist == distNorth) newZ = area.getPos1().getZ() - OFFSET;
            else newZ = area.getPos2().getZ() + OFFSET;
        } else if (insideX) {
            int distLeft  = p.getX() - area.getPos1().getX();
            int distRight = area.getPos2().getX() - p.getX();
            newX = (distLeft < distRight) ? area.getPos1().getX() - OFFSET : area.getPos2().getX() + OFFSET;
        } else if (insideZ) {
            int distNorth = p.getZ() - area.getPos1().getZ();
            int distSouth = area.getPos2().getZ() - p.getZ();
            newZ = (distNorth < distSouth) ? area.getPos1().getZ() - OFFSET : area.getPos2().getZ() + OFFSET;
        }

        double y = player.getY();
        double ty = findSafeY(player.serverLevel(), newX, newZ, y);
        player.teleportTo(newX + 0.5, ty, newZ + 0.5);

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

        org.lupz.doomsdayessentials.network.PacketHandler.CHANNEL.send(
                net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> player),
                new org.lupz.doomsdayessentials.network.packet.s2c.ClosedZonePacket(timeStr, true));
        // Set marker Y to 1 block above the highest Y of the area
        double markerY = area.getPos2().getY() + 1;
        org.lupz.doomsdayessentials.network.PacketHandler.CHANNEL.send(
                net.minecraftforge.network.PacketDistributor.ALL.noArg(),
                new org.lupz.doomsdayessentials.network.packet.s2c.TerritoryMarkerPacket(area.getName(), p.getX()+0.5, markerY, p.getZ()+0.5, (byte)4));
    }

    private static Integer minutesUntilClose(ManagedArea area) {
        java.time.ZoneId zone = java.time.ZoneId.of("America/Sao_Paulo");
        java.time.LocalTime now = java.time.LocalTime.now(zone);
        for (ManagedArea.TimeWindow tw : area.getOpenWindows()) {
            if (tw.isActive(now)) {
                java.time.ZonedDateTime base = java.time.ZonedDateTime.now(zone);
                java.time.ZonedDateTime end = base.withHour(tw.end().getHour()).withMinute(tw.end().getMinute()).withSecond(0).withNano(0);
                if (!end.isAfter(base)) end = end.plusDays(1);
                long mins = java.time.Duration.between(base, end).toMinutes();
                return (int) mins;
            }
        }
        return null;
    }

    private static double findSafeY(ServerLevel level, int x, int z, double baseY) {
        int by = (int) Math.floor(baseY);
        for (int dy = 0; dy <= 6; dy++) {
            int yy = by + dy;
            net.minecraft.core.BlockPos b1 = new net.minecraft.core.BlockPos(x, yy, z);
            net.minecraft.core.BlockPos b2 = b1.above();
            if (level.isEmptyBlock(b1) && level.isEmptyBlock(b2)) return yy + 0.0;
        }
        int ground = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        return ground + 0.5;
    }
} 
