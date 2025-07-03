package org.lupz.doomsdayessentials.network;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;
import org.lupz.doomsdayessentials.EssentialsMod;
import org.lupz.doomsdayessentials.killfeed.network.packet.KillFeedPacket;
import org.lupz.doomsdayessentials.network.packet.s2c.SyncAreasPacket;
import org.lupz.doomsdayessentials.network.packet.s2c.SyncCombatStatePacket;
import org.lupz.doomsdayessentials.professions.network.SelectProfessionPacket;
import org.lupz.doomsdayessentials.professions.network.BuyShopItemPacket;

public class PacketHandler {

    private static final String PROTOCOL_VERSION = "1";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            ResourceLocation.fromNamespaceAndPath(EssentialsMod.MOD_ID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals);

    private static int index = 0;

    public static void register() {
        CHANNEL.messageBuilder(SyncAreasPacket.class, index++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SyncAreasPacket::encode)
                .decoder(SyncAreasPacket::decode)
                .consumerMainThread(SyncAreasPacket::handle)
                .add();

        CHANNEL.messageBuilder(SyncCombatStatePacket.class, index++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SyncCombatStatePacket::encode)
                .decoder(SyncCombatStatePacket::decode)
                .consumerMainThread(SyncCombatStatePacket::handle)
                .add();

        CHANNEL.messageBuilder(KillFeedPacket.class, index++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(KillFeedPacket::toBytes)
                .decoder(KillFeedPacket::new)
                .consumerMainThread(KillFeedPacket::handle)
                .add();

        CHANNEL.messageBuilder(SelectProfessionPacket.class, index++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(SelectProfessionPacket::encode)
                .decoder(SelectProfessionPacket::decode)
                .consumerMainThread(SelectProfessionPacket::handle)
                .add();

        CHANNEL.messageBuilder(BuyShopItemPacket.class, index++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(BuyShopItemPacket::encode)
                .decoder(BuyShopItemPacket::decode)
                .consumerMainThread(BuyShopItemPacket::handle)
                .add();
    }
} 