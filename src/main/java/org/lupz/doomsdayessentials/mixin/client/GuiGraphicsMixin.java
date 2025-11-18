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
            ci.cancel();
        }
    }

    @Inject(method = "renderItemDecorations(Lnet/minecraft/client/gui/Font;Lnet/minecraft/world/item/ItemStack;IILjava/lang/String;)V",
            at = @At("HEAD"), cancellable = true, require = 0)
    private void dooms$replaceDecorations5(Font font, ItemStack stack, int x, int y, String text, CallbackInfo ci) {
        if (Minecraft.getInstance().screen instanceof GuildStorageScreen) {
            ci.cancel();
        }
    }

    private void drawGuildCount(Font font, ItemStack stack, int x, int y, String vanillaText) {}

    private static String formatK(int c) {
        double k = c / 1000.0;
        return String.format(java.util.Locale.ROOT, "%.1fk", k);
    }
}
