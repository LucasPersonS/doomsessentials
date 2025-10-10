package org.lupz.doomsdayessentials.mixin.client;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.item.ItemStack;
import org.lupz.doomsdayessentials.guild.menu.GuildStorageScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Suppress vanilla item count/durability decorations for our GuildStorageScreen by
 * redirecting the call to GuiGraphics.renderItemDecorations inside AbstractContainerScreen#renderSlot.
 *
 * MC 1.20.1 method target:
 * GuiGraphics.renderItemDecorations(Font, ItemStack, int, int)
 */
@Mixin(AbstractContainerScreen.class)
public abstract class AbstractContainerScreenMixin { }
