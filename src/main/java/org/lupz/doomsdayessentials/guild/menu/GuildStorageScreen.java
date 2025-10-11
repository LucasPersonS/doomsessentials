package org.lupz.doomsdayessentials.guild.menu;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.lupz.doomsdayessentials.network.PacketHandler;
import org.lupz.doomsdayessentials.EssentialsMod;
import org.lwjgl.glfw.GLFW;

public class GuildStorageScreen extends AbstractContainerScreen<GuildStorageMenu> {
    private static final ResourceLocation CHEST_BG = ResourceLocation.fromNamespaceAndPath("minecraft", "textures/gui/container/generic_54.png");
    private static final ResourceLocation BACK_ICON = ResourceLocation.fromNamespaceAndPath(EssentialsMod.MOD_ID, "textures/item/gui/back.png");
    private static final ResourceLocation NEXT_ICON = ResourceLocation.fromNamespaceAndPath(EssentialsMod.MOD_ID, "textures/item/gui/next.png");

    // UI Components
    private EditBox searchBox;
    private FilterButton filterButton;
    private int btnSize = 20;
    private int btnGap = 2;
    private int currentPage = 0;
    private int maxPages = 1;
    
    // Button positions
    private int prevX, nextX, filterX, sortX, upgX, logsX;
    private int btnY;
    
    // Current filter
    private GuildStorageMenu.Filter currentFilter = GuildStorageMenu.Filter.ALL;

    public GuildStorageScreen(GuildStorageMenu menu, Inventory inv, Component title) {
        super(menu, inv, Component.literal("§6Cofre da Organização"));
        this.imageWidth = 176;
        this.imageHeight = 222;
        this.inventoryLabelY = this.imageHeight - 94;
    }
    
    @Override
    protected void init() {
        super.init();
        
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;
        
        // Add search box at the top
        this.searchBox = new EditBox(this.font, x + 8, y - 25, 90, 16, Component.literal("Buscar"));
        this.searchBox.setMaxLength(50);
        this.searchBox.setBordered(true);
        this.searchBox.setVisible(true);
        this.searchBox.setTextColor(0xFFFFFF);
        this.searchBox.setHint(Component.literal("§7Buscar item..."));
        this.searchBox.setResponder(this::onSearchChanged);
        this.addRenderableWidget(this.searchBox);
        
        // Add filter button next to search
        this.filterButton = new FilterButton(x + 102, y - 26, 55, 18, currentFilter);
        this.addRenderableWidget(this.filterButton);
        
        // Calculate button positions - Back first, Next last
        btnY = y + imageHeight + 5;
        int totalWidth = (6 * btnSize) + (5 * btnGap);
        int startX = x + (imageWidth - totalWidth) / 2;
        
        // Rearranged order: Back, Filter, Sort, Upgrade, Logs, Next
        prevX = startX;  // Back button
        filterX = prevX + btnSize + btnGap;
        sortX = filterX + btnSize + btnGap;
        upgX = sortX + btnSize + btnGap;
        logsX = upgX + btnSize + btnGap;
        nextX = logsX + btnSize + btnGap;  // Next button at the end
    }
    
    private void onSearchChanged(String text) {
        // Send search to server
        PacketHandler.CHANNEL.sendToServer(new GuildStorageActionPacket(GuildStorageActionPacket.Action.SEARCH, text));
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partial, int mouseX, int mouseY) {
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;
        
        // Render main chest background
        RenderSystem.setShaderTexture(0, CHEST_BG);
        RenderSystem.enableBlend();
        graphics.blit(CHEST_BG, x, y, 0, 0, imageWidth, 125, 256, 256);
        graphics.blit(CHEST_BG, x, y + 125, 0, 126, imageWidth, 96, 256, 256);
        RenderSystem.disableBlend();
        
        // Custom title bar
        graphics.fill(x, y, x + imageWidth, y + 14, 0xFF1e1e1e);
        graphics.fill(x+1, y+1, x + imageWidth-1, y + 13, 0xFF3a3a3a);
        
        // Draw title with page info
        String titleText = title.getString();
        if (menu.getMaxPages() > 1) {
            titleText += String.format(" §7(Pág %d/%d)", menu.getCurrentPage() + 1, menu.getMaxPages());
        }
        graphics.drawCenteredString(font, titleText, x + imageWidth / 2, y + 4, 0xE0FFAA);
        
        // Search background
        graphics.fill(x + 6, y - 27, x + 100, y - 9, 0xFF000000);
        graphics.fill(x + 7, y - 26, x + 99, y - 10, 0xFF3C3C3C);
        
        // Storage capacity info (items, not slots)
        int usedItems = menu.getUsedItems();
        int maxCapacity = menu.getMaxCapacity();
        String storageInfo = String.format("§7%d/%d itens", usedItems, maxCapacity);
        graphics.drawString(font, storageInfo, x + 162, y - 24, 0xFFFFFF);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        // Don't render default labels
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partial) {
        this.renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partial);
        
