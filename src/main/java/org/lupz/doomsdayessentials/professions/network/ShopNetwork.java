package org.lupz.doomsdayessentials.professions.network;

import org.lupz.doomsdayessentials.network.PacketHandler;

public final class ShopNetwork {
    private ShopNetwork() {}
    public static void buy(String alias) {
        PacketHandler.CHANNEL.sendToServer(new BuyShopItemPacket(alias));
    }
} 