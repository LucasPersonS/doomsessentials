package org.lupz.doomsdayessentials.menu;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.lupz.doomsdayessentials.blockentity.EscavadeiraControllerBlockEntity;
import org.lupz.doomsdayessentials.registry.EscavadeiraRegistries;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.SlotItemHandler;
import org.lupz.doomsdayessentials.item.ModItems;

public class EscavadeiraMenu extends AbstractContainerMenu {
	private static final int SIZE = 54;
	private static final int SLOT_ON = 21;  // green dye
	private static final int SLOT_OFF = 23; // red dye
	private static final int SLOT_FUEL = 22; // center
	private static final int SLOT_PREV = 18; // profile prev (row 2 col 1)
	private static final int SLOT_NEXT = 26; // profile next (row 2 col 9)
	private static final int SLOT_PROFILE = 13; // paper above fuel slot

	private final SimpleContainer hud = new SimpleContainer(SIZE);
	private final Level level;
	private final BlockPos pos;

	// Slot index bookkeeping for quickMove and button clicks
	private int idxFuelSlot = -1;
	private int idxOutStart = -1, idxOutEnd = -1;
	private int idxPlayerStart = -1, idxPlayerEnd = -1;
	private int idxOnHud = -1, idxOffHud = -1;
	private int idxPrev = -1, idxNext = -1, idxProfile = -1;

	public EscavadeiraMenu(int windowId, Inventory inv, BlockPos pos) {
		super(EscavadeiraRegistries.ESCAVADEIRA_MENU_TYPE, windowId);
		this.level = inv.player.level();
		this.pos = pos;
		buildHud();

		// HUD layer top 3 rows
		for (int row = 0; row < 3; ++row) {
			for (int col = 0; col < 9; ++col) {
				int index = col + row * 9;
				if (index == SLOT_FUEL) continue;
				int x = 8 + col * 18;
				int y = 18 + row * 18;
				if (index == SLOT_ON) this.idxOnHud = this.slots.size();
				if (index == SLOT_OFF) this.idxOffHud = this.slots.size();
				if (index == SLOT_PREV) this.idxPrev = this.slots.size();
				if (index == SLOT_NEXT) this.idxNext = this.slots.size();
				if (index == SLOT_PROFILE) this.idxProfile = this.slots.size();
				this.addSlot(new ReadOnlySlot(hud, index, x, y));
			}
		}

		// Functional slots
		EscavadeiraControllerBlockEntity be = getBE();
		if (be != null) {
			IItemHandler fuel = be.getFuelHandler();
			this.idxFuelSlot = this.slots.size();
			this.addSlot(new SlotItemHandler(fuel, 0, 8 + (SLOT_FUEL % 9) * 18, 18 + (SLOT_FUEL / 9) * 18));

			IItemHandler out = be.getOutputHandler();
			this.idxOutStart = this.slots.size();
			int outIndex = 0;
			for (int r = 3; r < 6; r++) {
				for (int c = 0; c < 9; c++) {
					if (outIndex >= out.getSlots()) break;
					this.addSlot(new SlotItemHandler(out, outIndex++, 8 + c * 18, 18 + r * 18));
				}
			}
			this.idxOutEnd = this.slots.size();
		}

		// Player inventory
		this.idxPlayerStart = this.slots.size();
		int invStartY = 140;
		for (int r = 0; r < 3; r++) {
			for (int c = 0; c < 9; c++) {
				this.addSlot(new Slot(inv, c + r * 9 + 9, 8 + c * 18, invStartY + r * 18));
			}
		}
		int hotbarY = 198;
		for (int c = 0; c < 9; c++) {
			this.addSlot(new Slot(inv, c, 8 + c * 18, hotbarY));
		}
		this.idxPlayerEnd = this.slots.size();

		// Initialize profile label with current value
		updateProfileLabel();
	}

	public static EscavadeiraMenu fromNetwork(int windowId, Inventory inv, FriendlyByteBuf buf) {
		BlockPos pos = buf.readBlockPos();
		return new EscavadeiraMenu(windowId, inv, pos);
	}

