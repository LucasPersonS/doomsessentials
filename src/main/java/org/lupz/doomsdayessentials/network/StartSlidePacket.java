package org.lupz.doomsdayessentials.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import org.lupz.doomsdayessentials.movement.SlideHandler;

import java.util.function.Supplier;

public class StartSlidePacket {
	public StartSlidePacket() {}

	public StartSlidePacket(FriendlyByteBuf buf) {
		// no payload
	}

	public void encode(FriendlyByteBuf buf) {
		// no payload
	}

	public void handle(Supplier<NetworkEvent.Context> ctx) {
		ctx.get().enqueueWork(() -> {
			ServerPlayer player = ctx.get().getSender();
			if (player == null) return;
			org.lupz.doomsdayessentials.EssentialsMod.LOGGER.debug("[SlideServer] StartSlidePacket received from {}", player.getGameProfile().getName());
			SlideHandler.tryStartSlide(player);
		});
		ctx.get().setPacketHandled(true);
	}
} 