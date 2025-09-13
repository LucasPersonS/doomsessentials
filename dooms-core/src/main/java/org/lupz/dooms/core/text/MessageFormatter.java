package org.lupz.dooms.core.text;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

public final class MessageFormatter {
	private MessageFormatter() {}

	public static Component formatMessage(String message) {
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