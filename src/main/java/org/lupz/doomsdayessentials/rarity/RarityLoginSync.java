package org.lupz.doomsdayessentials.rarity;

import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lupz.doomsdayessentials.EssentialsMod;
import org.lupz.doomsdayessentials.network.PacketHandler;
import org.lupz.doomsdayessentials.network.packet.s2c.SyncRarityMapPacket;

@Mod.EventBusSubscriber(modid = EssentialsMod.MOD_ID)
public final class RarityLoginSync {
    private RarityLoginSync() {}

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent e) {
        if (!(e.getEntity() instanceof net.minecraft.server.level.ServerPlayer sp)) return;
        SyncRarityMapPacket pkt = SyncRarityMapPacket.full(RarityServerRegistry.snapshot(), RarityServerRegistry.snapshotVariants());
        PacketHandler.CHANNEL.sendTo(pkt, sp.connection.connection, net.minecraftforge.network.NetworkDirection.PLAY_TO_CLIENT);
    }
}
