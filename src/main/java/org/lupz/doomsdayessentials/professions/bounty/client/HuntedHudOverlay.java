package org.lupz.doomsdayessentials.professions.bounty.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;
import net.minecraftforge.fml.common.Mod;
import org.lupz.doomsdayessentials.EssentialsMod;

@Mod.EventBusSubscriber(modid = EssentialsMod.MOD_ID, value = Dist.CLIENT)
public final class HuntedHudOverlay {
	private HuntedHudOverlay() {}

	public static final IGuiOverlay HUD = (gui, guiGraphics, partialTicks, screenWidth, screenHeight) -> {
		if (!HuntedClientState.isHunted()) return;
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null || mc.options.hideGui) return;
		Font font = mc.font;
		String text = net.minecraft.network.chat.Component.translatable("hud.doomsdayessentials.hunted").getString();
		int textWidth = font.width(text);
		int x = (screenWidth - textWidth) / 2;
		int y = screenHeight - 59; // similar to other overlays above hotbar
		// Red with shadow
		guiGraphics.drawString(font, text, x, y, 0xFFFF5555, true);
	};
} 