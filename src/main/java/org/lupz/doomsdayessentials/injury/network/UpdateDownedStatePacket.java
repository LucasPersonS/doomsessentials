package org.lupz.doomsdayessentials.injury.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import org.lupz.doomsdayessentials.injury.client.InjuryClientPacketHandler;

import java.util.function.Supplier;

public class UpdateDownedStatePacket {
    private final boolean isDowned;
    private final long downedUntil;

    public UpdateDownedStatePacket(boolean isDowned, long downedUntil) {
        this.isDowned = isDowned;
        this.downedUntil = downedUntil;
    }

    public UpdateDownedStatePacket(FriendlyByteBuf buf) {
        this.isDowned = buf.readBoolean();
        this.downedUntil = buf.readLong();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBoolean(this.isDowned);
        buf.writeLong(this.downedUntil);
    }

    public boolean isDowned() {
        return isDowned;
    }

    public long getDownedUntil() {
        return downedUntil;
    }

    public static void handle(UpdateDownedStatePacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> InjuryClientPacketHandler.handleUpdateDownedState(msg));
        });
        ctx.get().setPacketHandled(true);
    }
} 