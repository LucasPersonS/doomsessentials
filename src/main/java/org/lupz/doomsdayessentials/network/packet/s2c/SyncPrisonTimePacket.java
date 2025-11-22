package org.lupz.doomsdayessentials.network.packet.s2c;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import org.lupz.doomsdayessentials.client.ClientCombatState;

import java.util.function.Supplier;

public class SyncPrisonTimePacket {

    private final int remainingSeconds;

    public SyncPrisonTimePacket(int remainingSeconds) {
        this.remainingSeconds = remainingSeconds;
    }

    public static void encode(SyncPrisonTimePacket pkt, FriendlyByteBuf buf) {
        buf.writeVarInt(pkt.remainingSeconds);
    }

    public static SyncPrisonTimePacket decode(FriendlyByteBuf buf) {
        return new SyncPrisonTimePacket(buf.readVarInt());
    }

    public static void handle(SyncPrisonTimePacket pkt, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> ClientCombatState.setPrisonTimeRemaining(pkt.remainingSeconds));
        ctx.get().setPacketHandled(true);
    }
}

