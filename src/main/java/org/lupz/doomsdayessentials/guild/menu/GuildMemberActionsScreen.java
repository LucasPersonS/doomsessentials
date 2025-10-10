package org.lupz.doomsdayessentials.guild.menu;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import com.mojang.blaze3d.systems.RenderSystem;

public class GuildMemberActionsScreen extends AbstractContainerScreen<GuildMemberActionsMenu> {
    private static final ResourceLocation CHEST_BG = ResourceLocation.fromNamespaceAndPath("minecraft", "textures/gui/container/generic_54.png");

    public GuildMemberActionsScreen(GuildMemberActionsMenu menu, Inventory inv, Component title) {
        super(menu, inv, Component.literal("§6Ações do Membro"));
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
        renderTooltip(graphics, mouseX, mouseY);
    }
}


