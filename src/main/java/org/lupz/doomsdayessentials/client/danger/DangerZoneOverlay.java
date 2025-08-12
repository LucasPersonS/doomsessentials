package org.lupz.doomsdayessentials.client.danger;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;
import org.lupz.doomsdayessentials.EssentialsMod;

public class DangerZoneOverlay {
    private static final ResourceLocation TEX = new ResourceLocation(EssentialsMod.MOD_ID, "textures/gui/closed_zone.png");

    public static final IGuiOverlay OVERLAY = (gui, gfx, partial, width, height) -> {
        if(!DangerZoneClientState.isActive()) return;
        RenderSystem.enableBlend();
        int imgW = 128, imgH = 64;
        int x = (width - imgW) /2;
        int y = height/3;
        gfx.blit(TEX, x, y, 0,0,imgW,imgH,imgW,imgH);
        // Build component with green bold style and custom KnightVision font
        String timeTxt = DangerZoneClientState.getTimeText().replace(":","h");
        net.minecraft.network.chat.Component txtComp = net.minecraft.network.chat.Component.literal("Abre as "+timeTxt)
                .withStyle(style -> style.withBold(true)
                        .withColor(net.minecraft.network.chat.TextColor.fromRgb(0x006400))
                        .withFont(new ResourceLocation(org.lupz.doomsdayessentials.EssentialsMod.MOD_ID, "pixelgame")));

        var font = Minecraft.getInstance().font;
        int txtW = font.width(txtComp);
        gfx.drawString(font, txtComp, width/2 - txtW/2, y+imgH+5, 0x00FF00);
        RenderSystem.disableBlend();
    };
} 