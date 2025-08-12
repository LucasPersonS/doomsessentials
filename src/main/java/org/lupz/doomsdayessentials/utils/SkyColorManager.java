package org.lupz.doomsdayessentials.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.lupz.doomsdayessentials.EssentialsMod;
import org.lupz.doomsdayessentials.network.PacketHandler;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Persists day/night sky tint colors and ensures they are sent to all players on login and when day phase changes.
 */
@Mod.EventBusSubscriber(modid = EssentialsMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class SkyColorManager {

    private static final SkyColorManager INSTANCE = new SkyColorManager();
    public static SkyColorManager get() { return INSTANCE; }

    private final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private final Path savePath;

    private int dayColor = 0;
    private float dayAlpha = 1.0f;
    private int nightColor = 0;
    private float nightAlpha = 1.0f;

    private boolean lastDaytime = true;
    private long lastCheck = 0;

    private SkyColorManager() {
        savePath = FMLPaths.CONFIGDIR.get().resolve("doomsdayessentials/sky_color.json");
        load();
        MinecraftForge.EVENT_BUS.register(this);
    }

    // ----------------------------------------------------------------------------------
    // Public API
    // ----------------------------------------------------------------------------------
    public void setDay(int color, float alpha) { dayColor = color; dayAlpha = alpha; save(); broadcast(); }
    public void setNight(int color, float alpha) { nightColor = color; nightAlpha = alpha; save(); broadcast(); }
    public void clear() { dayColor = nightColor = 0; dayAlpha = nightAlpha = 1.0f; save(); broadcast(); }

    // ----------------------------------------------------------------------------------
    // Event handlers
    // ----------------------------------------------------------------------------------
    @SubscribeEvent
    public void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer sp)) return;
        sendTo(sp);
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        long now = System.currentTimeMillis();
        if (now - lastCheck < 60_000) return; // once per minute
        lastCheck = now;
        ServerLevel level = ServerLifecycleHooks.getCurrentServer().overworld();
        if (level == null) return;
        boolean isDay = level.isDay();
        if (isDay != lastDaytime) {
            lastDaytime = isDay;
            broadcast();
        }
    }

    // ----------------------------------------------------------------------------------
    // Internal helpers
    // ----------------------------------------------------------------------------------
    private void sendTo(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        boolean isDay = level.isDay();
        int color = isDay ? dayColor : nightColor;
        float alpha = isDay ? dayAlpha : nightAlpha;
        PacketHandler.CHANNEL.send(net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> player),
                new org.lupz.doomsdayessentials.network.packet.s2c.SkyTintPacket(color, alpha));
    }

    private void broadcast() {
        var srv = ServerLifecycleHooks.getCurrentServer();
        if (srv == null) return;
        boolean isDay = srv.overworld().isDay();
        int color = isDay ? dayColor : nightColor;
        float alpha = isDay ? dayAlpha : nightAlpha;
        srv.getPlayerList().getPlayers().forEach(p -> PacketHandler.CHANNEL.send(
                net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> p),
                new org.lupz.doomsdayessentials.network.packet.s2c.SkyTintPacket(color, alpha)));
    }

    private void load() {
        try {
            Files.createDirectories(savePath.getParent());
            if (!Files.exists(savePath)) { save(); return; }
            String json = Files.readString(savePath);
            if (json.isEmpty()) return;
            JsonObject root = GSON.fromJson(json, JsonObject.class);
            dayColor = root.has("dayColor") ? root.get("dayColor").getAsInt() : 0;
            dayAlpha = root.has("dayAlpha") ? root.get("dayAlpha").getAsFloat() : 1.0f;
            nightColor = root.has("nightColor") ? root.get("nightColor").getAsInt() : 0;
            nightAlpha = root.has("nightAlpha") ? root.get("nightAlpha").getAsFloat() : 1.0f;
        } catch (IOException e) { e.printStackTrace(); }
    }

    private void save() {
        JsonObject root = new JsonObject();
        root.addProperty("dayColor", dayColor);
        root.addProperty("dayAlpha", dayAlpha);
        root.addProperty("nightColor", nightColor);
        root.addProperty("nightAlpha", nightAlpha);
        try { Files.writeString(savePath, GSON.toJson(root)); } catch (IOException e) { e.printStackTrace(); }
    }
} 