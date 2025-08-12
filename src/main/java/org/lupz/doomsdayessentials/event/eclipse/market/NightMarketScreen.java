package org.lupz.doomsdayessentials.event.eclipse.market;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import org.lupz.doomsdayessentials.EssentialsMod;

public class NightMarketScreen extends AbstractContainerScreen<NightMarketMenu> {
    private static final ResourceLocation BG = ResourceLocation.fromNamespaceAndPath(EssentialsMod.MOD_ID, "textures/gui/night_market.png");

    public NightMarketScreen(NightMarketMenu menu, Inventory playerInv, Component title){
        super(menu, playerInv, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY){
        RenderSystem.setShaderColor(1,1,1,1);
        g.blit(BG, this.leftPos, this.topPos, 0,0, this.imageWidth, this.imageHeight);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float delta){
        renderBackground(g);
        super.render(g, mouseX, mouseY, delta);
        renderTooltip(g, mouseX, mouseY);
    }
} 