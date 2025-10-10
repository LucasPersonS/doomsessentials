package org.lupz.doomsdayessentials.rarity.client;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lupz.doomsdayessentials.EssentialsMod;
import org.lupz.doomsdayessentials.rarity.RarityManager;

/** Adds a styled rarity line to the item tooltip (lore). */
@Mod.EventBusSubscriber(modid = EssentialsMod.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class RarityTooltipOverlay {
    private RarityTooltipOverlay() {}

    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent e) {
        // Intentionally disabled – we now use a textured banner via RarityTooltipComponents
        // to avoid adding extra vanilla-style text lines like "Uncommon", etc.
        return;
    }
}
