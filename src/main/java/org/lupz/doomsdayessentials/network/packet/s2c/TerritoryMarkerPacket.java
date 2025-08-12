package org.lupz.doomsdayessentials.network.packet.s2c;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Sent by the server each time the contested/capturing status of a territory event changes
 * or when the event ends. Status values:
 * 0 = remove marker, 1 = contested, 2 = capturing (dominating).
 */
public record TerritoryMarkerPacket(String areaName, double cx, double cy, double cz, byte status) {

    public static void encode(TerritoryMarkerPacket pkt, FriendlyByteBuf buf) {
        buf.writeUtf(pkt.areaName);
        buf.writeDouble(pkt.cx);
        buf.writeDouble(pkt.cy);
        buf.writeDouble(pkt.cz);
        buf.writeByte(pkt.status);
    }

    public static TerritoryMarkerPacket decode(FriendlyByteBuf buf) {
        String a=buf.readUtf();
        double x=buf.readDouble();
        double y=buf.readDouble();
        double z=buf.readDouble();
        byte s=buf.readByte();
        return new TerritoryMarkerPacket(a,x,y,z,s);
    }

    public static void handle(TerritoryMarkerPacket pkt, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            org.lupz.doomsdayessentials.territory.client.TerritoryMarkerClient.handlePacket(pkt);
        });
        ctx.get().setPacketHandled(true);
    }
} 