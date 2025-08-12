package org.lupz.doomsdayessentials.network.packet.s2c;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import org.lupz.doomsdayessentials.client.SkyTintClientState;

import java.util.function.Supplier;

public record SkyTintPacket(int color, float alpha) {

    public static void encode(SkyTintPacket pkt, FriendlyByteBuf buf) {
        buf.writeInt(pkt.color());
        buf.writeFloat(pkt.alpha());
    }

    public static SkyTintPacket decode(FriendlyByteBuf buf) {
        return new SkyTintPacket(buf.readInt(), buf.readFloat());
    }

    public static void handle(SkyTintPacket pkt, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> SkyTintClientState.set(pkt.color(), pkt.alpha()));
        ctx.get().setPacketHandled(true);
    }
} 