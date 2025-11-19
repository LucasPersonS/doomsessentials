package org.lupz.doomsdayessentials.network.packet.s2c;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Server-to-client packet carrying the KOFH scoreboard and status.
 */
public class KofhScorePacket {
    public final String areaName;
    public final int remainingSeconds;
    public final String controllingGuild; // may be null
    public final boolean contested;
    public final double multiplier;
    public final List<Entry> entries; // top N guild scores
    public final String dimensionId; // e.g., minecraft:overworld
    public final double centerX, centerY, centerZ;

    public KofhScorePacket(String areaName,
                           int remainingSeconds,
                           String controllingGuild,
                           boolean contested,
                           double multiplier,
                           List<Entry> entries,
                           String dimensionId,
                           double centerX, double centerY, double centerZ) {
        this.areaName = areaName;
        this.remainingSeconds = remainingSeconds;
        this.controllingGuild = controllingGuild;
        this.contested = contested;
        this.multiplier = multiplier;
        this.entries = entries;
        this.dimensionId = dimensionId;
        this.centerX = centerX;
        this.centerY = centerY;
        this.centerZ = centerZ;
    }

    public static void encode(KofhScorePacket pkt, FriendlyByteBuf buf) {
        buf.writeUtf(pkt.areaName);
        buf.writeInt(pkt.remainingSeconds);
        buf.writeBoolean(pkt.controllingGuild != null);
        if (pkt.controllingGuild != null) buf.writeUtf(pkt.controllingGuild);
        buf.writeBoolean(pkt.contested);
        buf.writeDouble(pkt.multiplier);
        buf.writeInt(pkt.entries.size());
        for (Entry e : pkt.entries) {
            buf.writeUtf(e.guildName);
            buf.writeInt(e.points);
        }
        buf.writeUtf(pkt.dimensionId);
        buf.writeDouble(pkt.centerX);
        buf.writeDouble(pkt.centerY);
        buf.writeDouble(pkt.centerZ);
    }

    public static KofhScorePacket decode(FriendlyByteBuf buf) {
        String area = buf.readUtf();
        int secs = buf.readInt();
        String controller = null;
        boolean hasController = buf.readBoolean();
        if (hasController) controller = buf.readUtf();
        boolean contested = buf.readBoolean();
        double mult = buf.readDouble();
        int n = buf.readInt();
        List<Entry> entries = new ArrayList<>(n);
        for (int i=0;i<n;i++) {
            String g = buf.readUtf();
            int p = buf.readInt();
            entries.add(new Entry(g, p));
        }
        String dimId = buf.readUtf();
        double cx = buf.readDouble();
        double cy = buf.readDouble();
        double cz = buf.readDouble();
        return new KofhScorePacket(area, secs, controller, contested, mult, entries, dimId, cx, cy, cz);
    }

    public static void handle(KofhScorePacket pkt, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            org.lupz.doomsdayessentials.kofh.client.KofhClientState.update(pkt);
        });
        ctx.get().setPacketHandled(true);
    }

    public static class Entry {
        public final String guildName;
        public final int points;
        public Entry(String guildName, int points) { this.guildName = guildName; this.points = points; }
    }
}
