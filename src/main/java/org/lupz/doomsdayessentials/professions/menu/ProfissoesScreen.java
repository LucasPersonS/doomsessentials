package org.lupz.doomsdayessentials.professions.menu;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.lupz.doomsdayessentials.EssentialsMod;
import org.lupz.doomsdayessentials.injury.InjuryItems;
import org.lupz.doomsdayessentials.professions.items.ProfessionItems;
import org.lupz.doomsdayessentials.professions.network.ProfessionNetwork;

@OnlyIn(Dist.CLIENT)
public class ProfissoesScreen extends AbstractContainerScreen<ProfissoesMenu> {

	private static final ResourceLocation CHEST_LOCATION = ResourceLocation.fromNamespaceAndPath("minecraft", "textures/gui/container/generic_54.png");
	private static final ResourceLocation BG_PRIMARY = ResourceLocation.fromNamespaceAndPath(EssentialsMod.MOD_ID, "textures/gui/professions_bg.png");
	private static final ResourceLocation BG_FALLBACK = ResourceLocation.fromNamespaceAndPath(EssentialsMod.MOD_ID, "textures/gui/closed_zone.png");
	
	public ProfissoesScreen(ProfissoesMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title);
		this.imageHeight = 3 * 18 + 17 + 7; // rows * slot + top+bottom
		this.inventoryLabelY = -10000; // keep player inventory label hidden
	}

	@Override
	protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
		// Dim and tile background
		guiGraphics.fillGradient(0, 0, this.width, this.height, 0xAA101010, 0xAA101010);
		var rm = this.minecraft.getResourceManager();
		ResourceLocation tex = rm.getResource(BG_PRIMARY).isPresent() ? BG_PRIMARY : BG_FALLBACK;
		RenderSystem.setShader(GameRenderer::getPositionTexShader);
		RenderSystem.setShaderColor(1f, 1f, 1f, 0.18f);
		int tile = 64;
		for (int yy = 0; yy < this.height; yy += tile) {
			for (int xx = 0; xx < this.width; xx += tile) {
				guiGraphics.blit(tex, xx, yy, 0, 0, tile, tile, tile, tile);
			}
		}
		RenderSystem.setShaderColor(1f, 1f, 1f, 1f);

		int x = (this.width - this.imageWidth) / 2;
		int y = (this.height - this.imageHeight) / 2;

		// Panel behind container
		int pad = 10;
		guiGraphics.fill(x - pad, y - pad, x + this.imageWidth + pad, y + this.imageHeight + pad, 0xB4000000);
		guiGraphics.fill(x - pad, y - pad, x + this.imageWidth + pad, y - pad + 1, 0x80FFFFFF);
		guiGraphics.fill(x - pad, y + this.imageHeight + pad - 1, x + this.imageWidth + pad, y + this.imageHeight + pad, 0x80111111);
		guiGraphics.fill(x - pad, y - pad, x - pad + 1, y + this.imageHeight + pad, 0x80FFFFFF);
		guiGraphics.fill(x + this.imageWidth + pad - 1, y - pad, x + this.imageWidth + pad, y + this.imageHeight + pad, 0x80111111);

		// Chest background (vanilla texture sliced)
		guiGraphics.blit(CHEST_LOCATION, x, y, 0, 0, this.imageWidth, 17);
		for (int row = 0; row < 3; ++row) {
			guiGraphics.blit(CHEST_LOCATION, x, y + 17 + row * 18, 0, 17, this.imageWidth, 18);
		}
		guiGraphics.blit(CHEST_LOCATION, x, y + 17 + 54, 0, 215, this.imageWidth, 7);

		// Title (centered, slightly larger look by bold color)
		guiGraphics.drawString(this.font, this.title, x + (this.imageWidth - this.font.width(this.title)) / 2, y + 6, 0xFFE6B866, false);
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		Slot slot = this.getSlotUnderMouse();
		if (slot != null && slot.hasItem()) {
			ItemStack stack = slot.getItem();
			if (stack.getItem() == InjuryItems.MEDIC_KIT.get()) {
				ProfessionNetwork.selectProfession("medico");
				this.minecraft.player.closeContainer();
				return true;
			}
			if (stack.is(Items.IRON_SWORD)) {
				ProfessionNetwork.selectProfession("combatente");
				this.minecraft.player.closeContainer();
				return true;
			}
			if (stack.is(Items.COMPASS)) {
				ProfessionNetwork.selectProfession("rastreador");
				this.minecraft.player.closeContainer();
				return true;
			}
			if (stack.getItem() == ProfessionItems.ENGINEER_HAMMER.get()) {
				ProfessionNetwork.selectProfession("engenheiro");
				this.minecraft.player.closeContainer();
				return true;
			}
			if (stack.is(Items.CROSSBOW)) {
				ProfessionNetwork.selectProfession("cacador");
				this.minecraft.player.closeContainer();
				return true;
			}
			if (stack.is(Items.BARRIER)) {
				ProfessionNetwork.abandonProfession();
				this.minecraft.player.closeContainer();
				return true;
			}
		}
		return super.mouseClicked(mouseX, mouseY, button);
	}

	@Override
	protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
		// keep vanilla tooltips only
	}

	@Override
	public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
		this.renderBackground(guiGraphics);
		super.render(guiGraphics, mouseX, mouseY, partialTick);
		if (this.hoveredSlot != null && this.hoveredSlot.hasItem()) {
			guiGraphics.renderTooltip(this.font, this.hoveredSlot.getItem(), mouseX, mouseY);
		}
	}
} 