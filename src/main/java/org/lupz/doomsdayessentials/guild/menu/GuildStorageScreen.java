package org.lupz.doomsdayessentials.guild.menu;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.lupz.doomsdayessentials.network.PacketHandler;
import org.lupz.doomsdayessentials.EssentialsMod;

public class GuildStorageScreen extends AbstractContainerScreen<GuildStorageMenu> {
    private static final ResourceLocation CHEST_BG = ResourceLocation.fromNamespaceAndPath("minecraft", "textures/gui/container/generic_54.png");
    private static final ResourceLocation BACK_ICON = ResourceLocation.fromNamespaceAndPath(EssentialsMod.MOD_ID, "textures/item/gui/back.png");
    private static final ResourceLocation NEXT_ICON = ResourceLocation.fromNamespaceAndPath(EssentialsMod.MOD_ID, "textures/item/gui/next.png");

    // External button metrics
    private int btnSize = 18;
    private int btnGap = 4;
    private int btnY;
    private int prevX, nextX, filterX, sortX, upgX, logsX;

    public GuildStorageScreen(GuildStorageMenu menu, Inventory inv, Component title) {
        super(menu, inv, Component.literal("§6Cofre da Organização"));
        this.imageWidth = 176;
        this.imageHeight = 222;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partial, int mouseX, int mouseY) {
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;
        RenderSystem.setShaderTexture(0, CHEST_BG);
        RenderSystem.enableBlend();
        graphics.blit(CHEST_BG, x, y, 0, 0, imageWidth, 125, 256, 256);
        graphics.blit(CHEST_BG, x, y + 125, 0, 126, imageWidth, 96, 256, 256);
        RenderSystem.disableBlend();
        graphics.fill(x, y, x + imageWidth, y + 14, 0xFF1e1e1e);
        graphics.fill(x+1, y+1, x + imageWidth-1, y + 13, 0xFF3a3a3a);
        graphics.drawCenteredString(font, title, x + imageWidth / 2, y + 4, 0xE0FFAA);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) { }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partial) {
        super.render(graphics, mouseX, mouseY, partial);

        // Draw external buttons row above the container
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;
        btnY = y - (btnSize + 6);
        int startX = x;
        prevX = startX;
        nextX = prevX + btnSize + btnGap;
        filterX = nextX + btnSize + btnGap;
        sortX = filterX + btnSize + btnGap;
        upgX = sortX + btnSize + btnGap;
        logsX = upgX + btnSize + btnGap;

        // Build tooltip stacks using the actual items in the hidden control slots when available
        ItemStack tipBack = getSlotTooltipOrFallback(45,
                makeTooltipStack(Items.PAPER, Component.literal("§eVoltar"), java.util.List.of(Component.literal("§7Voltar ao menu ou página anterior"))));
        ItemStack tipNext = getSlotTooltipOrFallback(53,
                makeTooltipStack(Items.PAPER, Component.literal("§ePróxima"), java.util.List.of(Component.literal("§7Ir para a próxima página"))));
        ItemStack tipFilter = getSlotTooltipOrFallback(46,
                makeTooltipStack(Items.HOPPER, Component.literal("§bFiltro"), java.util.List.of(Component.literal("§7Clique para alternar entre: Tudo/Blocos/Itens/Ferramentas/Armas/Armaduras/Comida/Poções/Encantados"))));
        ItemStack tipSort = getSlotTooltipOrFallback(47,
                makeTooltipStack(Items.COMPARATOR, Component.literal("§bOrganizar Cofre"), java.util.List.of(Component.literal("§7Organiza por tipo/ID/NBT (blocos, ferramentas, etc.)"))));
        ItemStack tipUpg = getSlotTooltipOrFallback(50,
                makeTooltipStack(Items.WRITABLE_BOOK, Component.literal("§aAprimorar"), java.util.List.of(Component.literal("§7Clique para Aprimorar"))));
        ItemStack tipLogs = getSlotTooltipOrFallback(52,
                makeTooltipStack(Items.PAPER, Component.literal("§eHistórico do Cofre"), java.util.List.of(Component.literal("§7Ver registros de alterações do cofre"))));

        drawButtonTexture(graphics, prevX, btnY, mouseX, mouseY, BACK_ICON, tipBack);
        drawButtonTexture(graphics, nextX, btnY, mouseX, mouseY, NEXT_ICON, tipNext);
        drawButtonItem(graphics, filterX, btnY, mouseX, mouseY, new ItemStack(Items.HOPPER), tipFilter);
        drawButtonItem(graphics, sortX, btnY, mouseX, mouseY, new ItemStack(Items.COMPARATOR), tipSort);
        drawButtonItem(graphics, upgX, btnY, mouseX, mouseY, new ItemStack(Items.WRITABLE_BOOK), tipUpg);
        drawButtonItem(graphics, logsX, btnY, mouseX, mouseY, new ItemStack(Items.PAPER), tipLogs);

        // Render vanilla tooltips for items/slots (external buttons draw their own tooltips)
        renderTooltip(graphics, mouseX, mouseY);
    }

    private String formatK(int count) {
        // kept for potential future use, current counts are drawn by mixin
        double k = count / 1000.0;
        return String.format(java.util.Locale.ROOT, "%.1fk", k);
    }

    private void drawFrame(GuiGraphics g, int bx, int by, boolean hovered) {
        int bg = 0xB0000000;
        g.fill(bx - 1, by - 1, bx + btnSize + 1, by + btnSize + 1, 0x40111111);
        g.fill(bx, by, bx + btnSize, by + btnSize, bg);
        if (hovered) {
            // Darken on hover
            g.fill(bx, by, bx + btnSize, by + btnSize, 0x60000000);
        }
    }

    private void drawButtonItem(GuiGraphics g, int bx, int by, int mouseX, int mouseY, ItemStack icon, ItemStack tooltipStack) {
        boolean hovered = isInside(mouseX, mouseY, bx, by, btnSize, btnSize);
        drawFrame(g, bx, by, hovered);
        g.renderItem(icon, bx + 1, by + 1);
        if (hovered) {
            // Brighten the item/icon on hover
            g.fill(bx + 1, by + 1, bx + 17, by + 17, 0x66FFFFFF);
            g.renderTooltip(this.font, tooltipStack, mouseX, mouseY);
        }
    }

    private void drawButtonTexture(GuiGraphics g, int bx, int by, int mouseX, int mouseY, ResourceLocation icon, ItemStack tooltipStack) {
        boolean hovered = isInside(mouseX, mouseY, bx, by, btnSize, btnSize);
        drawFrame(g, bx, by, hovered);
        // Draw 16x16 texture inside the frame
        g.blit(icon, bx + 1, by + 1, 0, 0, 16, 16, 16, 16);
        if (hovered) {
            // Brighten the texture on hover
            g.fill(bx + 1, by + 1, bx + 17, by + 17, 0x66FFFFFF);
            g.renderTooltip(this.font, tooltipStack, mouseX, mouseY);
        }
    }

    private ItemStack makeTooltipStack(net.minecraft.world.item.Item icon, Component name, java.util.List<Component> lore) {
        ItemStack s = new ItemStack(icon);
        s.setHoverName(name);
        if (lore != null && !lore.isEmpty()) {
            net.minecraft.nbt.ListTag tag = new net.minecraft.nbt.ListTag();
            for (Component c : lore) tag.add(net.minecraft.nbt.StringTag.valueOf(Component.Serializer.toJson(c)));
            s.getOrCreateTagElement("display").put("Lore", tag);
        }
        return s;
    }

    private ItemStack getSlotTooltipOrFallback(int slotId, ItemStack fallback) {
        try {
            net.minecraft.world.inventory.Slot s = this.menu.getSlot(slotId);
            if (s != null && s.hasItem()) return s.getItem().copy();
        } catch (Throwable ignored) { }
        return fallback;
    }

    private boolean isInside(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // Swallow clicks on in-grid control slot rectangles so they do nothing
        if (isOverControlSlotRect((int)mouseX, (int)mouseY)) return true;
        if (isInside((int)mouseX, (int)mouseY, prevX, btnY, btnSize, btnSize)) {
            PacketHandler.CHANNEL.sendToServer(new GuildStorageActionPacket(GuildStorageActionPacket.Action.PREV_PAGE));
            return true;
        }
        if (isInside((int)mouseX, (int)mouseY, nextX, btnY, btnSize, btnSize)) {
            PacketHandler.CHANNEL.sendToServer(new GuildStorageActionPacket(GuildStorageActionPacket.Action.NEXT_PAGE));
            return true;
        }
        if (isInside((int)mouseX, (int)mouseY, filterX, btnY, btnSize, btnSize)) {
            PacketHandler.CHANNEL.sendToServer(new GuildStorageActionPacket(GuildStorageActionPacket.Action.FILTER_CYCLE));
            return true;
        }
        if (isInside((int)mouseX, (int)mouseY, sortX, btnY, btnSize, btnSize)) {
            PacketHandler.CHANNEL.sendToServer(new GuildStorageActionPacket(GuildStorageActionPacket.Action.SORT));
            return true;
        }
        if (isInside((int)mouseX, (int)mouseY, upgX, btnY, btnSize, btnSize)) {
            PacketHandler.CHANNEL.sendToServer(new GuildStorageActionPacket(GuildStorageActionPacket.Action.UPGRADE));
            return true;
        }
        if (isInside((int)mouseX, (int)mouseY, logsX, btnY, btnSize, btnSize)) {
            PacketHandler.CHANNEL.sendToServer(new GuildStorageActionPacket(GuildStorageActionPacket.Action.OPEN_LOGS));
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private boolean isOverControlSlotRect(int mouseX, int mouseY) {
        int baseX = (width - imageWidth) / 2;
        int baseY = (height - imageHeight) / 2;
        int[][] controls = new int[][]{{0,5},{8,5},{1,5},{2,5},{7,5},{5,5}};
        for (int[] rc : controls) {
            int cx = baseX + 8 + rc[0] * 18;
            int cy = baseY + 18 + rc[1] * 18;
            if (isInside(mouseX, mouseY, cx, cy, 16, 16)) return true;
        }
        return false;
    }
}


