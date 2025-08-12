package org.lupz.doomsdayessentials.network.packet.s2c;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.network.NetworkEvent;
import org.lupz.doomsdayessentials.sound.ModSounds;

import java.util.function.Supplier;

public record EclipseCyclePacket(int type) {
    public static void encode(EclipseCyclePacket pkt, FriendlyByteBuf buf){ buf.writeInt(pkt.type()); }
    public static EclipseCyclePacket decode(FriendlyByteBuf buf){ return new EclipseCyclePacket(buf.readInt()); }
    public static void handle(EclipseCyclePacket pkt, Supplier<NetworkEvent.Context> ctx){
        ctx.get().enqueueWork(() -> {
            var mc = net.minecraft.client.Minecraft.getInstance();
            switch (pkt.type()){
                case 0 -> { // Title flash
                    mc.gui.clear();
                    mc.gui.setTitle(Component.literal("..."));
                }
                case 1 -> { // Quick black blink by toggling gamma overlay alpha
                    // We piggyback on the eclipse overlay by briefly increasing alpha
                    org.lupz.doomsdayessentials.client.eclipse.EclipseClientState.activate(
                            org.lupz.doomsdayessentials.client.eclipse.EclipseClientState.getFogNear(),
                            org.lupz.doomsdayessentials.client.eclipse.EclipseClientState.getFogFar(),
                            Math.min(1.0f, org.lupz.doomsdayessentials.client.eclipse.EclipseClientState.getOverlayAlpha() + 0.2f)
                    );
                }
                case 2 -> { // Static whisper
                    if (ModSounds.FREQUENCIA1.isPresent()) {
                        var p = mc.player;
                        if (p != null) p.playNotifySound(ModSounds.FREQUENCIA1.get(), SoundSource.AMBIENT, 0.6f, 1.0f);
                    }
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }
} 