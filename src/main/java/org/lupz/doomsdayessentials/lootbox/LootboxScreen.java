package org.lupz.doomsdayessentials.lootbox;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class LootboxScreen extends AbstractContainerScreen<LootboxMenu> {
	private static final ResourceLocation CHEST_LOCATION = ResourceLocation.fromNamespaceAndPath("minecraft", "textures/gui/container/generic_54.png");

	public LootboxScreen(LootboxMenu menu, Inventory inv, Component title) {
		super(menu, inv, title);
		this.imageWidth = 176;
		this.imageHeight = 3 * 18 + 17 + 7 + 12 + (3 * 18 + 22); // showcase + gap + inv + hotbar
	}

	@Override
	protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
		int left = (this.width - this.imageWidth) / 2;
		int top = (this.height - this.imageHeight) / 2;
		// Showcase 3 rows
		g.blit(CHEST_LOCATION, left, top, 0, 0, this.imageWidth, 17);
		for (int row = 0; row < 3; ++row) {
			g.blit(CHEST_LOCATION, left, top + 17 + row * 18, 0, 17, this.imageWidth, 18);
		}
		g.blit(CHEST_LOCATION, left, top + 17 + 3 * 18, 0, 215, this.imageWidth, 7);
		// Player inventory background (reuse slices)
		int invTop = top + 17 + 3 * 18 + 12;
		for (int row = 0; row < 3; ++row) {
			g.blit(CHEST_LOCATION, left, invTop + row * 18, 0, 17, this.imageWidth, 18);
		}
		g.blit(CHEST_LOCATION, left, invTop + 3 * 18, 0, 215, this.imageWidth, 7);
		// Hotbar row
		g.blit(CHEST_LOCATION, left, invTop + 3 * 18 + 7, 0, 17, this.imageWidth, 18);
	}

	@Override
	public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
		this.renderBackground(g);
		super.render(g, mouseX, mouseY, partialTick);
		this.renderTooltip(g, mouseX, mouseY);
	}

	@Override
	public void containerTick() {
		super.containerTick();
		this.menu.clientTickAnimate();
	}
} 