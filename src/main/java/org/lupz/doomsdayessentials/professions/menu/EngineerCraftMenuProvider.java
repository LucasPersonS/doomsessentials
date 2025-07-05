package org.lupz.doomsdayessentials.professions.menu;

import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;

public class EngineerCraftMenuProvider implements MenuProvider {
    @Override public Component getDisplayName(){return Component.literal("Oficina do Engenheiro");}
    @Override public AbstractContainerMenu createMenu(int id, Inventory inv, Player player){return new EngineerCraftMenu(id, inv);}  
} 