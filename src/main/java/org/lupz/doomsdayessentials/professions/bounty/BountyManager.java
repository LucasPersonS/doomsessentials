package org.lupz.doomsdayessentials.professions.bounty;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.lupz.doomsdayessentials.item.ModItems;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class BountyManager {
	private BountyManager() {}

	public static class Bounty {
		public final UUID target;
		public final Item rewardItem;
		public int amount;
		public final UUID placedBy;
		public final long createdAtMs;
		public Bounty(UUID target, Item rewardItem, int amount, UUID placedBy, long createdAtMs) {
			this.target = target; this.rewardItem = rewardItem; this.amount = amount; this.placedBy = placedBy; this.createdAtMs = createdAtMs;
		}
	}

	private static final Map<UUID, List<Bounty>> active = new ConcurrentHashMap<>();
	private static final Map<UUID, AcceptedHunt> acceptedByHunter = new ConcurrentHashMap<>();

	private record AcceptedHunt(UUID hunter, UUID target, long acceptedAtMs, long expiresAtMs) {}

	private static long nowMs() {
		// Real-world wall clock
		return java.time.Instant.now().toEpochMilli();
	}

	private static boolean isBountyExpired(Bounty b) {
		long ageMs = nowMs() - b.createdAtMs;
		return ageMs > Duration.ofDays(7).toMillis();
	}

	private static void cleanupExpiredBountiesFor(UUID targetId) {
		List<Bounty> list = active.get(targetId);
		if (list == null) return;
		list.removeIf(BountyManager::isBountyExpired);
		if (list.isEmpty()) active.remove(targetId);
	}

	private static void notifyTargetCleared(UUID target) {
		ServerPlayer targetPlayer = java.util.Optional.ofNullable(net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer())
			.map(s -> s.getPlayerList().getPlayer(target))
			.orElse(null);
		if (targetPlayer != null) {
			org.lupz.doomsdayessentials.network.PacketHandler.CHANNEL.sendTo(
					new org.lupz.doomsdayessentials.professions.bounty.HuntedStatePacket(false, 0L),
					targetPlayer.connection.connection,
					net.minecraftforge.network.NetworkDirection.PLAY_TO_CLIENT);
		}
	}

	public static synchronized boolean placeBounty(ServerPlayer placer, UUID target, Item rewardItem, int amount) {
		if (amount <= 0 || rewardItem == null) return false;
		int removed = removeItems(placer, rewardItem, amount);
		if (removed < amount) {
			// refund partial
			if (removed > 0) addItems(placer, rewardItem, removed);
			return false;
		}
		cleanupExpiredBountiesFor(target);
		List<Bounty> list = active.computeIfAbsent(target, k -> new ArrayList<>());
		for (Bounty b : list) {
			if (b.rewardItem == rewardItem && !isBountyExpired(b)) { b.amount += amount; return true; }
		}
		list.add(new Bounty(target, rewardItem, amount, placer.getUUID(), nowMs()));
		return true;
	}

	public static Optional<Bounty> getBounty(UUID target) {
		cleanupExpiredBountiesFor(target);
		List<Bounty> list = active.get(target);
		if (list == null || list.isEmpty()) return Optional.empty();
		// Return the first; kept for compatibility
		return Optional.of(list.get(0));
	}

	public static synchronized void onPlayerKilled(ServerPlayer killer, ServerPlayer victim) {
		// Only hunters who accepted this target and are within the 1-day validity can claim
		var acceptance = acceptedByHunter.get(killer.getUUID());
		long now = nowMs();
		if (acceptance == null || !acceptance.target.equals(victim.getUUID()) || now > acceptance.expiresAtMs) {
			return;
		}
		// Payout and cleanup acceptance and bounties (filter out expired bounties before paying)
		cleanupExpiredBountiesFor(victim.getUUID());
		List<Bounty> list = active.remove(victim.getUUID());
		if (list == null || list.isEmpty()) {
			acceptedByHunter.remove(killer.getUUID());
			return;
		}
		for (Bounty b : list) {
			if (!isBountyExpired(b)) addItems(killer, b.rewardItem, b.amount);
		}
		acceptedByHunter.remove(killer.getUUID());
		// Remove any other hunters' acceptance for this target as the bounty is fulfilled
		acceptedByHunter.entrySet().removeIf(en -> en.getValue().target().equals(victim.getUUID()));
		// Notify victim client to clear hunted HUD (optional; client also auto-expires)
		notifyTargetCleared(victim.getUUID());
	}

	public static List<Bounty> listAll() {
		List<Bounty> out = new ArrayList<>();
		for (Map.Entry<UUID, List<Bounty>> en : active.entrySet()) {
			cleanupExpiredBountiesFor(en.getKey());
			List<Bounty> list = active.get(en.getKey());
			if (list != null) {
				for (Bounty b : list) if (!isBountyExpired(b)) out.add(b);
			}
		}
		return out;
	}

	public static synchronized boolean acceptBounty(ServerPlayer hunter, UUID target) {
		// Must be a bounty hunter
		if (!org.lupz.doomsdayessentials.professions.CacadorProfession.isHunter(hunter)) {
			hunter.sendSystemMessage(net.minecraft.network.chat.Component.literal("§cApenas Caçadores de Recompensa podem aceitar caçadas."));
			return false;
		}
		cleanupExpiredBountiesFor(target);
		List<Bounty> list = active.get(target);
		if (list == null || list.isEmpty()) {
			hunter.sendSystemMessage(net.minecraft.network.chat.Component.literal("§cNão há recompensas válidas para este alvo."));
			return false;
		}
		// Register acceptance for 1 real day
		long now = nowMs();
		long expiresAt = now + Duration.ofDays(1).toMillis();
		acceptedByHunter.put(hunter.getUUID(), new AcceptedHunt(hunter.getUUID(), target, now, expiresAt));
		hunter.sendSystemMessage(net.minecraft.network.chat.Component.literal("§aVocê aceitou a caçada."));
		// Notify target client to show hunted HUD until the latest expiry across multiple hunters
		ServerPlayer targetPlayer = hunter.server.getPlayerList().getPlayer(target);
		if (targetPlayer != null) {
			org.lupz.doomsdayessentials.network.PacketHandler.CHANNEL.sendTo(
					new org.lupz.doomsdayessentials.professions.bounty.HuntedStatePacket(true, expiresAt),
					targetPlayer.connection.connection,
					net.minecraftforge.network.NetworkDirection.PLAY_TO_CLIENT);
		}
		return true;
	}

	// ---------------------------------------------------------------------
	// Admin utilities
	// ---------------------------------------------------------------------
	public static synchronized void clearAllBounties() {
		active.clear();
		// collect targets with accepted hunts to notify and then clear acceptances
		java.util.Set<UUID> impactedTargets = new java.util.HashSet<>();
		for (AcceptedHunt a : acceptedByHunter.values()) impactedTargets.add(a.target);
		acceptedByHunter.clear();
		for (UUID t : impactedTargets) notifyTargetCleared(t);
	}

	public static synchronized boolean removeBountiesForTarget(UUID target) {
		List<Bounty> removed = active.remove(target);
		boolean had = removed != null && !removed.isEmpty();
		if (had) {
			// also clear acceptances for this target and notify
			clearAcceptanceForTarget(target);
		}
		return had;
	}

	public static synchronized int cleanupExpiredBounties() {
		int removed = 0;
		for (UUID target : new ArrayList<>(active.keySet())) {
			List<Bounty> list = active.get(target);
			if (list == null) continue;
			int before = list.size();
			cleanupExpiredBountiesFor(target);
			List<Bounty> afterList = active.get(target);
			int after = afterList == null ? 0 : afterList.size();
			removed += Math.max(0, before - after);
			if (after == 0) {
				// if no more bounties for this target, also clear acceptances and notify
				clearAcceptanceForTarget(target);
			}
		}
		return removed;
	}

	public static synchronized boolean clearAcceptanceForHunter(UUID hunter) {
		AcceptedHunt removed = acceptedByHunter.remove(hunter);
		if (removed != null) {
			// if no other hunter still has this target accepted, notify target to clear HUD
			boolean anyLeft = acceptedByHunter.values().stream().anyMatch(a -> a.target.equals(removed.target));
			if (!anyLeft) notifyTargetCleared(removed.target);
			return true;
		}
		return false;
	}

	public static synchronized int clearAcceptanceForTarget(UUID target) {
		int count = 0;
		Iterator<Map.Entry<UUID, AcceptedHunt>> it = acceptedByHunter.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<UUID, AcceptedHunt> en = it.next();
			if (en.getValue().target.equals(target)) { it.remove(); count++; }
		}
		if (count > 0) notifyTargetCleared(target);
		return count;
	}

	private static int removeItems(ServerPlayer player, Item item, int amount) {
		int toRemove = amount;
		for (int i = 0; i < player.getInventory().getContainerSize() && toRemove > 0; i++) {
			ItemStack s = player.getInventory().getItem(i);
			if (s.is(item)) {
				int take = Math.min(s.getCount(), toRemove);
				s.shrink(take);
				toRemove -= take;
			}
		}
		return amount - toRemove;
	}

	private static void addItems(ServerPlayer player, Item item, int amount) {
		while (amount > 0) {
			int give = Math.min(64, amount);
			player.getInventory().placeItemBackInInventory(new ItemStack(item, give));
			amount -= give;
		}
	}
} 