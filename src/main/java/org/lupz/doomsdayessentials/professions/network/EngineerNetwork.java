package org.lupz.doomsdayessentials.professions.network;

import org.lupz.doomsdayessentials.network.PacketHandler;

public final class EngineerNetwork {
    private EngineerNetwork() {}
    public static void craft(String alias){
        PacketHandler.CHANNEL.sendToServer(new BuyEngineerItemPacket(alias));
    }
} 