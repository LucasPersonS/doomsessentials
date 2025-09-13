package org.lupz.doomsdayessentials.professions.menu;

import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.NotNull;
import org.lupz.doomsdayessentials.professions.bounty.BountyManager;

import java.util.ArrayList;
import java.util.List;

public class BountyBoardMenu extends AbstractContainerMenu {
	private static final int SIZE = 54;
	private static final int SLOT_PREV = 45;
	private static final int SLOT_NEXT = 53;
	private static final int SLOT_PLACE = 49;

	private final Container container = new SimpleContainer(SIZE);
	private final Player player;
	private int page = 0;

	public BountyBoardMenu(int windowId, Inventory inv) {
		super(ProfessionMenuTypes.BOUNTY_BOARD_MENU.get(), windowId);
		this.player = inv.player;
		buildContents();
		for (int row = 0; row < 6; ++row) {
			for (int col = 0; col < 9; ++col) {
				this.addSlot(new ReadOnlySlot(container, col + row * 9, 8 + col * 18, 18 + row * 18));
			}
		}
	}

	private void buildContents() {
		List<BountyManager.Bounty> all = BountyManager.listAll();
		int start = page * 45; // 5 rows x 9 cols for content (exclude bottom row for controls)
		int end = Math.min(start + 45, all.size());
		// clear
		for (int i = 0; i < SIZE; i++) container.setItem(i, ItemStack.EMPTY);

		int slot = 0;
		for (int i = start; i < end; i++) {
			BountyManager.Bounty b = all.get(i);
			ItemStack head = new ItemStack(Items.PLAYER_HEAD);
			String targetName = resolveName(b.target);
			head.setHoverName(Component.literal("§f" + targetName));
			List<Component> lore = new ArrayList<>();
			lore.add(Component.literal("§7Recompensa: §e" + b.gears + " gears"));
			lore.add(Component.literal("§7Colocada por: §f" + resolveName(b.placedBy)));
			addLore(head, lore);
			container.setItem(slot++, head);
		}

		// controls
		ItemStack prev = new ItemStack(Items.PLAYER_HEAD);
		prev.setHoverName(Component.literal("§ePágina Anterior"));
		addLore(prev, List.of(Component.literal("§7Clique para voltar")));
		container.setItem(SLOT_PREV, prev);

		ItemStack place = new ItemStack(Items.PLAYER_HEAD);
		place.setHoverName(Component.literal("§aColocar Recompensa"));
		addLore(place, List.of(Component.literal("§7Clique para iniciar")));
		container.setItem(SLOT_PLACE, place);

		ItemStack next = new ItemStack(Items.PLAYER_HEAD);
		next.setHoverName(Component.literal("§ePróxima página"));
		addLore(next, List.of(Component.literal("§7Clique para avançar")));
		container.setItem(SLOT_NEXT, next);

		// fillers
		ItemStack filler = new ItemStack(Items.GRAY_STAINED_GLASS_PANE);
		filler.setHoverName(Component.literal(""));
		for (int i = 0; i < SIZE; i++) {
			if (i == SLOT_PREV || i == SLOT_NEXT) continue;
			if (container.getItem(i).isEmpty()) container.setItem(i, filler.copy());
		}
	}

	private String resolveName(java.util.UUID id) {
		var server = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();
		if (server != null) {
			var p = server.getPlayerList().getPlayer(id);
			if (p != null) return p.getName().getString();
		}
		return id.toString().substring(0, 8);
	}

	private void addLore(ItemStack stack, List<Component> lore) {
		net.minecraft.nbt.ListTag tag = new net.minecraft.nbt.ListTag();
		for (Component c : lore) tag.add(net.minecraft.nbt.StringTag.valueOf(Component.Serializer.toJson(c)));
		stack.getOrCreateTagElement("display").put("Lore", tag);
	}

	@Override
	public void clicked(int slotId, int dragType, @NotNull net.minecraft.world.inventory.ClickType clickType, @NotNull Player clickPlayer) {
		if (clickType != net.minecraft.world.inventory.ClickType.PICKUP) { super.clicked(slotId, dragType, clickType, clickPlayer); return; }
		if (slotId == SLOT_PREV) {
			if (page > 0) { page--; buildContents(); broadcastChanges(); }
			return;
		}
		if (slotId == SLOT_NEXT) {
			int total = BountyManager.listAll().size();
			int maxPage = Math.max(0, (int)Math.ceil(total / 45.0) - 1);
			if (page < maxPage) { page++; buildContents(); broadcastChanges(); }
			return;
		}
		if (slotId == SLOT_PLACE && clickPlayer instanceof net.minecraft.server.level.ServerPlayer sp) {
			org.lupz.doomsdayessentials.professions.bounty.BountyConversationManager.start(sp);
			return;
		}
		// ignore taking items
		super.clicked(slotId, dragType, clickType, clickPlayer);
	}

	@Override
	public boolean stillValid(@NotNull Player p) { return true; }
	@Override
	public @NotNull ItemStack quickMoveStack(Player p, int idx) { return ItemStack.EMPTY; }

	private static class ReadOnlySlot extends Slot {
		public ReadOnlySlot(Container cont, int idx, int x, int y) { super(cont, idx, x, y); }
		@Override public boolean mayPlace(@NotNull ItemStack s) { return false; }
		@Override public boolean mayPickup(@NotNull Player p) { return false; }
	}
} 