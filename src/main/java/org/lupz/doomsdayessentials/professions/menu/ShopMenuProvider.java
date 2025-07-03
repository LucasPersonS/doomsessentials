package org.lupz.doomsdayessentials.professions.menu;

import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.jetbrains.annotations.NotNull;

public class ShopMenuProvider implements MenuProvider {
    @Override
    public @NotNull Component getDisplayName() {
        return Component.literal("§6Loja de Profissões");
    }

    @Override
    public AbstractContainerMenu createMenu(int windowId, Inventory inv, net.minecraft.world.entity.player.Player player) {
        return new ShopMenu(windowId, inv);
    }
} 