        // Draw bottom button bar
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;

        // Button tooltips
        boolean isFirstPage = menu.getCurrentPage() == 0;
        ItemStack tipPrev = makeTooltipStack(Items.ARROW, 
                Component.literal(isFirstPage ? "§eVoltar ao Menu" : "§ePágina Anterior"), 
                java.util.List.of(Component.literal(isFirstPage ? "§7Voltar ao menu principal" : "§7Navegar para página anterior")));
        ItemStack tipNext = makeTooltipStack(Items.ARROW, Component.literal("§ePróxima Página"), 
                java.util.List.of(Component.literal("§7Navegar para próxima página")));
        ItemStack tipFilter = makeTooltipStack(Items.HOPPER, Component.literal("§bFiltro"), 
                java.util.List.of(Component.literal("§7Filtro atual: " + currentFilter.name())));
        ItemStack tipSort = makeTooltipStack(Items.COMPARATOR, Component.literal("§bOrganizar"), 
                java.util.List.of(Component.literal("§7Organiza itens por categoria")));
        ItemStack tipUpg = makeTooltipStack(Items.ENCHANTED_BOOK, Component.literal("§aAprimorar (Nível " + menu.getStorageLevel() + ")"), 
                java.util.List.of(Component.literal("§7Capacidade atual: " + menu.getMaxCapacity() + " itens"),
                                 Component.literal("§7Clique para aprimorar")));
        ItemStack tipLogs = makeTooltipStack(Items.BOOK, Component.literal("§eHistórico"), 
                java.util.List.of(Component.literal("§7Ver logs do cofre")));
        
        // Render buttons in new order
        boolean canNext = menu.getCurrentPage() < menu.getMaxPages() - 1;
        
        drawButtonTexture(graphics, prevX, btnY, mouseX, mouseY, BACK_ICON, tipPrev, true);  // Always enabled (back or previous)
        drawButtonItem(graphics, filterX, btnY, mouseX, mouseY, new ItemStack(Items.HOPPER), tipFilter, true);
        drawButtonItem(graphics, sortX, btnY, mouseX, mouseY, new ItemStack(Items.COMPARATOR), tipSort, true);
        drawButtonItem(graphics, upgX, btnY, mouseX, mouseY, new ItemStack(Items.ENCHANTED_BOOK), tipUpg, true);
        drawButtonItem(graphics, logsX, btnY, mouseX, mouseY, new ItemStack(Items.BOOK), tipLogs, true);
        drawButtonTexture(graphics, nextX, btnY, mouseX, mouseY, NEXT_ICON, tipNext, canNext);

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

    private void drawButtonItem(GuiGraphics g, int bx, int by, int mouseX, int mouseY, ItemStack icon, ItemStack tooltipStack, boolean enabled) {
        boolean hovered = enabled && isInside(mouseX, mouseY, bx, by, btnSize, btnSize);
        drawFrame(g, bx, by, hovered, enabled);
        g.renderItem(icon, bx + 2, by + 2);
        if (!enabled) {
            g.fill(bx + 2, by + 2, bx + 18, by + 18, 0x88000000);
        } else if (hovered) {
            g.fill(bx + 2, by + 2, bx + 18, by + 18, 0x66FFFFFF);
            g.renderTooltip(this.font, tooltipStack, mouseX, mouseY);
        }
    }

    private void drawButtonTexture(GuiGraphics g, int bx, int by, int mouseX, int mouseY, ResourceLocation icon, ItemStack tooltipStack, boolean enabled) {
        boolean hovered = enabled && isInside(mouseX, mouseY, bx, by, btnSize, btnSize);
        drawFrame(g, bx, by, hovered, enabled);
        g.blit(icon, bx + 2, by + 2, 0, 0, 16, 16, 16, 16);
        if (!enabled) {
            g.fill(bx + 2, by + 2, bx + 18, by + 18, 0x88000000);
        } else if (hovered) {
            g.fill(bx + 2, by + 2, bx + 18, by + 18, 0x66FFFFFF);
            g.renderTooltip(this.font, tooltipStack, mouseX, mouseY);
        }
    }
    
