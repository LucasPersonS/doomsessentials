package org.lupz.doomsdayessentials.professions.bounty;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import org.lupz.doomsdayessentials.item.ModItems;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class BountyManager {
	private BountyManager() {}

	public static class Bounty {
		public final UUID target;
		public int gears;
		public final UUID placedBy;
		public final long createdAt;
		public Bounty(UUID target, int gears, UUID placedBy, long createdAt) {
			this.target = target; this.gears = gears; this.placedBy = placedBy; this.createdAt = createdAt;
		}
	}

	private static final Map<UUID, Bounty> active = new ConcurrentHashMap<>();

	public static synchronized boolean placeBounty(ServerPlayer placer, UUID target, int gears) {
		if (gears <= 0) return false;
		int removed = removeGears(placer, gears);
		if (removed < gears) {
			// refund partial
			if (removed > 0) addGears(placer, removed);
			return false;
		}
		active.merge(target, new Bounty(target, gears, placer.getUUID(), placer.level().getGameTime()), (oldB, newB) -> { oldB.gears += newB.gears; return oldB; });
		return true;
	}

	public static Optional<Bounty> getBounty(UUID target) { return Optional.ofNullable(active.get(target)); }

	public static synchronized void onPlayerKilled(ServerPlayer killer, ServerPlayer victim) {
		Bounty b = active.remove(victim.getUUID());
		if (b == null) return;
		addGears(killer, b.gears);
	}

	public static List<Bounty> listAll() { return new ArrayList<>(active.values()); }

	private static int removeGears(ServerPlayer player, int amount) {
		int toRemove = amount;
		for (int i = 0; i < player.getInventory().getContainerSize() && toRemove > 0; i++) {
			ItemStack s = player.getInventory().getItem(i);
			if (s.is(ModItems.GEARS.get())) {
				int take = Math.min(s.getCount(), toRemove);
				s.shrink(take);
				toRemove -= take;
			}
		}
		return amount - toRemove;
	}

	private static void addGears(ServerPlayer player, int amount) {
		while (amount > 0) {
			int give = Math.min(64, amount);
			player.getInventory().placeItemBackInInventory(new ItemStack(ModItems.GEARS.get(), give));
			amount -= give;
		}
	}
} 