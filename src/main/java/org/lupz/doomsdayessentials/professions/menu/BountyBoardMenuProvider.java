package org.lupz.doomsdayessentials.professions.menu;

import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;

public class BountyBoardMenuProvider implements MenuProvider {
	@Override
	public Component getDisplayName() {
		return Component.translatable("menu.doomsdayessentials.bounty_board");
	}

	@Override
	public AbstractContainerMenu createMenu(int windowId, Inventory inv, Player player) {
		return new BountyBoardMenu(windowId, inv);
	}
} 