package org.lupz.doomsdayessentials.client.key;

import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;

public final class KeyBindings {
    private KeyBindings() {}

    // Custom category for all Doomsday Essentials keybinds (shows as its own group in Controls)
    public static final String CATEGORY_DOOMSDAY = "key.categories.doomsdayessentials";

    public static final KeyMapping USE_SKILL = new KeyMapping(
        "key.doomsdayessentials.use_skill",
        GLFW.GLFW_KEY_G,
        CATEGORY_DOOMSDAY
    );
}
