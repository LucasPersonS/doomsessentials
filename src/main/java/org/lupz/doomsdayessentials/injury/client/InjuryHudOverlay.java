package org.lupz.doomsdayessentials.injury.client;

import com.mojang.blaze3d.systems.RenderSystem;
import java.awt.Color;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;
import org.lupz.doomsdayessentials.EssentialsMod;
import org.lupz.doomsdayessentials.injury.capability.InjuryCapabilityProvider;

public class InjuryHudOverlay {
   private static final ResourceLocation BROKEN_BONE_ICON = ResourceLocation.fromNamespaceAndPath(EssentialsMod.MOD_ID, "textures/gui/broken_bone.png");

   public static final IGuiOverlay HUD_INJURY = (gui, guiGraphics, partialTick, width, height) -> {
      Player player = Minecraft.getInstance().player;
      if (player == null) return;

      player.getCapability(InjuryCapabilityProvider.INJURY_CAPABILITY).ifPresent(cap -> {
         int injuryLevel = cap.getInjuryLevel();
         if (injuryLevel > 0) {
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

            int iconSize = 16;
            int padding = 2;
            Font font = Minecraft.getInstance().font;
            String injuryText = "x" + injuryLevel;
            int textWidth = font.width(injuryText);

            int x = 10;
            int y = 10;

            guiGraphics.blit(BROKEN_BONE_ICON, x, y, 0, 0, iconSize, iconSize, iconSize, iconSize);

            int textY = y + (iconSize - font.lineHeight) / 2;
            guiGraphics.drawString(font, injuryText, x + iconSize + padding, textY, 0xFFFFFF);

            RenderSystem.disableBlend();
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
         }

         if (cap.isHealing()) {
            renderHealingHud(guiGraphics, width, height, cap.getHealingProgress());
         }
      });
   };

   private static void renderHealingHud(GuiGraphics guiGraphics, int width, int height, float healingProgress) {
      Minecraft minecraft = Minecraft.getInstance();
      Font font = minecraft.font;
      String text = (healingProgress < 0.01f)
              ? "Healing..."
              : String.format("Healing: %.0f%%", healingProgress * 100.0F);

      int barWidth = 120;
      int barHeight = 10;
      int x = width / 2 - barWidth / 2;
      int y = height / 2 + 20;

      // Dark grey background for better contrast
      guiGraphics.fill(x - 1, y - 1, x + barWidth + 1, y + barHeight + 1, 0xFF444444);

      int progressWidth = Math.min(barWidth, (int)(barWidth * healingProgress));
      if (progressWidth == 0 && healingProgress > 0f) progressWidth = 1; // ensure visible
      guiGraphics.fill(x, y, x + progressWidth, y + barHeight, Color.GREEN.getRGB());

      // Draw text just above the bar for clarity
      int textX = width / 2 - font.width(text) / 2;
      int textY = y - font.lineHeight - 2;
      guiGraphics.drawString(font, text, textX, textY, 0xFFFFFF);
   }
} 