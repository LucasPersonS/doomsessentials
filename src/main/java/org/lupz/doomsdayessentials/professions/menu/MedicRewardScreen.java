package org.lupz.doomsdayessentials.professions.menu;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import org.lupz.doomsdayessentials.EssentialsMod;

public class MedicRewardScreen extends AbstractContainerScreen<MedicRewardMenu> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(EssentialsMod.MOD_ID, "textures/gui/medic_rewards.png");
    private static final int TEXTURE_WIDTH = 176;
    private static final int TEXTURE_HEIGHT = 166;

    public MedicRewardScreen(MedicRewardMenu menu, Inventory playerInv, Component title) {
        super(menu, playerInv, Component.literal("§6Recompensas de Médico"));
        this.imageWidth = TEXTURE_WIDTH;
        this.imageHeight = TEXTURE_HEIGHT;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;
        
        // Render background
        graphics.fill(x, y, x + imageWidth, y + imageHeight, 0xFF1a1a1a);
        
        // Render title
        graphics.drawCenteredString(font, title, x + imageWidth / 2, y + 6, 0xFFFFFF);
        
        // Render subtitle with line break
        String subtitle = "§7Itens que podem ser recebidos como recompensa";
        String[] lines = subtitle.split(" ");
        int lineY = y + 18;
        
        StringBuilder currentLine = new StringBuilder();
        for (String word : lines) {
            if (currentLine.length() + word.length() > 20) {
                // Draw current line and start new one
                graphics.drawCenteredString(font, Component.literal(currentLine.toString()), 
                                         x + imageWidth / 2, lineY, 0xCCCCCC);
                lineY += 10;
                currentLine = new StringBuilder(word);
            } else {
                if (currentLine.length() > 0) currentLine.append(" ");
                currentLine.append(word);
            }
        }
        // Draw the last line
        if (currentLine.length() > 0) {
            graphics.drawCenteredString(font, Component.literal(currentLine.toString()), 
                                     x + imageWidth / 2, lineY, 0xCCCCCC);
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        // Don't render labels as we're handling them in renderBg
    }
} 