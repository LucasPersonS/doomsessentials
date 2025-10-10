package org.lupz.doomsdayessentials.mixin.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;
import org.lupz.doomsdayessentials.guild.menu.GuildStorageScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Replace vanilla item count/durability text for GuildStorageScreen only.
 * Drawing here preserves vanilla layering: decorations are drawn after item icons, before tooltips.
 */
@Mixin(GuiGraphics.class)
public abstract class GuiGraphicsMixin {

    @Inject(method = "renderItemDecorations(Lnet/minecraft/client/gui/Font;Lnet/minecraft/world/item/ItemStack;II)V",
            at = @At("HEAD"), cancellable = true, require = 0)
    private void dooms$replaceDecorations4(Font font, ItemStack stack, int x, int y, CallbackInfo ci) {
        if (Minecraft.getInstance().screen instanceof GuildStorageScreen) {
            drawGuildCount(font, stack, x, y, null);
            ci.cancel();
        }
    }

    @Inject(method = "renderItemDecorations(Lnet/minecraft/client/gui/Font;Lnet/minecraft/world/item/ItemStack;IILjava/lang/String;)V",
            at = @At("HEAD"), cancellable = true, require = 0)
    private void dooms$replaceDecorations5(Font font, ItemStack stack, int x, int y, String text, CallbackInfo ci) {
        if (Minecraft.getInstance().screen instanceof GuildStorageScreen) {
            drawGuildCount(font, stack, x, y, text);
            ci.cancel();
        }
    }

    private void drawGuildCount(Font font, ItemStack stack, int x, int y, String vanillaText) {
        if (stack.isEmpty()) return;

        // Normalize vanilla text: treat empty strings as no text
        String vanilla = (vanillaText != null && !vanillaText.isEmpty()) ? vanillaText : null;

        // Decide text: k-format >= 1000; otherwise vanilla text (or computed count when > 1)
        int cnt = stack.getCount();
        String text = (cnt >= 1000)
                ? formatK(cnt)
                : (vanilla != null ? vanilla : (cnt > 1 ? Integer.toString(cnt) : null));
        if (text == null || text.isEmpty()) return;

        GuiGraphics self = (GuiGraphics)(Object)this;
        float scale = 0.75f;
        int textW = font.width(text);
        // Bottom-right of the 16x16 area relative to x,y with 1px padding
        float right = x + 16 - 1;
        float bottom = y + 16 - 1;
        float scaledH = 9.0f * scale;
        float drawX = right - textW * scale;
        float drawY = bottom - scaledH;

        self.pose().pushPose();
        // Match vanilla overlay depth so it renders above item but below tooltips
        self.pose().translate(0, 0, 200);
        self.pose().translate(drawX, drawY, 0);
        self.pose().scale(scale, scale, 1f);
        self.drawString(font, text, 0, 0, 0xFFFFFF, true);
        self.pose().popPose();
    }

    private static String formatK(int c) {
        double k = c / 1000.0;
        return String.format(java.util.Locale.ROOT, "%.1fk", k);
    }
}
