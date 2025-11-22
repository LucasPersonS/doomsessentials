package org.lupz.doomsdayessentials.kofh;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.Nullable;
import org.lupz.doomsdayessentials.EssentialsMod;
import org.lupz.doomsdayessentials.combat.AreaManager;
import org.lupz.doomsdayessentials.combat.ManagedArea;
import org.lupz.doomsdayessentials.guild.GuildResourceBank;
import org.lupz.doomsdayessentials.guild.GuildsManager;
import org.lupz.doomsdayessentials.network.PacketHandler;
import org.lupz.doomsdayessentials.network.packet.s2c.TerritoryMarkerPacket;
import org.lupz.doomsdayessentials.network.packet.s2c.KofhScorePacket;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * King of the Hill event manager – round-based scoreboard system that uses ManagedArea
 * for the "hill" objective, awards points per second to the controlling guild, pauses
 * on contest, and grants rewards to the winner at round end.
 */
@Mod.EventBusSubscriber(modid = EssentialsMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class KofhEventManager {
    private static final KofhEventManager INSTANCE = new KofhEventManager();
    public static KofhEventManager get() { return INSTANCE; }

    private boolean running = false;
    private @Nullable ManagedArea currentArea = null;
    private int remainingSeconds = 0;

    private final Map<String, Double> points = new ConcurrentHashMap<>(); // guildName -> points (double for fractional accumulation)
    private String controllingGuild = null;
    private boolean contested = false;
    private double multiplier = 1.0;
    private int holdStreakSeconds = 0;
    private int noControlStreakSeconds = 0;

    // Internal timers
    private int tickCounter = 0;

    private KofhEventManager() {}

    public boolean isRunning() { return running; }
    public @Nullable ManagedArea getCurrentArea() { return currentArea; }
    public int getRemainingSeconds() { return remainingSeconds; }

    // ---------------------------------------------------------------------
    // Lifecycle
    // ---------------------------------------------------------------------
    public boolean start(String areaName, int durationSeconds) {
        ManagedArea area = AreaManager.get().getArea(areaName);
        if (area == null) return false;
        if (running) stop(null);

        this.currentArea = area;
        this.remainingSeconds = Math.max(1, durationSeconds);
        this.points.clear();
        this.controllingGuild = null;
        this.contested = false;
        this.multiplier = 1.0;
        this.holdStreakSeconds = 0;
        this.tickCounter = 0;
        this.running = true;

        EssentialsMod.LOGGER.info("[KOFH] Started at area '{}' for {}s", areaName, durationSeconds);
        return true;
    }

    /**
     * Convenience overload to start and immediately broadcast the world marker using the provided server.
     */
    public boolean start(MinecraftServer server, String areaName, int durationSeconds) {
        boolean ok = start(areaName, durationSeconds);
        if (ok) {
            broadcastMarker(server);
            // Send initial HUD update so clients see timer from second 0
            sendHudUpdate(server);
        }
        return ok;
    }

    public void stop(@Nullable MinecraftServer server) {
        if (!running) return;
        running = false;
        String winner = getWinnerGuild();
        if (server != null && winner != null) {
            // Reward distribution
            for (ServerLevel level : server.getAllLevels()) {
                GuildResourceBank bank = GuildResourceBank.get(level);
                bank.add(winner, KofhConfig.REWARD_RESOURCE_ID.get(), KofhConfig.REWARD_AMOUNT_WINNER.get());
            }
            EssentialsMod.LOGGER.info("[KOFH] Round ended. Winner: {} (+{} {})", winner, KofhConfig.REWARD_AMOUNT_WINNER.get(), KofhConfig.REWARD_RESOURCE_ID.get());
            // Notify players online with leaderboard
            List<Map.Entry<String, Double>> ranking = points.entrySet().stream()
                    .sorted((a,b) -> Double.compare(b.getValue(), a.getValue()))
                    .toList();
            for (ServerPlayer sp : server.getPlayerList().getPlayers()) {
                sp.displayClientMessage(Component.literal("KOFH finalizado! Vencedor: " + winner + "."), false);
                int pos = 1;
                for (var en : ranking) {
                    String line = pos + ". " + en.getKey() + " - " + (int)Math.round(en.getValue()) + " pts";
                    sp.displayClientMessage(Component.literal(line), false);
                    if (++pos > 5) break;
                }
            }
        }
        if (currentArea != null && server != null) {
            AreaManager am = AreaManager.get();
            am.deleteArea(currentArea.getName());
            ManagedArea safeArea = new ManagedArea(currentArea.getName(), org.lupz.doomsdayessentials.combat.AreaType.SAFE,
                    currentArea.getDimension(), currentArea.getPos1(), currentArea.getPos2());
            am.addArea(safeArea);
            if (winner != null) org.lupz.doomsdayessentials.territory.ResourceGeneratorManager.get().claimArea(currentArea.getName(), winner);
            sendMarkerToAll(server, (byte)3);
        }

        currentArea = null;
        controllingGuild = null;
        contested = false;
        points.clear();
        remainingSeconds = 0;
        multiplier = 1.0;
        holdStreakSeconds = 0;
        noControlStreakSeconds = 0;
    }

    private String getWinnerGuild() {
        if (points.isEmpty()) return null;
        return points.entrySet().stream()
                .max(Comparator.comparingDouble(Map.Entry::getValue))
                .map(Map.Entry::getKey).orElse(null);
    }

    // ---------------------------------------------------------------------
    // Ticking
    // ---------------------------------------------------------------------
    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        KofhEventManager.get().tick(e);
    }

    private void tick(TickEvent.ServerTickEvent e) {
        if (!running || currentArea == null) return;
        MinecraftServer server = e.getServer();
        if (server == null) return;

        // Determine guild presence inside the area
        Map<String, Integer> counts = new HashMap<>();
        for (ServerPlayer sp : server.getPlayerList().getPlayers()) {
            if (sp.level().dimension() != currentArea.getDimension()) continue;
            BlockPos p = sp.blockPosition();
            if (!currentArea.contains(p)) continue;
            String gname = Optional.ofNullable(GuildsManager.get(sp.serverLevel()).getGuildByMember(sp.getUUID()))
                    .map(org.lupz.doomsdayessentials.guild.Guild::getName).orElse(null);
            if (gname == null) continue;
            counts.put(gname, counts.getOrDefault(gname, 0) + 1);
        }

        // Compute contesting state
        String newController = null;
        boolean newContested = false;
        if (!counts.isEmpty()) {
            // Who has the most players? If tie or >1 guild present, contested
            int max = counts.values().stream().mapToInt(i -> i).max().orElse(0);
            List<String> top = new ArrayList<>();
            for (var en : counts.entrySet()) if (en.getValue() == max) top.add(en.getKey());
            if (top.size() == 1) {
                newController = top.get(0);
                newContested = counts.size() > 1; // other guilds present -> contested even if max unique
            } else {
                newController = null;
                newContested = true;
            }
        } else {
            newController = null; newContested = false;
        }

        // Status change? update marker and multiplier streaks
        boolean markerChanged = false;
        if (!Objects.equals(newController, controllingGuild) || newContested != contested) {
            controllingGuild = newController;
            contested = newContested;
            holdStreakSeconds = 0; // reset streak when status changes
            noControlStreakSeconds = 0;
            markerChanged = true;
        }

        // Accumulate points once per second based on status
        tickCounter++;
        if (tickCounter >= 20) {
            tickCounter = 0;
            if (controllingGuild != null && !contested) {
                // Uncontested hold – increase streak and possibly multiplier
                holdStreakSeconds++;
                if (holdStreakSeconds >= KofhConfig.MULTIPLIER_STEP_SECONDS.get()) {
                    holdStreakSeconds = 0;
                    multiplier = Math.min(KofhConfig.MULTIPLIER_MAX.get(), multiplier + KofhConfig.MULTIPLIER_STEP_AMOUNT.get());
                }
                double add = KofhConfig.POINTS_PER_SECOND.get() * multiplier;
                points.put(controllingGuild, points.getOrDefault(controllingGuild, 0.0) + add);
            } else if (controllingGuild != null) {
                // Contested – optionally add small points, do not decay multiplier
                double addContested = KofhConfig.CONTESTED_POINTS_PER_SECOND.get();
                if (addContested > 0) {
                    points.put(controllingGuild, points.getOrDefault(controllingGuild, 0.0) + addContested);
                }
            } else {
                // Nobody holding – decay multiplier over time
                holdStreakSeconds = 0;
                noControlStreakSeconds++;
                if (noControlStreakSeconds >= KofhConfig.MULTIPLIER_DECAY_STEP_SECONDS.get()) {
                    noControlStreakSeconds = 0;
                    multiplier = Math.max(KofhConfig.MULTIPLIER_MIN.get(), multiplier - KofhConfig.MULTIPLIER_DECAY_STEP_AMOUNT.get());
                }
            }

            remainingSeconds = Math.max(remainingSeconds - 1, 0);

            // Send HUD updates
            sendHudUpdate(server);

            // End round when timer hits zero
            if (remainingSeconds == 0) {
                stop(server);
            }
        }

        // Update world marker when status changes.
        if (markerChanged) {
            broadcastMarker(server);
        }
    }

    private void broadcastMarker(MinecraftServer srv) {
        // Send a world billboard marker at the center of the area.
        if (srv == null || currentArea == null) return;
        sendMarkerToAll(srv, contested ? (byte)1 : (controllingGuild != null ? (byte)2 : (byte)0));
    }

    private void sendMarkerToAll(MinecraftServer srv, byte status) {
        if (currentArea == null) return;
        BlockPos c1 = currentArea.getPos1(); BlockPos c2 = currentArea.getPos2();
        double cx = (c1.getX() + c2.getX()) / 2.0 + 0.5;
        double cy = (c1.getY() + c2.getY()) / 2.0 + 0.5;
        double cz = (c1.getZ() + c2.getZ()) / 2.0 + 0.5;
        TerritoryMarkerPacket pkt = new TerritoryMarkerPacket(currentArea.getName(), cx, cy, cz, status);
        for (ServerPlayer sp : srv.getPlayerList().getPlayers()) {
            safeSend(sp, pkt);
        }
    }

    private void sendHudUpdate(MinecraftServer server) {
        if (!running || currentArea == null) return;
        // Build top 5 scoreboard entries
        List<KofhScorePacket.Entry> entries = points.entrySet().stream()
                .sorted((a,b) -> Double.compare(b.getValue(), a.getValue()))
                .limit(5)
                .map(e -> new KofhScorePacket.Entry(e.getKey(), (int)Math.round(e.getValue())))
                .toList();
        String dimId = currentArea.getDimension().location().toString();
        BlockPos c1 = currentArea.getPos1(); BlockPos c2 = currentArea.getPos2();
        double cx = (c1.getX() + c2.getX()) / 2.0 + 0.5;
        double cy = (c1.getY() + c2.getY()) / 2.0 + 0.5;
        double cz = (c1.getZ() + c2.getZ()) / 2.0 + 0.5;
        KofhScorePacket pkt = new KofhScorePacket(
                currentArea.getName(),
                remainingSeconds,
                controllingGuild,
                contested,
                multiplier,
                entries,
                dimId,
                cx, cy, cz
        );
        for (ServerPlayer sp : server.getPlayerList().getPlayers()) safeSend(sp, pkt);
    }

    private void sendHudUpdateTo(MinecraftServer server, ServerPlayer sp) {
        if (!running || currentArea == null) return;
        List<KofhScorePacket.Entry> entries = points.entrySet().stream()
                .sorted((a,b) -> Double.compare(b.getValue(), a.getValue()))
                .limit(5)
                .map(e -> new KofhScorePacket.Entry(e.getKey(), (int)Math.round(e.getValue())))
                .toList();
        String dimId = currentArea.getDimension().location().toString();
        BlockPos c1 = currentArea.getPos1(); BlockPos c2 = currentArea.getPos2();
        double cx = (c1.getX() + c2.getX()) / 2.0 + 0.5;
        double cy = (c1.getY() + c2.getY()) / 2.0 + 0.5;
        double cz = (c1.getZ() + c2.getZ()) / 2.0 + 0.5;
        KofhScorePacket pkt = new KofhScorePacket(
                currentArea.getName(), remainingSeconds, controllingGuild, contested, multiplier, entries, dimId, cx, cy, cz
        );
        safeSend(sp, pkt);
    }

    private void sendMarkerTo(ServerPlayer sp, byte status) {
        if (currentArea == null) return;
        BlockPos c1 = currentArea.getPos1(); BlockPos c2 = currentArea.getPos2();
        double cx = (c1.getX() + c2.getX()) / 2.0 + 0.5;
        double cy = (c1.getY() + c2.getY()) / 2.0 + 0.5;
        double cz = (c1.getZ() + c2.getZ()) / 2.0 + 0.5;
        TerritoryMarkerPacket pkt = new TerritoryMarkerPacket(currentArea.getName(), cx, cy, cz, status);
        safeSend(sp, pkt);
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer sp)) return;
        MinecraftServer server = sp.getServer();
        if (server == null) return;
        KofhEventManager mgr = KofhEventManager.get();
        if (mgr.running && mgr.currentArea != null) {
            // Send HUD state and marker to late joiners
            mgr.sendHudUpdateTo(server, sp);
            mgr.sendMarkerTo(sp, mgr.contested ? (byte)1 : (mgr.controllingGuild != null ? (byte)2 : (byte)0));
        }
    }

    private static void safeSend(ServerPlayer player, Object pkt) {
        try {
            PacketHandler.CHANNEL.sendTo(pkt, player.connection.connection, net.minecraftforge.network.NetworkDirection.PLAY_TO_CLIENT);
        } catch (Throwable t) {
            EssentialsMod.LOGGER.error("[KOFH] Failed to send packet {} to {}: {}", pkt.getClass().getSimpleName(), player.getGameProfile().getName(), t.toString());
        }
    }
}
