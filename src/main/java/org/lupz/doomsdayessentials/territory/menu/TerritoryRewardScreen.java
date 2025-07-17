package org.lupz.doomsdayessentials.territory.menu;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import org.lupz.doomsdayessentials.EssentialsMod;
import com.mojang.blaze3d.systems.RenderSystem;

public class TerritoryRewardScreen extends AbstractContainerScreen<TerritoryRewardMenu> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(EssentialsMod.MOD_ID, "textures/gui/territory_rewards.png");
    private static final int TEXTURE_WIDTH = 176;
    private static final int TEXTURE_HEIGHT = 222;
    private static final ResourceLocation CHEST_BG = ResourceLocation.fromNamespaceAndPath("minecraft", "textures/gui/container/generic_54.png");

    public TerritoryRewardScreen(TerritoryRewardMenu menu, Inventory inv, Component title) {
        super(menu, inv, Component.literal("§6Recompensas de Território"));
        this.imageWidth = TEXTURE_WIDTH;
        this.imageHeight = TEXTURE_HEIGHT;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partial, int mouseX, int mouseY) {
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;

        RenderSystem.setShaderTexture(0, CHEST_BG);
        RenderSystem.enableBlend();
        // Top (6 rows) height 6*18 + 17 = 125? Actually vanilla slices: top 0..6*18+17 (this render uses 0,0 -> 176x125)
        graphics.blit(CHEST_BG, x, y, 0, 0, imageWidth, 125, 256, 256);
        // Player inventory area
        graphics.blit(CHEST_BG, x, y + 125, 0, 126, imageWidth, 96, 256, 256);
        RenderSystem.disableBlend();

        // Header bar: dark outline + inner lighter
        graphics.fill(x, y, x + imageWidth, y + 14, 0xFF1e1e1e); // dark
        graphics.fill(x+1, y+1, x + imageWidth-1, y + 13, 0xFF3a3a3a); // lighter inner
        graphics.drawCenteredString(font, title, x + imageWidth / 2, y + 4, 0xE0FFAA);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partial) {
        super.render(graphics, mouseX, mouseY, partial);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        // handled in renderBg
    }
} 