package org.lupz.doomsdayessentials.network.packet.s2c;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import org.lupz.doomsdayessentials.client.eclipse.EclipseClientState;

import java.util.function.Supplier;

public record EclipseStatePacket(boolean active, float fogNear, float fogFar, float overlayAlpha) {

    public static void encode(EclipseStatePacket pkt, FriendlyByteBuf buf) {
        buf.writeBoolean(pkt.active());
        buf.writeFloat(pkt.fogNear());
        buf.writeFloat(pkt.fogFar());
        buf.writeFloat(pkt.overlayAlpha());
    }

    public static EclipseStatePacket decode(FriendlyByteBuf buf) {
        return new EclipseStatePacket(buf.readBoolean(), buf.readFloat(), buf.readFloat(), buf.readFloat());
    }

    public static void handle(EclipseStatePacket pkt, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            if (pkt.active()) {
                EclipseClientState.activate(pkt.fogNear(), pkt.fogFar(), pkt.overlayAlpha());
            } else {
                EclipseClientState.deactivate();
            }
        });
        ctx.get().setPacketHandled(true);
    }
} 