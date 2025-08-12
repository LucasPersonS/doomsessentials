package org.lupz.doomsdayessentials.network.packet.s2c;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import org.lupz.doomsdayessentials.frequency.capability.FrequencyCapabilityProvider;

import java.util.function.Supplier;

public class SyncFrequencyPacket {
    private final int level;
    private final boolean soundsEnabled;
    private final boolean imagesEnabled;

    public SyncFrequencyPacket(int level) {
        this(level, true, true);
    }

    public SyncFrequencyPacket(int level, boolean soundsEnabled, boolean imagesEnabled) {
        this.level = Math.max(0, Math.min(100, level));
        this.soundsEnabled = soundsEnabled;
        this.imagesEnabled = imagesEnabled;
    }

    public static void encode(SyncFrequencyPacket pkt, FriendlyByteBuf buf) {
        buf.writeVarInt(pkt.level);
        buf.writeBoolean(pkt.soundsEnabled);
        buf.writeBoolean(pkt.imagesEnabled);
    }

    public static SyncFrequencyPacket decode(FriendlyByteBuf buf) {
        int lvl = buf.readVarInt();
        boolean snd = buf.readBoolean();
        boolean img = buf.readBoolean();
        return new SyncFrequencyPacket(lvl, snd, img);
    }

    public static void handle(SyncFrequencyPacket pkt, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            var mc = net.minecraft.client.Minecraft.getInstance();
            if (mc.player == null) return;
            mc.player.getCapability(FrequencyCapabilityProvider.FREQUENCY_CAPABILITY).ifPresent(cap -> {
                cap.setLevel(pkt.level);
                cap.setLastServerUpdateMs(System.currentTimeMillis());
                cap.setSoundsEnabled(pkt.soundsEnabled);
                cap.setImagesEnabled(pkt.imagesEnabled);
            });
        });
        ctx.get().setPacketHandled(true);
    }
} 