package org.lupz.doomsdayessentials.client.frequency;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import org.lupz.doomsdayessentials.frequency.capability.FrequencyCapabilityProvider;

public final class FrequencyHudOverlay {
    private FrequencyHudOverlay() {}

    private static final ResourceLocation VIGNETTE = ResourceLocation.fromNamespaceAndPath("minecraft", "textures/misc/vignette.png");

    public static final IGuiOverlay HUD = (gui, gfx, partial, width, height) -> {
        if (Minecraft.getInstance().player == null || Minecraft.getInstance().options.hideGui) return;
        Minecraft mc = Minecraft.getInstance();

        // Tick effect spawner
        FrequencyClientEffects.tick(mc);

        // Draw vignette based on frequency level (no HUD bar/text)
        mc.player.getCapability(FrequencyCapabilityProvider.FREQUENCY_CAPABILITY).ifPresent(cap -> {
            int level = cap.getLevel();
            if (level > 0) {
                float base = 0.06f + (level / 100.0f) * 0.34f; // 0.06 .. 0.40
                long gt = mc.level != null ? mc.level.getGameTime() : 0L;
                float pulse = (float)(0.85 + 0.15 * Math.sin(gt * 0.15));
                float alpha = Math.max(0f, Math.min(1f, base * pulse));
                drawVignette(gfx, width, height, alpha);
            }
        });

        // Draw crazy symbols with varying position/rotation/scale
        FrequencyClientEffects.render(gfx, width, height);
    };

    private static void drawVignette(GuiGraphics gfx, int width, int height, float alpha) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1f, 0f, 0f, alpha); // tint red
        RenderSystem.setShaderTexture(0, VIGNETTE);
        gfx.blit(VIGNETTE, 0, 0, 0, 0, width, height, 256, 256);
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        RenderSystem.disableBlend();
    }
} 