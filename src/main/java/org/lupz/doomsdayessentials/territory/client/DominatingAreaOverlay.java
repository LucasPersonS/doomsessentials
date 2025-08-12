package org.lupz.doomsdayessentials.territory.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;
import org.lupz.doomsdayessentials.EssentialsMod;

public class DominatingAreaOverlay {
    private static final ResourceLocation TEX = new ResourceLocation(EssentialsMod.MOD_ID, "textures/misc/area_dominating.png");

    public static final IGuiOverlay OVERLAY = (gui, graphics, partial, width, height) -> {
        if (!TerritoryClientState.running) return;
        if (TerritoryClientState.guildName == null) return; // only when capturing
        draw(graphics, width, height);
    };

    private static void draw(GuiGraphics g, int width, int height) {
        int w = 64, h = 64;
        int x = width/2 - w/2;
        int y = 30;
        RenderSystem.enableBlend();
        g.blit(TEX, x, y, 0,0,w,h,w,h);
        RenderSystem.disableBlend();
    }
} 