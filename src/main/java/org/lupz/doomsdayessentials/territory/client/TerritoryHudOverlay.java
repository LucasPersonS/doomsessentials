package org.lupz.doomsdayessentials.territory.client;

import net.minecraft.client.Minecraft;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.ChatFormatting;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.resources.ResourceLocation;
import org.lupz.doomsdayessentials.EssentialsMod;

public class TerritoryHudOverlay {
    public static final IGuiOverlay HUD = (gui, graphics, partial, width, height) -> {
        if (Minecraft.getInstance().player == null) return;
        if (!TerritoryClientState.running) return;

        int barWidth = 120;
        int barHeight = 8;
        int x = (width - barWidth) /2;
        int y = 10;

        float pct = TerritoryClientState.total==0?0:(float)TerritoryClientState.progress / TerritoryClientState.total;
        int filled = (int)(pct*barWidth);

        // background
        graphics.fill(x-1,y-1,x+barWidth+1,y+barHeight+1,0xAA000000);

        // Decide color: red if contested (guild null)
        int color = TerritoryClientState.guildName==null ? 0xFFFF0000 : 0xFF00FF00;

        // Use texture overlay
        RenderSystem.enableBlend();
        ResourceLocation tex = new ResourceLocation("minecraft","textures/gui/widgets.png");
        // Vanilla progress bar segment (u=0,v=64 width 182 height 5) – scale to barWidth
        graphics.blit(tex, x, y, 0, 64, barWidth, barHeight, 256, 256);
        graphics.fill(x, y, x+filled, y+barHeight, color);
        RenderSystem.disableBlend();

        // Text
        String txt = ChatFormatting.GOLD + "Capturando " + ChatFormatting.YELLOW + TerritoryClientState.areaName + " " + ChatFormatting.WHITE + TerritoryClientState.progress + "/" + TerritoryClientState.total + "s";
        graphics.drawString(Minecraft.getInstance().font, txt, x + barWidth/2 - Minecraft.getInstance().font.width(txt)/2, y - 10, 0xFFFFFF);
    };
} 