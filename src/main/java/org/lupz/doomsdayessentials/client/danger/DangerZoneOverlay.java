package org.lupz.doomsdayessentials.client.danger;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;
import org.lupz.doomsdayessentials.EssentialsMod;

public class DangerZoneOverlay {

	private static final ResourceLocation TEX = ResourceLocation.fromNamespaceAndPath(EssentialsMod.MOD_ID, "textures/gui/closed_zone.png");

	public static final IGuiOverlay OVERLAY = (gui, gfx, partial, width, height) -> {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null || mc.level == null) return;
		if (!DangerZoneClientState.isActive()) return;
		RenderSystem.enableBlend();
		int imgW = 128, imgH = 64;
		int x = (width - imgW) / 2;
		int y = height / 3;
		gfx.blit(TEX, x, y, 0, 0, imgW, imgH, imgW, imgH);
		String timeTxt = DangerZoneClientState.getTimeText().replace(":", "h");
		Component txtComp = Component.literal("Abre as " + timeTxt)
				.withStyle(style -> style.withBold(true)
						.withColor(net.minecraft.network.chat.TextColor.fromRgb(0x006400))
						.withFont(ResourceLocation.fromNamespaceAndPath(org.lupz.doomsdayessentials.EssentialsMod.MOD_ID, "pixelgame")));
		Font font = Minecraft.getInstance().font;
		int txtW = font.width(txtComp);
		gfx.drawString(font, txtComp, width / 2 - txtW / 2, y + imgH + 5, 0x00FF00);
		RenderSystem.disableBlend();
	};

	private static void drawText(GuiGraphics g, Component text, int x, int y) {
		Font font = Minecraft.getInstance().font;
		g.drawString(font, text, x, y, 0xFFFFFF);
	}

	private static Component styled(Component c) {
		return c.copy().withStyle(s -> s.withFont(ResourceLocation.fromNamespaceAndPath(org.lupz.doomsdayessentials.EssentialsMod.MOD_ID, "pixelgame")));
	}
} 