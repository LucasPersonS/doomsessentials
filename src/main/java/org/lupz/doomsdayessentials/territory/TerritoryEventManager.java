package org.lupz.doomsdayessentials.territory;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.lupz.doomsdayessentials.combat.AreaManager;
import org.lupz.doomsdayessentials.combat.ManagedArea;
import org.lupz.doomsdayessentials.guild.Guild;
import org.lupz.doomsdayessentials.guild.GuildsManager;
import org.lupz.doomsdayessentials.network.PacketHandler;
import org.lupz.doomsdayessentials.network.packet.s2c.TerritoryProgressPacket;
import net.minecraftforge.network.PacketDistributor;
import net.minecraft.ChatFormatting;

import java.util.*;

/**
 * Handles a single territory capture event at a time. During an active event, players must
 * stand inside the configured area for a set duration while meeting the minimum player
 * requirement. If they leave/die or another guild contests, progress resets.
 */
public class TerritoryEventManager {

    private static final TerritoryEventManager INSTANCE = new TerritoryEventManager();

    public static TerritoryEventManager get() { return INSTANCE; }

    // ---------------------------------------------------------------------
    // Inner data class representing a running capture event
    // ---------------------------------------------------------------------
    private static class CaptureEvent {
        ManagedArea area;
        int requiredPlayers;
        int durationSeconds;
        int progressSeconds = 0;
        String currentGuild = null; // guild currently accumulating progress
        byte lastStatus = 0; // 0 none,1 contested,2 capturing
    }

    private final Map<String, CaptureEvent> activeEvents = new HashMap<>();

    private TerritoryEventManager() {
        MinecraftForge.EVENT_BUS.register(this);
    }

    /**
     * Starts a capture event for the given area. Only one event can run at a time.
     * @param area The ManagedArea being contested (should be DANGER or RESOURCE type).
     * @param durationSeconds How long the controlling guild must hold the point.
     * @param requiredPlayers Minimum simultaneous players of the controlling guild.
     * @return true if successfully started, false if another event is already running.
     */
    public boolean startEvent(ManagedArea area, int durationSeconds, int requiredPlayers) {
        if (activeEvents.containsKey(area.getName())) return false; // already running for this area
        if (activeEvents.size() >= 2) return false; // limit two concurrent events
        // If area is currently SAFE (previously dominated) convert back to DANGER for the event
        if (area.getType() == org.lupz.doomsdayessentials.combat.AreaType.SAFE) {
            org.lupz.doomsdayessentials.combat.AreaManager am = org.lupz.doomsdayessentials.combat.AreaManager.get();
            am.deleteArea(area.getName());
            area = new org.lupz.doomsdayessentials.combat.ManagedArea(area.getName(), org.lupz.doomsdayessentials.combat.AreaType.DANGER,
                    area.getDimension(), area.getPos1(), area.getPos2());
            am.addArea(area);
        }

        CaptureEvent ev = new CaptureEvent();
        ev.area = area;
        ev.durationSeconds = durationSeconds;
        ev.requiredPlayers = requiredPlayers;
        activeEvents.put(area.getName(), ev);

        // Reset any previous ownership so the winner will claim it anew
        org.lupz.doomsdayessentials.territory.ResourceGeneratorManager.get().unclaimArea(area.getName());

        broadcast(Component.literal("§eEvento iniciado em §f" + area.getName() + "§e! Capture mantendo §f" + requiredPlayers + "§e jogadores por §f" + (durationSeconds/60) + "§em."));

        // send initial contested marker
        BlockPos c = new BlockPos((area.getPos1().getX()+area.getPos2().getX())/2, area.getPos1().getY()+4, (area.getPos1().getZ()+area.getPos2().getZ())/2);
        PacketHandler.CHANNEL.send(PacketDistributor.ALL.noArg(),
                new org.lupz.doomsdayessentials.network.packet.s2c.TerritoryMarkerPacket(area.getName(), c.getX()+0.5, c.getY(), c.getZ()+0.5, (byte)1));
        ev.lastStatus = 1;
        return true;
    }

    public void stopEvent(String areaName) {
        CaptureEvent ev = activeEvents.remove(areaName);
        if (ev == null) return;
        PacketHandler.CHANNEL.send(PacketDistributor.ALL.noArg(),
                new TerritoryProgressPacket(ev.area.getName(), ev.currentGuild, ev.progressSeconds, ev.durationSeconds, false));
        broadcast(Component.literal("§cEvento em " + areaName + " finalizado."));
    }

    public void stopAll() {
        new java.util.ArrayList<>(activeEvents.keySet()).forEach(this::stopEvent);
    }

    public boolean isEventRunning() { return !activeEvents.isEmpty(); }
    public boolean isEventRunning(String areaName) { return activeEvents.containsKey(areaName); }

