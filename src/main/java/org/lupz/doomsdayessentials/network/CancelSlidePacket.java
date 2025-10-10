package org.lupz.doomsdayessentials.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import org.lupz.doomsdayessentials.movement.SlideHandler;

import java.util.function.Supplier;

public class CancelSlidePacket {
	public CancelSlidePacket() {}
	public CancelSlidePacket(FriendlyByteBuf buf) {}
	public void encode(FriendlyByteBuf buf) {}

	public void handle(Supplier<NetworkEvent.Context> ctx) {
		ctx.get().enqueueWork(() -> {
			ServerPlayer player = ctx.get().getSender();
			if (player == null) return;
			org.lupz.doomsdayessentials.EssentialsMod.LOGGER.debug("[SlideServer] CancelSlidePacket received from {}", player.getGameProfile().getName());
			SlideHandler.cancelSlide(player);
		});
		ctx.get().setPacketHandled(true);
	}
} 