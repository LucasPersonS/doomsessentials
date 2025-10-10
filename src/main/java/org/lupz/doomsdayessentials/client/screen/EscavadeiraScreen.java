package org.lupz.doomsdayessentials.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.NotNull;
import org.lupz.doomsdayessentials.menu.EscavadeiraMenu;

public class EscavadeiraScreen extends AbstractContainerScreen<EscavadeiraMenu> {
	private static final ResourceLocation BG = ResourceLocation.withDefaultNamespace("textures/gui/container/generic_54.png");

	public EscavadeiraScreen(EscavadeiraMenu menu, Inventory inv, Component title) {
		super(menu, inv, title);
		this.imageWidth = 176;
		this.imageHeight = 222; // 6 rows
	}

	@Override
	protected void renderBg(@NotNull GuiGraphics graphics, float partialTicks, int mouseX, int mouseY) {
		int x = (this.width - this.imageWidth) / 2;
		int y = (this.height - this.imageHeight) / 2;
		graphics.blit(BG, x, y, 0, 0, this.imageWidth, this.imageHeight);
		// separator line above player inventory
		int sepY = y + 18 + 3 * 18 - 2;
		graphics.fill(x + 7, sepY, x + this.imageWidth - 7, sepY + 2, 0xFFAA0000);
	}

	@Override
	protected void renderLabels(@NotNull GuiGraphics graphics, int mouseX, int mouseY) {
		// do not render default inventory title label
	}

	@Override
	public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
		this.renderBackground(graphics);
		super.render(graphics, mouseX, mouseY, partialTicks);
		this.renderTooltip(graphics, mouseX, mouseY);
	}
} 