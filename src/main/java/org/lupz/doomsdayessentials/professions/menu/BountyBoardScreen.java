package org.lupz.doomsdayessentials.professions.menu;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;

public class BountyBoardScreen extends AbstractContainerScreen<BountyBoardMenu> {
	public BountyBoardScreen(BountyBoardMenu menu, Inventory inv, Component title) {
		super(menu, inv, title);
		this.imageWidth = 176;
		this.imageHeight = 166;
	}

	@Override
	protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
		// No background texture for now; simple placeholder text
	}

	@Override
	public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
		this.renderBackground(g);
		super.render(g, mouseX, mouseY, partialTick);
		String title = this.title.getString();
		g.drawString(this.font, title, (this.width - this.font.width(title)) / 2, this.topPos + 6, 0xFFFFFF, true);
		String line = Component.translatable("menu.doomsdayessentials.bounty_board.placeholder").getString();
		g.drawString(this.font, line, (this.width - this.font.width(line)) / 2, this.topPos + 24, 0xAAAAAA, false);
	}
} 