package org.lupz.doomsdayessentials.utils;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

public class ChatUtils {

	public static Component formatMessage(String message) {
		try {
			return org.lupz.dooms.core.text.MessageFormatter.formatMessage(message);
		} catch (Throwable t) {
			return fallbackFormatMessage(message);
		}
	}

	/**
	 * Scans the raw chat message for occurrences of "@<playername>" (case-insensitive)
	 * and notifies those players with a sound and an action-bar message.
	 */
	public static void processMentions(ServerPlayer sender, String rawMessage) {
		try {
			org.lupz.dooms.core.chat.MentionService.notifyMentions(sender, rawMessage);
		} catch (NoClassDefFoundError e) {
			// Core dependency not available, skip mention processing
		}
	}

	private static Component fallbackFormatMessage(String message) {
		MutableComponent mainComponent = Component.literal("");
		StringBuilder currentText = new StringBuilder();
		Style currentStyle = Style.EMPTY;

		for (int i = 0; i < message.length(); i++) {
			char c = message.charAt(i);

			if (c == '&' && i + 1 < message.length()) {
				char formatChar = message.charAt(i + 1);
				ChatFormatting format = ChatFormatting.getByCode(formatChar);

				if (format != null) {
					if (currentText.length() > 0) {
						mainComponent.append(Component.literal(currentText.toString()).withStyle(currentStyle));
						currentText = new StringBuilder();
					}

					if (format.isColor()) {
						currentStyle = Style.EMPTY.withColor(format);
					} else if (format == ChatFormatting.RESET) {
						currentStyle = Style.EMPTY;
					} else {
						currentStyle = currentStyle.applyFormat(format);
					}

					i++;
					continue;
				}
			}

			currentText.append(c);
		}

		if (currentText.length() > 0) {
			mainComponent.append(Component.literal(currentText.toString()).withStyle(currentStyle));
		}

		return mainComponent;
	}
} 