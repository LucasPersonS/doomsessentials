package org.lupz.doomsdayessentials.injury.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;
import org.lupz.doomsdayessentials.EssentialsMod;
import org.lupz.doomsdayessentials.injury.capability.InjuryCapabilityProvider;

public class InjuryHudOverlay {
   private static final ResourceLocation BROKEN_BONE = new ResourceLocation(EssentialsMod.MOD_ID, "textures/gui/broken_bone.png");

   public static final IGuiOverlay HUD_INJURY = (gui, guiGraphics, partialTicks, screenWidth, screenHeight) -> {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player == null || mc.options.hideGui) {
         return;
      }
      mc.player.getCapability(InjuryCapabilityProvider.INJURY_CAPABILITY).ifPresent(cap -> {
         int injuryLevel = cap.getInjuryLevel();
         if (injuryLevel > 0) {
            renderInjuryLevel(guiGraphics, screenWidth, screenHeight, injuryLevel);
         }

         float healingProgress = cap.getHealingProgress();
         if (healingProgress > 0.0f) {
            renderHealingProgress(guiGraphics, screenWidth, screenHeight, healingProgress);
         }
      });
   };

   private static void renderInjuryLevel(GuiGraphics guiGraphics, int screenWidth, int screenHeight, int level) {
      int iconSize = 16;
      int padding = 2;
      int startX = 10;
      int y = 10;

      for (int i = 0; i < level; i++) {
         guiGraphics.blit(BROKEN_BONE, startX + i * (iconSize + padding), y, 0, 0, iconSize, iconSize, iconSize, iconSize);
      }
   }

   private static void renderHealingProgress(GuiGraphics guiGraphics, int screenWidth, int screenHeight, float progress) {
      int barWidth = 100;
      int barHeight = 5;
      int x = (screenWidth - barWidth) / 2;
      int y = screenHeight - 50;

      guiGraphics.fill(x - 1, y - 1, x + barWidth + 1, y + barHeight + 1, 0xFF000000); // Black background
      guiGraphics.fill(x, y, x + (int) (progress * barWidth), y + barHeight, 0xFF00FF00); // Green progress
   }
} 