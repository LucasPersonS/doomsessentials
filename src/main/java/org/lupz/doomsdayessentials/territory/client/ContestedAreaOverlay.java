package org.lupz.doomsdayessentials.territory.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;
import org.lupz.doomsdayessentials.EssentialsMod;

/**
 * Displays a small banner on screen whenever a territory event is running but contested (no guild capturing).
 */
public class ContestedAreaOverlay {
    private static final ResourceLocation TEXTURE = new ResourceLocation(EssentialsMod.MOD_ID, "textures/misc/area_contested.png");

    public static final IGuiOverlay OVERLAY = (gui, graphics, partial, width, height) -> {
        if (!TerritoryClientState.running) return;
        if (TerritoryClientState.guildName != null) return; // someone is capturing
        drawCentered(graphics, width, height);
    };

    private static void drawCentered(GuiGraphics graphics, int width, int height) {
        int texW = 64;
        int texH = 64;
        int x = width / 2 - texW / 2;
        int y = 30; // below the HUD bar
        RenderSystem.enableBlend();
        graphics.blit(TEXTURE, x, y, 0, 0, texW, texH, texW, texH);
        RenderSystem.disableBlend();
    }
} 