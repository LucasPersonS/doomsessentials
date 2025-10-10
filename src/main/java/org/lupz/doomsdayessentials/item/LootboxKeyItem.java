package org.lupz.doomsdayessentials.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class LootboxKeyItem extends Item {
	private final String rarity;

	public LootboxKeyItem(Properties props, String rarity) {
		super(props);
		this.rarity = rarity;
	}

	public String getRarity() { return rarity; }

	@Override
	public Component getName(ItemStack stack) {
		String r = rarity.toUpperCase();
		return Component.literal("§b§lChave §f§l" + r);
	}

	@Override
	public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
		super.appendHoverText(stack, level, tooltip, flag);
		tooltip.add(Component.literal("§7Clique com botão direito para abrir uma §6Caixa §f" + rarity + " §7(do inventário)"));
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
		ItemStack keyStack = player.getItemInHand(hand);
		int slot = findLootboxSlot(player);
		if (slot == -1) {
			if (!level.isClientSide) player.displayClientMessage(Component.literal("§cVocê não possui uma §6Caixa §f" + rarity + " §cno inventário."), true);
			return InteractionResultHolder.fail(keyStack);
		}
		if (!level.isClientSide) {
			java.util.List<net.minecraft.world.item.ItemStack> pool = org.lupz.doomsdayessentials.lootbox.LootboxManager.getAllAsStacks(rarity);
			net.minecraftforge.network.NetworkHooks.openScreen((net.minecraft.server.level.ServerPlayer) player,
					new org.lupz.doomsdayessentials.lootbox.LootboxMenuProvider(rarity, pool), buf -> {
						buf.writeUtf(rarity);
						buf.writeVarInt(pool.size());
						for (net.minecraft.world.item.ItemStack s : pool) buf.writeItem(s);
					});
			if (!player.getAbilities().instabuild) {
				keyStack.shrink(1);
				consumeFromSlot(player, slot);
			}
		}
		return InteractionResultHolder.sidedSuccess(keyStack, level.isClientSide);
	}

	private int findLootboxSlot(Player player) {
		// Prefer other hand first
		ItemStack main = player.getMainHandItem();
		ItemStack off = player.getOffhandItem();
		if (main.getItem() instanceof LootboxItem lb && lb.getRarity().equals(this.rarity)) return player.getInventory().selected;
		if (off.getItem() instanceof LootboxItem lb2 && lb2.getRarity().equals(this.rarity)) return 40; // offhand index
		// Search inventory (0-35)
		for (int i = 0; i < 36; i++) {
			ItemStack st = player.getInventory().getItem(i);
			if (st.getItem() instanceof LootboxItem lb3 && lb3.getRarity().equals(this.rarity)) return i;
		}
		return -1;
	}

	private void consumeFromSlot(Player player, int slot) {
		if (slot == 40) {
			player.getOffhandItem().shrink(1);
			return;
		}
		if (slot == player.getInventory().selected) {
			player.getMainHandItem().shrink(1);
			return;
		}
		ItemStack s = player.getInventory().getItem(slot);
		s.shrink(1);
		player.getInventory().setItem(slot, s);
	}
} 