    public CaptureEvent getEvent(String areaName) { return activeEvents.get(areaName); }

    public java.util.Set<String> getActiveAreas() { return java.util.Collections.unmodifiableSet(activeEvents.keySet()); }

    public int getEventProgress(String areaName) { return activeEvents.containsKey(areaName) ? activeEvents.get(areaName).progressSeconds : 0; }
    public int getEventDuration(String areaName) { return activeEvents.containsKey(areaName) ? activeEvents.get(areaName).durationSeconds : 0; }
    public int getEventRequiredPlayers(String areaName) { return activeEvents.containsKey(areaName) ? activeEvents.get(areaName).requiredPlayers : 0; }

    // ---------------------------------------------------------------------
    // Runtime adjustments
    // ---------------------------------------------------------------------
    /**
     * Updates the minimum amount of simultaneous players required to capture the area while an
     * event is already running.
     * @param newRequired The new player count (must be >= 1)
     * @return true if the event is running and the value was updated, false otherwise.
     */
    public boolean updateRequiredPlayers(String areaName, int newRequired) {
        if (newRequired < 1) return false;
        if (!activeEvents.containsKey(areaName)) return false;
        activeEvents.get(areaName).requiredPlayers = newRequired;
        broadcast(Component.literal("§eRequisito de jogadores em " + areaName + " ajustado para §f" + newRequired + "§e."));
        return true;
    }

    // ---------------------------------------------------------------------
    // Server tick processing – run each second to update capture progress
    // ---------------------------------------------------------------------
    private int tickCounter = 0;

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        if (activeEvents.isEmpty()) return;

        tickCounter++;
        if (tickCounter % 20 != 0) return; // once per second

        ServerLevel level = ServerLifecycleHooks.getCurrentServer().overworld();
        if (level == null) return;

