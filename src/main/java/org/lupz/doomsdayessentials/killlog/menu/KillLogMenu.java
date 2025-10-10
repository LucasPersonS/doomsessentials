package org.lupz.doomsdayessentials.killlog.menu;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import org.lupz.doomsdayessentials.killlog.KillLogMenus;

import java.util.ArrayList;
import java.util.List;

public class KillLogMenu extends AbstractContainerMenu {

	public static class Entry {
		public final String timestampIso;
		public final double x;
		public final double y;
		public final double z;
		public final String causeId;
		public final String killerName;
		public final String weaponDisplay;
		public final String areaName;
		public final String areaType;
		public final List<ItemStack> items;
		public final List<String> lastHits;
		public Entry(String timestampIso, List<ItemStack> items, double x, double y, double z, String causeId, String killerName, String weaponDisplay, String areaName, String areaType, List<String> lastHits) {
			this.timestampIso = timestampIso;
			this.items = items;
			this.x = x; this.y = y; this.z = z;
			this.causeId = causeId;
			this.killerName = killerName;
			this.weaponDisplay = weaponDisplay;
			this.areaName = areaName;
			this.areaType = areaType;
			this.lastHits = lastHits;
		}
	}

	private final List<Entry> entries;
	private String targetUuid = "";

	public KillLogMenu(int windowId, Inventory inv) {
		super(KillLogMenus.KILLLOG_MENU.get(), windowId);
		this.entries = new ArrayList<>();
	}

	public KillLogMenu(int windowId, Inventory inv, FriendlyByteBuf data) {
		super(KillLogMenus.KILLLOG_MENU.get(), windowId);
		this.entries = new ArrayList<>();
		if (data != null && data.isReadable()) {
			this.targetUuid = data.readUtf();
			int e = data.readVarInt();
			for (int i = 0; i < e; i++) {
				String ts = data.readUtf();
				double x = data.readDouble();
				double y = data.readDouble();
				double z = data.readDouble();
				String causeId = data.readUtf();
				String killerName = data.readUtf();
				String weaponDisplay = data.readUtf();
				String areaName = data.readUtf();
				String areaType = data.readUtf();
				int size = data.readVarInt();
				List<ItemStack> items = new ArrayList<>(size);
				for (int j = 0; j < size; j++) items.add(data.readItem());
				int hits = data.readVarInt();
				List<String> lastHits = new ArrayList<>(hits);
				for (int h = 0; h < hits; h++) lastHits.add(data.readUtf());
				this.entries.add(new Entry(ts, items, x, y, z, causeId, killerName, weaponDisplay, areaName, areaType, lastHits));
			}
		}
	}

	public List<Entry> getEntries() { return entries; }

	public String getTargetUuid() { return targetUuid; }

	@Override public boolean stillValid(Player player) { return true; }

	@Override public ItemStack quickMoveStack(Player player, int index) { return ItemStack.EMPTY; }
} 