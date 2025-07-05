package org.lupz.doomsdayessentials.network.packet.s2c;

import net.minecraft.network.FriendlyByteBuf;
import java.util.function.Supplier;
import net.minecraftforge.network.NetworkEvent;

public class TerritoryProgressPacket {
    private final String areaName;
    private final String guildName;
    private final int progress;
    private final int total;
    private final boolean running;

    public TerritoryProgressPacket(String areaName, String guildName, int progress, int total, boolean running) {
        this.areaName = areaName; this.guildName = guildName; this.progress = progress; this.total = total; this.running = running;
    }

    public static void encode(TerritoryProgressPacket pkt, FriendlyByteBuf buf) {
        buf.writeUtf(pkt.areaName);
        buf.writeUtf(pkt.guildName != null ? pkt.guildName : "");
        buf.writeInt(pkt.progress);
        buf.writeInt(pkt.total);
        buf.writeBoolean(pkt.running);
    }

    public static TerritoryProgressPacket decode(FriendlyByteBuf buf) {
        String area = buf.readUtf();
        String guild = buf.readUtf();
        int prog = buf.readInt();
        int tot = buf.readInt();
        boolean run = buf.readBoolean();
        return new TerritoryProgressPacket(area, guild.isEmpty()?null:guild, prog, tot, run);
    }

    public static void handle(TerritoryProgressPacket pkt, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            org.lupz.doomsdayessentials.territory.client.TerritoryClientState.update(pkt);
        });
        ctx.get().setPacketHandled(true);
    }

    // Getters
    public String getAreaName(){return areaName;} public String getGuildName(){return guildName;} public int getProgress(){return progress;} public int getTotal(){return total;} public boolean isRunning(){return running;}
} 