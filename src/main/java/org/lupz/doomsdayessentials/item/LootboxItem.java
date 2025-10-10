package org.lupz.doomsdayessentials.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class LootboxItem extends Item {
	private final String rarity;

	public LootboxItem(Properties props, String rarity) {
		super(props);
		this.rarity = rarity;
	}

	public String getRarity() { return rarity; }

	@Override
	public Component getName(ItemStack stack) {
		String r = rarity.toUpperCase();
		return Component.literal("§6§lCaixa §e§l" + r);
	}

	@Override
	public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
		super.appendHoverText(stack, level, tooltip, flag);
		tooltip.add(Component.literal("§7Abra com uma chave §f" + rarity));
	}
} 