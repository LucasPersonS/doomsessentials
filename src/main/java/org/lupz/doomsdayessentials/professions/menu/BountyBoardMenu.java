package org.lupz.doomsdayessentials.professions.menu;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import org.jetbrains.annotations.NotNull;

public class BountyBoardMenu extends AbstractContainerMenu {
	public BountyBoardMenu(int windowId, Inventory inv) {
		super(ProfessionMenuTypes.BOUNTY_BOARD_MENU.get(), windowId);
	}

	@Override
	public boolean stillValid(@NotNull net.minecraft.world.entity.player.Player player) {
		return true;
	}

	@Override
	public @NotNull net.minecraft.world.item.ItemStack quickMoveStack(@NotNull net.minecraft.world.entity.player.Player player, int index) {
		return net.minecraft.world.item.ItemStack.EMPTY;
	}
} 