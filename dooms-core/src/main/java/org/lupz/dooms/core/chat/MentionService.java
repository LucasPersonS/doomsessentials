package org.lupz.dooms.core.chat;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

public final class MentionService {
	private MentionService() {}

	public static void notifyMentions(ServerPlayer sender, String rawMessage) {
		if (sender.getServer() == null) return;
		String lower = rawMessage.toLowerCase();
		for (ServerPlayer target : sender.getServer().getPlayerList().getPlayers()) {
			String name = target.getName().getString();
			if (lower.contains("@" + name.toLowerCase())) {
				target.playNotifySound(SoundEvents.NOTE_BLOCK_PLING.value(), SoundSource.PLAYERS, 1.0F, 1.0F);
				target.displayClientMessage(Component.literal("Alguém te mencionou no chat").withStyle(ChatFormatting.GREEN), true);
			}
		}
	}
} 