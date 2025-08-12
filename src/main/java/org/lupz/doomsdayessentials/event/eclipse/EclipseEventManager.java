package org.lupz.doomsdayessentials.event.eclipse;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.fml.common.Mod;
import org.lupz.doomsdayessentials.EssentialsMod;
import org.lupz.doomsdayessentials.network.PacketHandler;

/**
 * Server-side manager for the Eclipse event lifecycle. Holds the authoritative state
 * and broadcasts updates to clients when the event starts/stops or when settings change.
 */
@Mod.EventBusSubscriber(modid = EssentialsMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class EclipseEventManager {

    private static final EclipseEventManager INSTANCE = new EclipseEventManager();

    private volatile boolean active;
    private volatile float fogNear;
    private volatile float fogFar;
    private volatile float overlayAlpha;

    private EclipseEventManager() {
        // Default minimal darkness: 0 -> 6 blocks
        this.active = false;
        this.fogNear = 0.0f;
        this.fogFar = 6.0f; // blocks
        this.overlayAlpha = 0.45f; // UI darkening overlay (reduced to avoid full black)
    }

    public static EclipseEventManager get() {
        return INSTANCE;
    }

    public boolean isActive() {
        return active;
    }

    public void start(ServerLevel level) {
        if (this.active) return;
        this.active = true;
        broadcast(level);
    }

    public void stop(ServerLevel level) {
        if (!this.active) return;
        this.active = false;
        broadcast(level);
        // Pretty scoreboard message for players with >=5 deaths
        var srv = level.getServer();
        if (srv != null) {
            srv.getPlayerList().getPlayers().forEach(p -> {
                p.getCapability(EclipseScoreCapabilityProvider.SCORE_CAPABILITY).ifPresent(cap -> {
                    if (cap.getDeaths() >= 5) {
                        Component line = Component.literal("[MORTO] ")
                                .withStyle(ChatFormatting.DARK_RED)
                                .append(p.getName().copy().withStyle(ChatFormatting.RED))
                                .append(Component.literal("  ").withStyle(ChatFormatting.GRAY))
                                .append(Component.literal("K:"+cap.getKills()+" D:"+cap.getDeaths()+" S:"+cap.getScore()).withStyle(ChatFormatting.GOLD));
                        srv.getPlayerList().broadcastSystemMessage(line, false);
                    }
                    // Reset scores on stop
                    cap.deserializeNBT(new net.minecraft.nbt.CompoundTag());
                });
            });
        }
    }

    public void setFog(float near, float far, float alpha, ServerLevel level) {
        this.fogNear = Math.max(0.0f, near);
        this.fogFar = Math.max(0.1f, far);
        this.overlayAlpha = Math.max(0.0f, Math.min(1.0f, alpha));
        if (this.active) broadcast(level);
    }

    public void sendTo(ServerPlayer player) {
        PacketHandler.CHANNEL.send(
                net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> player),
                new org.lupz.doomsdayessentials.network.packet.s2c.EclipseStatePacket(active, fogNear, fogFar, overlayAlpha)
        );
    }

    public void broadcast(ServerLevel level) {
        if (level == null || level.getServer() == null) return;
        var list = level.getServer().getPlayerList().getPlayers();
        for (ServerPlayer p : list) {
            sendTo(p);
        }
    }

    // Ensure clients get current eclipse state on login
    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp) {
            var level = sp.serverLevel();
            EclipseEventManager.get().sendTo(sp);
        }
    }

    // Keep eclipse active across server restart by defaulting to active state if it was active before shutdown.
    // A simple persistence hook; for a full persistent config, integrate with a saved data file.
    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        // No persistence file yet; if desired, could read a saved flag here.
        // Current behavior: do nothing; if active flag was kept in memory it remains.
        // Optionally, could auto-start with last used fog settings; requires saved data support.
    }
} 