package org.lupz.doomsdayessentials.professions.bounty;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class HuntedStatePacket {
	private final boolean hunted;
	private final long untilEpochMs;

	public HuntedStatePacket(boolean hunted, long untilEpochMs) {
		this.hunted = hunted;
		this.untilEpochMs = untilEpochMs;
	}

	public static void encode(HuntedStatePacket msg, FriendlyByteBuf buf) {
		buf.writeBoolean(msg.hunted);
		buf.writeLong(msg.untilEpochMs);
	}

	public static HuntedStatePacket decode(FriendlyByteBuf buf) {
		boolean hunted = buf.readBoolean();
		long until = buf.readLong();
		return new HuntedStatePacket(hunted, until);
	}

	public static void handle(HuntedStatePacket msg, Supplier<NetworkEvent.Context> ctx) {
		ctx.get().enqueueWork(() -> applyClient(msg.hunted, msg.untilEpochMs));
		ctx.get().setPacketHandled(true);
	}

	@OnlyIn(Dist.CLIENT)
	private static void applyClient(boolean hunted, long untilEpochMs) {
		org.lupz.doomsdayessentials.professions.bounty.client.HuntedClientState.setHunted(hunted, untilEpochMs);
	}
} 