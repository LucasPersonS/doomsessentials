package org.lupz.doomsdayessentials.event.eclipse;

import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lupz.doomsdayessentials.EssentialsMod;
import org.lupz.doomsdayessentials.network.PacketHandler;

import java.util.Random;

@Mod.EventBusSubscriber(modid = EssentialsMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class EclipseCyclesManager {
    private static final Random RNG = new Random();
    private static long nextTriggerAt = System.currentTimeMillis() + 60_000; // first after 1 min

    private EclipseCyclesManager() {}

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent e){
        if (e.phase != TickEvent.Phase.END) return;
        if (!EclipseEventManager.get().isActive()) return;
        long now = System.currentTimeMillis();
        if (now < nextTriggerAt) return;
        nextTriggerAt = now + (9 + RNG.nextInt(7)) * 60_000L; // 9–15 min

        MinecraftServer srv = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();
        if (srv == null) return;

        int type = RNG.nextInt(3); // 0 title, 1 blink, 2 message
        for (ServerPlayer p : srv.getPlayerList().getPlayers()){
            PacketHandler.CHANNEL.send(net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> p), new org.lupz.doomsdayessentials.network.packet.s2c.EclipseCyclePacket(type));
        }
    }
} 