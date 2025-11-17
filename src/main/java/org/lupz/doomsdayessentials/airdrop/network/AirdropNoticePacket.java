package org.lupz.doomsdayessentials.airdrop.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * S2C packet to show a typing-animated HUD message when Airdrop is opened or despawns.
 */
public class AirdropNoticePacket {
    public static final byte STATE_OPENED = 0;
    public static final byte STATE_DESPAWNED = 1;
    public static final byte STATE_LANDED = 2;

    private final String messageKey;
    private final String[] args;
    private final byte state;

    public AirdropNoticePacket(String messageKey, byte state, String... args) {
        this.messageKey = messageKey;
        this.args = args == null ? new String[0] : args;
        this.state = state;
    }

    public static void encode(AirdropNoticePacket msg, FriendlyByteBuf buf) {
        buf.writeUtf(msg.messageKey);
        buf.writeVarInt(msg.args.length);
        for (String a : msg.args) buf.writeUtf(a);
        buf.writeByte(msg.state);
    }

    public static AirdropNoticePacket decode(FriendlyByteBuf buf) {
        String key = buf.readUtf();
        int n = buf.readVarInt();
        String[] args = new String[Math.max(0, n)];
        for (int i = 0; i < args.length; i++) args[i] = buf.readUtf();
        byte s = buf.readByte();
        return new AirdropNoticePacket(key, s, args);
    }

    public static void handle(AirdropNoticePacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            org.lupz.doomsdayessentials.airdrop.client.AirdropTypingOverlay.showKey(msg.messageKey, msg.state, msg.args);
        });
        ctx.get().setPacketHandled(true);
    }
}
