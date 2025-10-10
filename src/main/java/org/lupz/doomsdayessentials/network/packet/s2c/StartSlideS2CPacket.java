package org.lupz.doomsdayessentials.network.packet.s2c;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import org.lupz.doomsdayessentials.EssentialsMod;
import org.lupz.doomsdayessentials.client.animation.AnimationManager;
import org.lupz.doomsdayessentials.client.animation.SlidingAnimator;

import java.util.UUID;
import java.util.function.Supplier;

public class StartSlideS2CPacket {
	private final UUID playerId;
	public StartSlideS2CPacket(UUID playerId) { this.playerId = playerId; }
	public StartSlideS2CPacket(FriendlyByteBuf buf) { this.playerId = buf.readUUID(); }
	public void encode(FriendlyByteBuf buf) { buf.writeUUID(playerId); }
	public void handle(Supplier<NetworkEvent.Context> ctx) {
		ctx.get().enqueueWork(() -> {
			var level = Minecraft.getInstance().level;
			if (level == null) return;
			var player = level.getPlayerByUUID(playerId);
			if (player == null) {
				EssentialsMod.LOGGER.debug("[SlideS2C] StartSlideS2C received but player not found: {}", playerId);
				return;
			}
			EssentialsMod.LOGGER.debug("[SlideS2C] Setting SlidingAnimator for {} ({})", player.getGameProfile().getName(), playerId);
			AnimationManager.setAnimator(player, new SlidingAnimator());
		});
		ctx.get().setPacketHandled(true);
	}
} 