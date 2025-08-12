package org.lupz.doomsdayessentials.network.packet.s2c;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkEvent;
import org.lupz.doomsdayessentials.client.danger.DangerZoneClientState;

import java.util.function.Supplier;

public record ClosedZonePacket(String time, boolean show) {
    public static void encode(ClosedZonePacket pkt, FriendlyByteBuf buf){
        buf.writeUtf(pkt.time);
        buf.writeBoolean(pkt.show);
    }
    public static ClosedZonePacket decode(FriendlyByteBuf buf){
        return new ClosedZonePacket(buf.readUtf(), buf.readBoolean());
    }
    public static void handle(ClosedZonePacket pkt, Supplier<NetworkEvent.Context> ctx){
        ctx.get().enqueueWork(() -> {
            if(pkt.show) DangerZoneClientState.show(pkt.time, 100); // 5s
            else DangerZoneClientState.hide();
        });
        ctx.get().setPacketHandled(true);
    }
} 