	private void buildHud() {
		for (int i = 0; i < SIZE; i++) hud.setItem(i, ItemStack.EMPTY);
		ItemStack on = new ItemStack(Items.LIME_DYE); on.setHoverName(Component.literal("§aLigar")); hud.setItem(SLOT_ON, on);
		ItemStack off = new ItemStack(Items.RED_DYE); off.setHoverName(Component.literal("§cDesligar")); hud.setItem(SLOT_OFF, off);
		ItemStack prev = new ItemStack(ModItems.GUI_BACK.get()); prev.setHoverName(Component.literal("§7Perfil §f<")); hud.setItem(SLOT_PREV, prev);
		ItemStack next = new ItemStack(ModItems.GUI_NEXT.get()); next.setHoverName(Component.literal("§7Perfil §f>")); hud.setItem(SLOT_NEXT, next);
		ItemStack profile = new ItemStack(Items.PAPER); profile.setHoverName(Component.literal("§ePerfil")); hud.setItem(SLOT_PROFILE, profile);
		ItemStack filler = new ItemStack(Items.GRAY_STAINED_GLASS_PANE); filler.setHoverName(Component.literal(""));
		for (int i = 0; i < 27; i++) {
			if (i == SLOT_ON || i == SLOT_OFF || i == SLOT_FUEL || i == SLOT_PREV || i == SLOT_NEXT || i == SLOT_PROFILE) continue;
			if (hud.getItem(i).isEmpty()) hud.setItem(i, filler.copy());
		}
	}

	private void updateProfileLabel() {
		EscavadeiraControllerBlockEntity be = getBE();
		if (be == null) return;
		ItemStack label = new ItemStack(Items.PAPER);
		label.setHoverName(Component.literal("§ePerfil: §f" + be.getProfile().name()));
		hud.setItem(SLOT_PROFILE, label);
		broadcastChanges();
	}

	private EscavadeiraControllerBlockEntity getBE() {
		BlockPos p = this.pos;
		if (p == null || !(level.getBlockEntity(p) instanceof EscavadeiraControllerBlockEntity be)) return null;
		return be;
	}

	@Override
	public void clicked(int slotId, int dragType, @NotNull ClickType clickType, @NotNull Player player) {
		if (clickType != ClickType.PICKUP) { super.clicked(slotId, dragType, clickType, player); return; }
		EscavadeiraControllerBlockEntity be = getBE();
		if (be == null) { super.clicked(slotId, dragType, clickType, player); return; }
		if (slotId == idxOnHud) {
			net.minecraftforge.items.IItemHandler fuel = be.getFuelHandler();
			if (fuel.getStackInSlot(0).isEmpty()) {
				for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
					ItemStack st = player.getInventory().getItem(i);
					if (!st.isEmpty() && (st.is(Items.COAL) || st.is(Items.CHARCOAL) || st.is(Items.COAL_BLOCK) || st.is(Items.BLAZE_ROD))) {
						ItemStack one = st.copy(); one.setCount(1);
						ItemStack rem = fuel.insertItem(0, one, false);
						if (rem.isEmpty()) { st.shrink(1); break; }
					}
				}
			}
			be.setRunning(true);
			return;
		}
		if (slotId == idxOffHud) { be.setRunning(false); return; }
		if (slotId == idxPrev) { be.cycleProfile(false); updateProfileLabel(); player.displayClientMessage(Component.literal("§7Perfil: §f" + be.getProfile().name()), true); return; }
		if (slotId == idxNext) { be.cycleProfile(true); updateProfileLabel(); player.displayClientMessage(Component.literal("§7Perfil: §f" + be.getProfile().name()), true); return; }
		super.clicked(slotId, dragType, clickType, player);
	}

	@Override
	public boolean stillValid(@NotNull Player p) { return true; }

	@Override
	public @NotNull ItemStack quickMoveStack(@NotNull Player player, int index) {
		ItemStack empty = ItemStack.EMPTY;
		Slot slot = this.slots.get(index);
		if (slot == null || !slot.hasItem()) return empty;
		ItemStack stack = slot.getItem();
		ItemStack stackCopy = stack.copy();

		boolean isPlayer = index >= idxPlayerStart && index < idxPlayerEnd;
		boolean isOutput = index >= idxOutStart && index < idxOutEnd;
		boolean isFuelSlot = index == idxFuelSlot;

		if (isPlayer) {
			if (stack.is(Items.COAL) || stack.is(Items.CHARCOAL) || stack.is(Items.COAL_BLOCK) || stack.is(Items.BLAZE_ROD)) {
				if (!this.moveItemStackTo(stack, idxFuelSlot, idxFuelSlot + 1, false)) return ItemStack.EMPTY;
				return stackCopy;
			}
			if (this.moveItemStackTo(stack, idxOutStart, idxOutEnd, false)) { return stackCopy; }
		}
		else if (isOutput || isFuelSlot) {
			if (this.moveItemStackTo(stack, idxPlayerStart, idxPlayerEnd, false)) { return stackCopy; }
		}
		return ItemStack.EMPTY;
	}

	private static class ReadOnlySlot extends Slot {
		public ReadOnlySlot(SimpleContainer cont, int idx, int x, int y) { super(cont, idx, x, y); }
		@Override public boolean mayPlace(@NotNull ItemStack s) { return false; }
		@Override public boolean mayPickup(@NotNull Player p) { return false; }
	}
} 