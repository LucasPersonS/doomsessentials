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
    }

    private CaptureEvent activeEvent;

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
        if (activeEvent != null) return false;
        CaptureEvent ev = new CaptureEvent();
        ev.area = area;
        ev.durationSeconds = durationSeconds;
        ev.requiredPlayers = requiredPlayers;
        this.activeEvent = ev;
        broadcast(Component.literal("§6[Territory] Evento iniciado em " + area.getName() + "! Capture mantendo " + requiredPlayers + " jogadores por " + (durationSeconds/60) + "m."));
        return true;
    }

    public void stopEvent() {
        this.activeEvent = null;
    }

    public boolean isEventRunning() { return activeEvent != null; }

    // ---------------------------------------------------------------------
    // Server tick processing – run each second to update capture progress
    // ---------------------------------------------------------------------
    private int tickCounter = 0;

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        if (activeEvent == null) return;

        tickCounter++;
        if (tickCounter % 20 != 0) return; // once per second

        ServerLevel level = ServerLifecycleHooks.getCurrentServer().overworld();
        if (level == null) return;

        CaptureEvent ev = activeEvent;
        ManagedArea area = ev.area;

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

        if (controllingGuild == null) {
            // contested or nobody meets requirement -> reset progress
            if (ev.progressSeconds != 0) {
                broadcast(Component.literal("§e[Territory] Captura reiniciada – área contestada."));
            }
            ev.progressSeconds = 0;
            ev.currentGuild = null;
            return;
        }

        // If guild changed, reset progress
        if (!Objects.equals(ev.currentGuild, controllingGuild)) {
            ev.currentGuild = controllingGuild;
            ev.progressSeconds = 0;
            broadcast(Component.literal("§e[Territory] " + controllingGuild + " iniciou a captura (" + ev.progressSeconds + "/" + ev.durationSeconds + "s)."));
        }

        ev.progressSeconds++;
        if (ev.progressSeconds % 30 == 0 || ev.progressSeconds == ev.durationSeconds) {
            broadcast(Component.literal("§6[Territory] " + controllingGuild + " capturando... " + ev.progressSeconds + "/" + ev.durationSeconds + "s"));
        }

        // Send progress packet to all players for HUD
        PacketHandler.CHANNEL.send(PacketDistributor.ALL.noArg(), new TerritoryProgressPacket(area.getName(), ev.currentGuild, ev.progressSeconds, ev.durationSeconds, true));

        if (ev.progressSeconds >= ev.durationSeconds) {
            finishCapture(ev, level, controllingGuild);
            PacketHandler.CHANNEL.send(PacketDistributor.ALL.noArg(), new TerritoryProgressPacket(area.getName(), controllingGuild, ev.durationSeconds, ev.durationSeconds, false));
            activeEvent = null;
        }
    }

    private void finishCapture(CaptureEvent ev, ServerLevel level, String guildName) {
        broadcast(Component.literal("§a[Territory] A guilda " + guildName + " capturou " + ev.area.getName() + "!"));
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
    }

    private void broadcast(Component msg) {
        var server = ServerLifecycleHooks.getCurrentServer();
        if (server != null) {
            server.getPlayerList().broadcastSystemMessage(msg, false);
        }
    }
} 