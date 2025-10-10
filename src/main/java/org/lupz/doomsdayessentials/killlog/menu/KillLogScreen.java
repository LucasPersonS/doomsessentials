package org.lupz.doomsdayessentials.killlog.menu;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class KillLogScreen extends AbstractContainerScreen<KillLogMenu> {

	private int selectedIndex = 0;
	private int dateScroll = 0; // pixels
	private int itemScroll = 0; // rows offset

	private Button takeAllBtn;

	// Dynamic columns
	private int datesW, itemsW, logsW;
	private int datesX, itemsX, logsX;

	public KillLogScreen(KillLogMenu menu, Inventory inv, Component title) {
		super(menu, inv, Component.literal("Killlog"));
		this.imageWidth = 520;
		this.imageHeight = 248;
	}

	@Override
	protected void init() {
		super.init();
		// Fit width to screen with margin
		this.imageWidth = Math.min(this.width - 40, 520);
		this.leftPos = (this.width - this.imageWidth) / 2;

		// Columns: Dates | Items | Logs
		this.datesW = 160;
		this.logsW = Math.max(160, Math.min(220, this.imageWidth - datesW - 220));
		this.itemsW = this.imageWidth - datesW - logsW - 16; // gaps = 4 + 4 + 8
		if (itemsW < 160) { itemsW = 160; logsW = this.imageWidth - datesW - itemsW - 16; }

		this.datesX = this.leftPos + 4;
		this.itemsX = this.leftPos + 8 + datesW; // 4 gap + dates panel
		this.logsX = this.leftPos + this.imageWidth - logsW - 4;

		this.takeAllBtn = Button.builder(Component.literal("Pegar Todos"), b -> onTakeAll())
			.bounds(itemsX + (itemsW - 140) / 2, this.topPos + this.imageHeight - 24, 140, 18)
			.build();
		addRenderableWidget(this.takeAllBtn);
	}

	private void onTakeAll() {
		String uuid = this.menu.getTargetUuid();
		int idx = Math.max(0, Math.min(selectedIndex, this.menu.getEntries().size()-1));
		org.lupz.doomsdayessentials.killlog.network.TakeKillLogItemsPacket.sendToServer(uuid, idx);
	}

	@Override
	protected void renderLabels(GuiGraphics gfx, int mouseX, int mouseY) {
		// Suppress default container labels (e.g., "Inventory") to avoid ghost text
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
		int left = this.leftPos;
		int top = this.topPos;
		if (mouseX >= datesX && mouseX <= datesX + datesW && mouseY >= top + 24 && mouseY <= top + this.imageHeight - 6) {
			dateScroll = (int)Math.max(0, dateScroll - delta * 12);
			return true;
		}
		if (mouseX >= itemsX && mouseX <= itemsX + itemsW && mouseY >= top + 24 && mouseY <= top + this.imageHeight - 32) {
			itemScroll = Math.max(0, itemScroll - (int)Math.signum(delta));
			return true;
		}
		return super.mouseScrolled(mouseX, mouseY, delta);
	}

	@Override
	public void render(GuiGraphics gfx, int mouseX, int mouseY, float partialTicks) {
		this.renderBackground(gfx);
		super.render(gfx, mouseX, mouseY, partialTicks);
		this.renderTooltip(gfx, mouseX, mouseY);

		int left = this.leftPos;
		int top = this.topPos;

		// Background + panels
		gfx.fill(left, top, left + this.imageWidth, top + this.imageHeight, 0xAA0A0A0A);
		// Dates panel
		gfx.fill(datesX, top + 24, datesX + datesW, top + this.imageHeight - 6, 0x66000000);
		// Items panel
		gfx.fill(itemsX, top + 24, itemsX + itemsW, top + this.imageHeight - 32, 0x66000000);
		// Logs panel
		gfx.fill(logsX, top + 24, logsX + logsW, top + this.imageHeight - 6, 0x66000000);
		// Items footer
		gfx.fill(itemsX, top + this.imageHeight - 28, itemsX + itemsW, top + this.imageHeight - 6, 0x66000000);

		gfx.drawString(this.font, Component.literal("Datas"), datesX, top + 12, 0xFFFFFF, false);
		gfx.drawString(this.font, Component.literal("Itens"), itemsX, top + 12, 0xFFFFFF, false);
		gfx.drawString(this.font, Component.literal("Logs"), logsX, top + 12, 0xFFFFFF, false);

		// Dates list
		gfx.enableScissor(datesX, top + 24, datesX + datesW, top + this.imageHeight - 6);
		int y0 = top + 28 - dateScroll;
		for (int i = 0; i < this.menu.getEntries().size(); i++) {
			KillLogMenu.Entry e = this.menu.getEntries().get(i);
			int y = y0 + i * 12;
			int color = (i == selectedIndex) ? 0xFFFFE066 : 0xFFCCCCCC;
			gfx.drawString(this.font, Component.literal((i == selectedIndex ? "> " : "  ") + e.timestampIso), datesX + 4, y, color, false);
		}
		gfx.disableScissor();

		// Items grid + hover preview name
		ItemStack hovered = ItemStack.EMPTY;
		gfx.enableScissor(itemsX, top + 24, itemsX + itemsW, top + this.imageHeight - 32);
		if (!this.menu.getEntries().isEmpty()) {
			KillLogMenu.Entry sel = this.menu.getEntries().get(Math.max(0, Math.min(selectedIndex, this.menu.getEntries().size()-1)));
			int startX = itemsX + 4;
			int startY = top + 28;
			int cols = Math.max(5, (itemsW - 8) / 18);
			int rowOffset = itemScroll;
			for (int i = rowOffset * cols; i < sel.items.size(); i++) {
				int idx = i - rowOffset * cols;
				int cx = idx % cols;
				int cy = idx / cols;
				int x = startX + cx * 18;
				int z = startY + cy * 18;
				ItemStack st = sel.items.get(i);
				gfx.renderItem(st, x, z);
				// Render item count
				if (st.getCount() > 1) {
					String countStr = String.valueOf(st.getCount());
					int countWidth = this.font.width(countStr);
					gfx.drawString(this.font, countStr, x + 17 - countWidth, z + 9, 0xFFFFFF, true);
				}
				if (mouseX >= x && mouseX <= x + 16 && mouseY >= z && mouseY <= z + 16) hovered = st;
			}
		}
		gfx.disableScissor();
		if (!hovered.isEmpty()) {
			gfx.drawString(this.font, hovered.getHoverName(), itemsX + 6, top + this.imageHeight - 38, 0xFFE0E0E0, false);
			// Scaled preview center of footer
			int px = itemsX + itemsW / 2 - 8;
			int py = top + this.imageHeight - 26;
			gfx.renderItem(hovered, px, py);
		}

		// Logs panel
		gfx.enableScissor(logsX, top + 24, logsX + logsW, top + this.imageHeight - 6);
		if (!this.menu.getEntries().isEmpty()) {
			KillLogMenu.Entry sel = this.menu.getEntries().get(Math.max(0, Math.min(selectedIndex, this.menu.getEntries().size()-1)));
			int lx = logsX + 4;
			int ly = top + 28;
			gfx.drawString(this.font, Component.literal(String.format("Zona: %s (%s)", safe(sel.areaType), safe(sel.areaName))), lx, ly, 0xFFFFFF, false);
			ly += 12;
			gfx.drawString(this.font, Component.literal(String.format("Coordenadas: %.1f, %.1f, %.1f", sel.x, sel.y, sel.z)), lx, ly, 0xFFFFFF, false);
			ly += 12;
			gfx.drawString(this.font, Component.literal("Causa: " + safe(sel.causeId)), lx, ly, 0xFFFFFF, false);
			ly += 12;
			gfx.drawString(this.font, Component.literal("Assassino: " + safe(sel.killerName)), lx, ly, 0xFFFFFF, false);
			ly += 12;
			gfx.drawString(this.font, Component.literal("Arma: " + (sel.weaponDisplay != null ? sel.weaponDisplay : "Null")), lx, ly, 0xFFFFFF, false);
			ly += 14;
			gfx.drawString(this.font, Component.literal("Últimos danos:"), lx, ly, 0xFFFFD27F, false);
			ly += 12;
			for (String s : sel.lastHits) {
				gfx.drawString(this.font, Component.literal("- " + s), lx, ly, 0xFFECECEC, false);
				ly += 10;
				if (ly > top + this.imageHeight - 10) break;
			}
		}
		gfx.disableScissor();
	}

	private static String safe(String s){ return s != null ? s : ""; }

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		int top = this.topPos;
		if (mouseX >= datesX && mouseX <= datesX + datesW && mouseY >= top + 24 && mouseY <= top + this.imageHeight - 6) {
			int localY = (int)mouseY - (top + 28) + dateScroll;
			int i = localY / 12;
			if (i >= 0 && i < this.menu.getEntries().size()) {
				this.selectedIndex = i;
				this.itemScroll = 0;
				return true;
			}
		}
		return super.mouseClicked(mouseX, mouseY, button);
	}

	@Override
	protected void renderBg(GuiGraphics gfx, float partialTicks, int mouseX, int mouseY) {
	}
} 