package org.lupz.doomsdayessentials.network.packet.s2c;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import org.lupz.doomsdayessentials.client.MadnessClientState;

import java.util.function.Supplier;

public class MadnessEffectPacket {

    private final int durationTicks;
    private final float shakeIntensity;
    private final float overlayIntensity;

    public MadnessEffectPacket(int durationTicks, float shakeIntensity, float overlayIntensity) {
        this.durationTicks = durationTicks;
        this.shakeIntensity = shakeIntensity;
        this.overlayIntensity = overlayIntensity;
    }

    // ---------------------------------------------------------------------
    // Serialization helpers
    // ---------------------------------------------------------------------
    public static void encode(MadnessEffectPacket pkt, FriendlyByteBuf buf) {
        buf.writeInt(pkt.durationTicks);
        buf.writeFloat(pkt.shakeIntensity);
        buf.writeFloat(pkt.overlayIntensity);
    }

    public static MadnessEffectPacket decode(FriendlyByteBuf buf) {
        int duration = buf.readInt();
        float shake = buf.readFloat();
        float overlay = buf.readFloat();
        return new MadnessEffectPacket(duration, shake, overlay);
    }

    public static void handle(MadnessEffectPacket pkt, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            MadnessClientState.activate(pkt.durationTicks, pkt.shakeIntensity, pkt.overlayIntensity);
        });
        ctx.get().setPacketHandled(true);
    }
} 