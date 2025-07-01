package org.lupz.doomsdayessentials.professions.network;

import org.lupz.doomsdayessentials.network.PacketHandler;

public final class ProfessionNetwork {
    private ProfessionNetwork() {}

    public static void selectProfession(String professionId) {
        PacketHandler.CHANNEL.sendToServer(new SelectProfessionPacket(professionId));
    }

    public static void abandonProfession() {
        selectProfession("abandonar");
    }
} 