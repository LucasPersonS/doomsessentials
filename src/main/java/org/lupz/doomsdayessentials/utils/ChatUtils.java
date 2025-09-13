package org.lupz.doomsdayessentials.utils;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class ChatUtils {

	public static Component formatMessage(String message) {
		return org.lupz.dooms.core.text.MessageFormatter.formatMessage(message);
	}

	/**
	 * Scans the raw chat message for occurrences of "@<playername>" (case-insensitive)
	 * and notifies those players with a sound and an action-bar message.
	 */
	public static void processMentions(ServerPlayer sender, String rawMessage) {
		org.lupz.dooms.core.chat.MentionService.notifyMentions(sender, rawMessage);
	}
} 