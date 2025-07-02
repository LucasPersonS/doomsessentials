package org.lupz.doomsdayessentials.injury.network;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import org.lupz.doomsdayessentials.injury.client.DownedScreen;
import org.lupz.doomsdayessentials.injury.client.InjuryClientState;

import java.util.function.Supplier;

public class UpdateDownedStatePacket {
    public final boolean isDowned;
    public final long downedUntil;

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

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                InjuryClientState.setDowned(this.isDowned, this.downedUntil);
                if (this.isDowned) {
                    Minecraft.getInstance().setScreen(new DownedScreen());
                } else {
                    if (Minecraft.getInstance().screen instanceof DownedScreen) {
                        Minecraft.getInstance().setScreen(null);
                    }
                }
            });
        });
        ctx.get().setPacketHandled(true);
    }
} 