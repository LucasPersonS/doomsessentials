package org.lupz.doomsdayessentials.professions.menu;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.client.gui.GuiGraphics;

public class BountyBoardScreen extends AbstractContainerScreen<BountyBoardMenu> {
	private static final net.minecraft.resources.ResourceLocation CHEST_LOCATION = net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("minecraft", "textures/gui/container/generic_54.png");

	public BountyBoardScreen(BountyBoardMenu menu, Inventory inv, Component title) {
		super(menu, inv, title);
		this.imageWidth = 176;
		this.imageHeight = 222; // double chest height
	}

	@Override
	protected void init() {
		super.init();
	}

	@Override
	protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
		int left = (this.width - this.imageWidth) / 2;
		int top = (this.height - this.imageHeight) / 2;
		// Chest background (6 rows)
		g.blit(CHEST_LOCATION, left, top, 0, 0, this.imageWidth, 17);
		for (int row = 0; row < 6; ++row) {
			g.blit(CHEST_LOCATION, left, top + 17 + row * 18, 0, 17, this.imageWidth, 18);
		}
		g.blit(CHEST_LOCATION, left, top + 17 + 6 * 18, 0, 215, this.imageWidth, 7);
	}

	@Override
	public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
		this.renderBackground(g);
		super.render(g, mouseX, mouseY, partialTick);
		// No extra texts; controls are player-head items inside the container slots
		this.renderTooltip(g, mouseX, mouseY);
	}
} 