package org.lupz.doomsdayessentials.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import org.lupz.doomsdayessentials.guild.menu.GuildStorageMenu;

public final class ClientPackets {
    private ClientPackets() {}

    public static void handleGuildStorageCounts(org.lupz.doomsdayessentials.guild.menu.GuildStorageCountsPacket msg) {
        Player p = Minecraft.getInstance().player;
        if (p == null) return;
        if (p.containerMenu instanceof GuildStorageMenu menu) {
            menu.setClientCounts(msg.getPage(), msg.getCounts());
        }
    }
}
