package org.lupz.doomsdayessentials.lootbox;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.NotNull;
import org.lupz.doomsdayessentials.sound.ModSounds;

import java.util.ArrayList;
import java.util.List;

public class LootboxMenu extends AbstractContainerMenu {
	private static final int GUI_SIZE = 27; // 3 rows x 9 cols for showcase
	private static final int ROWS = 3;
	private static final int COLS = 9;
	private final Container container = new SimpleContainer(GUI_SIZE);
	private final Player player;
	private final String rarity;
	private final List<ItemStack> pool; // cached pool for client rotation

	// Animation state
	private int tick;
	private int colorIndex;
	private boolean rolling;
	private int rollTicks;
	private ItemStack finalReward = ItemStack.EMPTY;
	private int refreshIntervalTicks = 2;

	private static final Item[] GLASS = new Item[]{
			Items.WHITE_STAINED_GLASS_PANE, Items.LIGHT_GRAY_STAINED_GLASS_PANE, Items.GRAY_STAINED_GLASS_PANE,
			Items.BLACK_STAINED_GLASS_PANE, Items.RED_STAINED_GLASS_PANE, Items.ORANGE_STAINED_GLASS_PANE,
			Items.YELLOW_STAINED_GLASS_PANE, Items.LIME_STAINED_GLASS_PANE, Items.GREEN_STAINED_GLASS_PANE,
			Items.CYAN_STAINED_GLASS_PANE, Items.LIGHT_BLUE_STAINED_GLASS_PANE, Items.BLUE_STAINED_GLASS_PANE,
			Items.PURPLE_STAINED_GLASS_PANE, Items.MAGENTA_STAINED_GLASS_PANE, Items.PINK_STAINED_GLASS_PANE
	};

	public LootboxMenu(int windowId, Inventory inv, String rarity) {
		this(windowId, inv, rarity, LootboxManager.getAllAsStacks(rarity));
	}

	public LootboxMenu(int windowId, Inventory inv, String rarity, List<ItemStack> preloadedPool) {
		super(LootboxMenus.LOOTBOX_MENU.get(), windowId);
		this.player = inv.player;
		this.rarity = rarity;
		this.pool = new ArrayList<>(preloadedPool);
		buildInitial();

		// Showcase slots (3x9)
		for (int row = 0; row < ROWS; ++row) {
			for (int col = 0; col < COLS; ++col) {
				this.addSlot(new ReadOnlySlot(container, col + row * COLS, 8 + col * 18, 18 + row * 18));
			}
		}

		// Player inventory (3 rows) + hotbar
		int invY = 18 + ROWS * 18 + 12; // a little gap below showcase
		for (int row = 0; row < 3; ++row) {
			for (int col = 0; col < 9; ++col) {
				this.addSlot(new net.minecraft.world.inventory.Slot(inv, col + row * 9 + 9, 8 + col * 18, invY + row * 18));
			}
		}
		for (int col = 0; col < 9; ++col) {
			this.addSlot(new net.minecraft.world.inventory.Slot(inv, col, 8 + col * 18, invY + 58));
		}
	}

	private void buildInitial() {
		for (int i = 0; i < GUI_SIZE; i++) container.setItem(i, ItemStack.EMPTY);
		ItemStack pane = new ItemStack(GLASS[colorIndex % GLASS.length]);
		pane.setHoverName(Component.literal(""));
		for (int c = 0; c < COLS; c++) container.setItem(c, pane.copy());
		for (int c = 0; c < COLS; c++) container.setItem((ROWS - 1) * COLS + c, pane.copy());
		refreshMiddleRow();
		rolling = true;
		rollTicks = 0;
		refreshIntervalTicks = 2;
		// Play initial spin sound on client
		if (player.level().isClientSide) {
			player.playSound(ModSounds.LOOTBOX_SPIN.get(), 0.8f, 1.0f);
		}
	}

	private void refreshMiddleRow() {
		int base = COLS; // middle row index start
		
		if (pool.isEmpty()) {
			for (int i = 0; i < COLS; i++) container.setItem(base + i, new ItemStack(Items.BARRIER));
			return;
		}
		
		for (int i = 0; i < COLS; i++) {
			ItemStack randomItem = pool.get(player.getRandom().nextInt(pool.size())).copy();
			container.setItem(base + i, randomItem);
		}
	}

	public void clientTickAnimate() {
		tick++;
		if (!rolling) return;
		rollTicks++;

		// Gradual slowdown: every second, increase refresh interval a bit
		if (rollTicks % 20 == 0) {
			if (refreshIntervalTicks < 12) refreshIntervalTicks += 2; // 2 -> 4 -> 6 -> ... -> 12
			// Final seconds: slow even more
			if (rollTicks >= 160 && refreshIntervalTicks < 16) refreshIntervalTicks += 2;
		}

		// Animate borders
		if (tick % 5 == 0) {
			colorIndex = (colorIndex + 1) % GLASS.length;
			ItemStack pane = new ItemStack(GLASS[colorIndex]);
			pane.setHoverName(Component.literal(""));
			for (int c = 0; c < COLS; c++) container.setItem(c, pane.copy());
			for (int c = 0; c < COLS; c++) container.setItem((ROWS - 1) * COLS + c, pane.copy());
		}
		// Rotate items with current interval (and play tick sound in sync on client)
		if (tick % Math.max(1, refreshIntervalTicks) == 0) {
			refreshMiddleRow();
			if (player.level().isClientSide) {
				float pitchBase = 1.8f - 0.06f * refreshIntervalTicks; // faster -> higher pitch
				float pitch = Math.max(0.7f, Math.min(1.8f, pitchBase));
				player.playSound(net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK.value(), 0.25f, pitch);
			}
		}
		// Stop after ~10 seconds
		if (rollTicks >= 200) {
			finishRoll();
		}
		broadcastChanges();
	}

