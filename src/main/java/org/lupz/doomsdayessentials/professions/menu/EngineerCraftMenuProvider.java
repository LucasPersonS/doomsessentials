package org.lupz.doomsdayessentials.professions.menu;

import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.jetbrains.annotations.Nullable;

public class EngineerCraftMenuProvider implements MenuProvider {
    @Override
    public Component getDisplayName() {
        return Component.translatable("gui.doomsdayessentials.engineer_crafting.title");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player){return new EngineerCraftMenu(id, inv);}  
} 