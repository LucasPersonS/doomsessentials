package org.lupz.doomsdayessentials.client.key;

import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;

public final class KeyBindings {
	private KeyBindings() {}

	public static final KeyMapping USE_SKILL = new KeyMapping(
		"key.doomsdayessentials.use_skill",
		GLFW.GLFW_KEY_G,
		"key.categories.gameplay"
	);
} 