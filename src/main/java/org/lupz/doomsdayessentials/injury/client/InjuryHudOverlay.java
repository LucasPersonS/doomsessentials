package org.lupz.doomsdayessentials.injury.client;

import org.lwjgl.glfw.GLFW;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.common.Mod;
import org.lupz.doomsdayessentials.EssentialsMod;
import org.lupz.doomsdayessentials.injury.capability.InjuryCapabilityProvider;
import org.lupz.doomsdayessentials.injury.network.InjuryNetwork;
import org.lupz.doomsdayessentials.injury.network.PlayerActionPacket;

@Mod.EventBusSubscriber(modid = EssentialsMod.MOD_ID, value = Dist.CLIENT)
public class InjuryHudOverlay {
   private static final ResourceLocation BROKEN_BONE = ResourceLocation.fromNamespaceAndPath(EssentialsMod.MOD_ID, "textures/gui/broken_bone.png");

   private static int desistirX = -1;
   private static int desistirY = -1;
   private static int desistirW = 180;
   private static int desistirH = 22;

   private static boolean holding = false;
   private static long holdStartMs = 0L;
   private static final long HOLD_DURATION_MS = 10_000L;

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

        if (InjuryClientState.isDowned()) {
            renderDesistir(guiGraphics, screenWidth, screenHeight);
            renderTimer(guiGraphics, screenWidth, screenHeight);
            // Update holding state from raw mouse each frame (if released, reset)
            long handle = mc.getWindow().getWindow();
            boolean rightDown = GLFW.glfwGetMouseButton(handle, GLFW.GLFW_MOUSE_BUTTON_RIGHT) == GLFW.GLFW_PRESS;
            if (!rightDown && holding) {
               holding = false;
               holdStartMs = 0L;
            }
            renderHoldProgress(guiGraphics);
        } else {
            // Reset if state ends
            holding = false;
            holdStartMs = 0L;
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

      guiGraphics.fill(x - 1, y - 1, x + barWidth + 1, y + barHeight + 1, 0xFF000000);
      guiGraphics.fill(x, y, x + (int) (progress * barWidth), y + barHeight, 0xFF00FF00);
   }

   private static void renderDesistir(GuiGraphics guiGraphics, int screenWidth, int screenHeight) {
      String label = "DESISTIR (Segure botão direito 10s)";
      Minecraft mc = Minecraft.getInstance();
      Font font = mc.font;
      int w = Math.max(180, font.width(label) + 20);
      int h = 22;
      int x = (screenWidth - w) / 2;
      int y = screenHeight / 2 + 50;

      desistirX = x;
      desistirY = y;
      desistirW = w;
      desistirH = h;

      // Background
      guiGraphics.fill(x, y, x + w, y + h, 0xAA000000);
      // Border
      guiGraphics.fill(x, y, x + w, y + 1, 0xFFFFFFFF);
      guiGraphics.fill(x, y + h - 1, x + w, y + h, 0xFFFFFFFF);
      guiGraphics.fill(x, y, x + 1, y + h, 0xFFFFFFFF);
      guiGraphics.fill(x + w - 1, y, x + w, y + h, 0xFFFFFFFF);

      // Label
      guiGraphics.drawCenteredString(font, label, x + w / 2, y + (h - 8) / 2, 0xFFFFFF);

      // Hint text
      String hint = "Sem itens enquanto incapacitado";
      guiGraphics.drawCenteredString(font, hint, screenWidth / 2, y + h + 6, 0xFFAAAAAA);
   }

   private static void renderTimer(GuiGraphics guiGraphics, int screenWidth, int screenHeight) {
      long remainingMillis = InjuryClientState.getDownedUntil() - System.currentTimeMillis();
      if (remainingMillis <= 0) return;
      int secs = (int) (remainingMillis / 1000);
      int mm = Math.max(0, secs / 60);
      int ss = Math.max(0, secs % 60);
      String timer = String.format("Sangrando em: %02d:%02d", mm, ss);
      Minecraft mc = Minecraft.getInstance();
      Font font = mc.font;
      guiGraphics.drawCenteredString(font, timer, screenWidth / 2, screenHeight / 2 + 30, 0xFFFF5555);
   }

   private static void renderHoldProgress(GuiGraphics guiGraphics) {
      if (!holding || holdStartMs <= 0L) return;
      long elapsed = System.currentTimeMillis() - holdStartMs;
      float frac = Math.min(1f, (float) elapsed / (float) HOLD_DURATION_MS);
      int px = desistirX + 3;
      int py = desistirY + desistirH - 6;
      int pw = desistirW - 6;
      int ph = 4;
      // Background
      guiGraphics.fill(px - 1, py - 1, px + pw + 1, py + ph + 1, 0xCC000000);
      // Fill based on progress
      guiGraphics.fill(px, py, px + (int)(pw * frac), py + ph, 0xFF55FF55);
      if (frac >= 1f) {
         // Completed – send give up
         InjuryNetwork.sendToServer(new PlayerActionPacket(PlayerActionPacket.Action.GIVE_UP));
         holding = false;
         holdStartMs = 0L;
      }
   }

   @SubscribeEvent
   public static void onMouseClick(InputEvent.MouseButton event) {
      if (!InjuryClientState.isDowned()) return;
      Minecraft mc = Minecraft.getInstance();
      if (mc.player == null) return;

      int action = event.getAction();
      int button = event.getButton();

      if (action == 1 && button == 1) { // Press right anywhere
         holding = true;
         holdStartMs = System.currentTimeMillis();
      } else if (action == 0 && button == 1) { // Release right
         holding = false;
         holdStartMs = 0L;
      }
   }

   // Note: LeftClickEmpty/RightClickEmpty are not cancelable on client. Server-side cancel guards already exist.
} 