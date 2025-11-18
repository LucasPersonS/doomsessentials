package org.lupz.doomsdayessentials.guild.menu;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class GuildStorageCountsPacket {
    private final int page;
    private final int[] counts;

    public GuildStorageCountsPacket(int page, int[] counts) {
        this.page = page;
        this.counts = counts;
    }
    public int getPage() { return page; }
    public int[] getCounts() { return counts; }

    public static void encode(GuildStorageCountsPacket msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.page);
        buf.writeVarInt(msg.counts.length);
        for (int i = 0; i < msg.counts.length; i++) buf.writeVarInt(msg.counts[i]);
    }

    public static GuildStorageCountsPacket decode(FriendlyByteBuf buf) {
        int page = buf.readVarInt();
        int len = buf.readVarInt();
        int[] counts = new int[len];
        for (int i = 0; i < len; i++) counts[i] = buf.readVarInt();
        return new GuildStorageCountsPacket(page, counts);
    }

    public static void handle(GuildStorageCountsPacket msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context c = ctx.get();
        c.enqueueWork(() -> org.lupz.doomsdayessentials.client.ClientPackets.handleGuildStorageCounts(msg));
        c.setPacketHandled(true);
    }
}