	@Override
	public void removed(@NotNull Player p) {
		super.removed(p);
		if (!p.level().isClientSide && rolling && p instanceof net.minecraft.server.level.ServerPlayer sp) {
			// If closed early, finalize immediately on server
			org.lupz.doomsdayessentials.lootbox.network.LootboxFinishPacket.finalizeServer(sp, rarity);
		}
	}

	private void finishRoll() {
		rolling = false;
		// Pick a single weighted reward and place in center (middle row, center column = index base+4)
		List<LootboxManager.LootEntry> entries = LootboxManager.getEntries(rarity);
		LootboxManager.LootEntry picked = pickWeighted(entries);
		finalReward = picked != null ? picked.toStack() : ItemStack.EMPTY;
		int center = COLS + 4;
		container.setItem(center, finalReward.copy());
		broadcastChanges();
		
		// Completion feedback on server
		if (!player.level().isClientSide) {
			player.level().playSound(null, player.getX(), player.getY(), player.getZ(), 
				ModSounds.LOOTBOX_COMPLETE.get(), SoundSource.PLAYERS, 1.0f, 1.0f);
			if (player.level() instanceof ServerLevel serverLevel) {
				spawnRewardParticles(serverLevel);
			}
		}
		
		// Announce and give item
		if (player.level().isClientSide) {
			org.lupz.doomsdayessentials.network.PacketHandler.CHANNEL.sendToServer(
					new org.lupz.doomsdayessentials.lootbox.network.LootboxFinishPacket(rarity, this.containerId));
		}
	}

	private void spawnRewardParticles(ServerLevel level) {
		// Spawn golden particles around the player
		double x = player.getX();
		double y = player.getY() + 1.0;
		double z = player.getZ();
		
		// Circle of happy particles
		for (int i = 0; i < 20; i++) {
			double angle = (i / 20.0) * 2 * Math.PI;
			double offsetX = Math.cos(angle) * 2.0;
			double offsetZ = Math.sin(angle) * 2.0;
			level.sendParticles(ParticleTypes.HAPPY_VILLAGER, 
				x + offsetX, y, z + offsetZ, 1, 0.1, 0.1, 0.1, 0.05);
		}
		
		// Firework-like burst
		for (int i = 0; i < 15; i++) {
			double offsetX = (player.getRandom().nextDouble() - 0.5) * 4.0;
			double offsetY = player.getRandom().nextDouble() * 2.0;
			double offsetZ = (player.getRandom().nextDouble() - 0.5) * 4.0;
			level.sendParticles(ParticleTypes.FIREWORK, 
				x + offsetX, y + offsetY, z + offsetZ, 1, 0, 0.1, 0, 0.1);
		}
		
		// Golden sparkles
		for (int i = 0; i < 30; i++) {
			double offsetX = (player.getRandom().nextDouble() - 0.5) * 3.0;
			double offsetY = player.getRandom().nextDouble() * 3.0;
			double offsetZ = (player.getRandom().nextDouble() - 0.5) * 3.0;
			level.sendParticles(ParticleTypes.END_ROD, 
				x + offsetX, y + offsetY, z + offsetZ, 1, 0, 0, 0, 0.02);
		}
	}

	private LootboxManager.LootEntry pickWeighted(List<LootboxManager.LootEntry> entries) {
		double total = 0;
		for (LootboxManager.LootEntry e : entries) total += Math.max(0.0, e.chance());
		if (total <= 0) return null;
		double r = this.player.getRandom().nextDouble() * total;
		double acc = 0;
		for (LootboxManager.LootEntry e : entries) {
			acc += Math.max(0.0, e.chance());
			if (r <= acc) return e;
		}
		return entries.get(entries.size() - 1);
	}

	private void announceReward() {
		if (player.level().isClientSide) return;
		if (finalReward.isEmpty()) return;
		String playerName = player.getName().getString();
		String itemName = finalReward.getHoverName().getString();
		String boxName = rarity.toUpperCase();
		net.minecraft.network.chat.MutableComponent msg = Component.literal("§6§l» §e§lLOOTBOX §6§l« §7O jogador §f" + playerName + " §7conseguiu o item ")
				.append(Component.literal(itemName).withStyle(ChatFormatting.GOLD))
				.append(Component.literal(" §7na Caixa §e" + boxName));
		msg.withStyle(s -> s.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_ITEM, new HoverEvent.ItemStackInfo(finalReward))));
		var server = player.level().getServer();
		if (server != null) {
			server.getPlayerList().broadcastSystemMessage(msg, false);
		}
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