        // Iterate over a copy to avoid concurrent modification when finishing events
        for (CaptureEvent ev : new java.util.ArrayList<>(activeEvents.values())) {
            processEventTick(level, ev);
        }
    }

    private void processEventTick(ServerLevel level, CaptureEvent ev) {
        ManagedArea area = ev.area;

        // No visualization here – moved to after state evaluation

        // Build map guildName -> player count currently inside area
        Map<String, Integer> counts = new HashMap<>();
        List<ServerPlayer> playersHere = new ArrayList<>();
        for (ServerPlayer p : level.getPlayers(p -> true)) {
            if (area.contains(p.blockPosition())) {
                Guild g = GuildsManager.get(level).getGuildByMember(p.getUUID());
                if (g == null) continue; // Only guild members count
                String gname = g.getName();
                counts.put(gname, counts.getOrDefault(gname, 0) + 1);
                playersHere.add(p);
            }
        }

        // Determine controlling guild this tick
        String controllingGuild = null;
        for (var entry : counts.entrySet()) {
            if (entry.getValue() >= ev.requiredPlayers) {
                if (controllingGuild == null) {
                    controllingGuild = entry.getKey();
                } else {
                    // More than one guild meets the requirement – contest, reset
                    controllingGuild = null;
                    break;
                }
            }
        }

        // Determine status byte
        byte status = (controllingGuild == null) ? (byte)1 : (byte)2;
        if (status != ev.lastStatus) {
            BlockPos c = new BlockPos((area.getPos1().getX()+area.getPos2().getX())/2, area.getPos1().getY()+4, (area.getPos1().getZ()+area.getPos2().getZ())/2);
            PacketHandler.CHANNEL.send(PacketDistributor.ALL.noArg(),
                    new org.lupz.doomsdayessentials.network.packet.s2c.TerritoryMarkerPacket(area.getName(), c.getX()+0.5, c.getY(), c.getZ()+0.5, status));
            ev.lastStatus = status;
        }

        // Draw border: red if idle/contested, green if capturing
        visualizeAreaBorders(level, area, controllingGuild != null);

        if (controllingGuild == null) {
            // contested or nobody meets requirement -> reset progress
            if (ev.progressSeconds != 0) {
                broadcast(Component.literal("§6Captura em §f" + area.getName() + " §6reiniciada §r– §eárea contestada."));
            }
            ev.progressSeconds = 0;
            ev.currentGuild = null;
            return;
        }

        // If guild changed, reset progress
        if (!Objects.equals(ev.currentGuild, controllingGuild)) {
            ev.currentGuild = controllingGuild;
            ev.progressSeconds = 0;
            broadcast(Component.literal("§e" + controllingGuild + " §aagora está capturando §f" + area.getName() + " §7(" + ev.progressSeconds + "/" + ev.durationSeconds + "s)"));
        }

        ev.progressSeconds++;
        if (ev.progressSeconds % 30 == 0 || ev.progressSeconds == ev.durationSeconds) {
            broadcast(Component.literal("§6Capturando §f" + area.getName() + " §7… §e" + controllingGuild + " §7(" + ev.progressSeconds + "/" + ev.durationSeconds + "s)"));
        }

        // Send progress packet to all players for HUD
        PacketHandler.CHANNEL.send(PacketDistributor.ALL.noArg(), new TerritoryProgressPacket(area.getName(), ev.currentGuild, ev.progressSeconds, ev.durationSeconds, true));

        // border already drawn above

        if (ev.progressSeconds >= ev.durationSeconds) {
            finishCapture(ev, level, controllingGuild);
            PacketHandler.CHANNEL.send(PacketDistributor.ALL.noArg(), new TerritoryProgressPacket(area.getName(), controllingGuild, ev.durationSeconds, ev.durationSeconds, false));
            activeEvents.remove(area.getName());
        }
    }

    private void finishCapture(CaptureEvent ev, ServerLevel level, String guildName) {
        broadcast(Component.literal("§aA guilda §2" + guildName + " §acapturou §f" + ev.area.getName() + "§a!"));
        // Title announcement
        Component title = Component.literal("§a§lTerritório capturado");
        Component subtitle = Component.literal("§e" + guildName + " agora controla " + ev.area.getName());
        for (ServerPlayer p : level.getServer().getPlayerList().getPlayers()) {
            p.connection.send(new net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket(title));
            p.connection.send(new net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket(subtitle));
        }
        // Transform the area into SAFE zone and re-register
        AreaManager am = AreaManager.get();
        am.deleteArea(ev.area.getName());
        ManagedArea safeArea = new ManagedArea(ev.area.getName(), org.lupz.doomsdayessentials.combat.AreaType.SAFE,
                ev.area.getDimension(), ev.area.getPos1(), ev.area.getPos2());
        am.addArea(safeArea);
        // Claim the generator for the guild (if it exists)
        org.lupz.doomsdayessentials.territory.ResourceGeneratorManager.get().claimArea(ev.area.getName(), guildName);
        BlockPos c2 = new BlockPos((ev.area.getPos1().getX()+ev.area.getPos2().getX())/2, ev.area.getPos1().getY()+4, (ev.area.getPos2().getZ()+ev.area.getPos1().getZ())/2);
        PacketHandler.CHANNEL.send(PacketDistributor.ALL.noArg(),
                new org.lupz.doomsdayessentials.network.packet.s2c.TerritoryMarkerPacket(ev.area.getName(), c2.getX()+0.5, c2.getY(), c2.getZ()+0.5, (byte)3));
    }

    private void broadcast(Component msg) {
        var server = ServerLifecycleHooks.getCurrentServer();
        if (server != null) {
            Component prefix = Component.literal("⟦").withStyle(ChatFormatting.DARK_AQUA)
                    .append(Component.literal("TERRITÓRIO").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD))
                    .append(Component.literal("⟧ ").withStyle(ChatFormatting.DARK_AQUA));
            server.getPlayerList().broadcastSystemMessage(prefix.copy().append(msg), false);
        }
    }

    // ---------------------------------------------------------------
    // Particle helper
    // ---------------------------------------------------------------
    private void visualizeAreaBorders(ServerLevel level, ManagedArea area, boolean capturing) {
        // Determine bounds
        int minX = area.getPos1().getX();
        int minZ = area.getPos1().getZ();
        int maxX = area.getPos2().getX();
        int maxZ = area.getPos2().getZ();

        // Add small offset to spawn slightly inside block borders
        double offset = 0.1;

        int y = area.getPos1().getY() + 1; // one block above area floor

        int step = 4;

        net.minecraft.core.particles.DustParticleOptions particle = capturing
                ? new net.minecraft.core.particles.DustParticleOptions(new org.joml.Vector3f(0f, 1f, 0f), 1.0f) // green
                : new net.minecraft.core.particles.DustParticleOptions(new org.joml.Vector3f(1f, 0f, 0f), 1.0f); // red

        // West and East walls
        for (int z = minZ; z <= maxZ; z += step) {
            level.sendParticles(particle, minX + offset, y, z + offset, 1, 0, 0, 0, 0);
            level.sendParticles(particle, maxX + 1 - offset, y, z + offset, 1, 0, 0, 0, 0);
        }
        // North and South walls
        for (int x = minX; x <= maxX; x += step) {
            level.sendParticles(particle, x + offset, y, minZ + offset, 1, 0, 0, 0, 0);
            level.sendParticles(particle, x + offset, y, maxZ + 1 - offset, 1, 0, 0, 0, 0);
        }
    }
} 