    private void drawFrame(GuiGraphics g, int bx, int by, boolean hovered, boolean enabled) {
        int borderColor = enabled ? (hovered ? 0xFFFFFF88 : 0xFF666666) : 0xFF333333;
        int bgColor = enabled ? (hovered ? 0xFF505050 : 0xFF383838) : 0xFF1A1A1A;
        
        // Border
        g.fill(bx, by, bx + btnSize, by + btnSize, borderColor);
        // Background 
        g.fill(bx + 1, by + 1, bx + btnSize - 1, by + btnSize - 1, bgColor);
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
        // Handle button clicks - new order
        if (isInside((int)mouseX, (int)mouseY, prevX, btnY, btnSize, btnSize)) {
            PacketHandler.CHANNEL.sendToServer(new GuildStorageActionPacket(GuildStorageActionPacket.Action.PREV_PAGE));
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
        if (menu.getCurrentPage() < menu.getMaxPages() - 1 && isInside((int)mouseX, (int)mouseY, nextX, btnY, btnSize, btnSize)) {
            PacketHandler.CHANNEL.sendToServer(new GuildStorageActionPacket(GuildStorageActionPacket.Action.NEXT_PAGE));
            return true;
        }
        
        return super.mouseClicked(mouseX, mouseY, button);
    }
    
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.searchBox != null && this.searchBox.isFocused()) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                this.searchBox.setFocused(false);
                return true;
            }
            return this.searchBox.keyPressed(keyCode, scanCode, modifiers);
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    // Custom filter button class
    private class FilterButton extends AbstractButton {
        private GuildStorageMenu.Filter filter;
        
        public FilterButton(int x, int y, int width, int height, GuildStorageMenu.Filter initial) {
            super(x, y, width, height, Component.literal(""));
            this.filter = initial;
            this.setMessage(Component.literal(getFilterName(initial)));
        }
        
        @Override
        public void onPress() {
            // Cycle filter
            this.filter = switch(filter) {
                case ALL -> GuildStorageMenu.Filter.BLOCKS;
                case BLOCKS -> GuildStorageMenu.Filter.ITEMS;
                case ITEMS -> GuildStorageMenu.Filter.TOOLS;
                case TOOLS -> GuildStorageMenu.Filter.WEAPONS;
                case WEAPONS -> GuildStorageMenu.Filter.ARMOR;
                case ARMOR -> GuildStorageMenu.Filter.FOOD;
                case FOOD -> GuildStorageMenu.Filter.POTIONS;
                case POTIONS -> GuildStorageMenu.Filter.ENCHANTED;
                case ENCHANTED -> GuildStorageMenu.Filter.ALL;
            };
            
            this.setMessage(Component.literal(getFilterName(filter)));
            currentFilter = filter;
            
            // Send to server
            PacketHandler.CHANNEL.sendToServer(new GuildStorageActionPacket(GuildStorageActionPacket.Action.FILTER_CYCLE));
        }
        
        @Override
        protected void updateWidgetNarration(NarrationElementOutput output) {
            this.defaultButtonNarrationText(output);
        }
        
        @Override
        public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            // Custom render
            boolean hovered = this.isHovered();
            int color = hovered ? 0xFF6688FF : 0xFF4466DD;
            graphics.fill(this.getX(), this.getY(), this.getX() + this.width, this.getY() + this.height, color);
            graphics.fill(this.getX() + 1, this.getY() + 1, this.getX() + this.width - 1, this.getY() + this.height - 1, 0xFF2A2A2A);
            
            // Draw text
            graphics.drawCenteredString(font, this.getMessage(), this.getX() + this.width / 2, this.getY() + (this.height - 8) / 2, 0xFFFFFF);
        }
        
        private String getFilterName(GuildStorageMenu.Filter f) {
            return switch(f) {
                case ALL -> "§fTodos";
                case BLOCKS -> "§6Blocos";
                case ITEMS -> "§eItens";
                case TOOLS -> "§bFerramentas";
                case WEAPONS -> "§cArmas";
                case ARMOR -> "§9Armaduras";
                case FOOD -> "§aComida";
                case POTIONS -> "§dPoções";
                case ENCHANTED -> "§5Encantados";
            };
        }
    }
}


