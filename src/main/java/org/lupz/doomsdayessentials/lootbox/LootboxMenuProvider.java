package org.lupz.doomsdayessentials.lootbox;

import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public class LootboxMenuProvider implements MenuProvider {
	private final String rarity;
	private final java.util.List<ItemStack> pool;

	public LootboxMenuProvider(String rarity, java.util.List<ItemStack> pool) {
		this.rarity = rarity;
		this.pool = pool;
	}

	@Override
	public Component getDisplayName() {
		return Component.literal("§e§lCAIXA " + rarity.toUpperCase());
	}

	@Override
	public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
		return new LootboxMenu(id, inv, rarity, pool);
	}
} 