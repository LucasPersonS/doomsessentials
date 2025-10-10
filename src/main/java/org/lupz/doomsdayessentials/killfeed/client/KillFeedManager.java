package org.lupz.doomsdayessentials.killfeed.client;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.registries.ForgeRegistries;
import org.lupz.doomsdayessentials.sound.ModSounds;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;

public class KillFeedManager {
	public static final long ENTRY_DURATION_MS = 10000L;
	public static final int MAX_ENTRIES = 10;
	private static final ConcurrentLinkedQueue<KillFeedEntry> ENTRIES = new ConcurrentLinkedQueue<>();

	// Killcard
	public static final long KILLCARD_DURATION_MS = 2500L;
	private static volatile KillCard ACTIVE_KILLCARD = null;
	// Local player kill streak index: cycles 1..5 then wraps back to 1
	private static int LOCAL_KILL_STREAK_INDEX = 0;
	private static long LAST_LOCAL_KILL_MS = 0L;
	private static final long KILL_STREAK_TIMEOUT_MS = 5000L; // 5 seconds window to chain kills

	public static void addEntry(UUID killerUUID, String killer, UUID victimUUID, String victim, String weaponName, String weaponId, String entityTypeId) {
		ItemStack weapon = new ItemStack(ForgeRegistries.ITEMS.getValue(ResourceLocation.parse(weaponId)));
		if (weapon.getItem() == Items.AIR && !"minecraft:air".equals(weaponId)) {
			 // This could be a custom weapon not in the registry, handle as needed
		}

		long creationTime = System.currentTimeMillis();
		KillFeedEntry entry = new KillFeedEntry(killerUUID, killer, victimUUID, victim, weaponName, weaponId, entityTypeId, creationTime, weapon);
		ENTRIES.add(entry);

		if (ENTRIES.size() > MAX_ENTRIES) {
			ENTRIES.poll();
		}

		// If local player is the killer, trigger killcard + sound
		try {
			var mc = net.minecraft.client.Minecraft.getInstance();
			if (mc.player != null && killerUUID != null && mc.player.getUUID().equals(killerUUID)) {
				ResourceLocation tex = KillcardAssets.getTextureForWeapon(weaponId);
				// Reset streak on timeout before incrementing
				if (creationTime - LAST_LOCAL_KILL_MS > KILL_STREAK_TIMEOUT_MS) {
					LOCAL_KILL_STREAK_INDEX = 0;
				}
				// Cycle streak index 1..5 and wrap
				LOCAL_KILL_STREAK_INDEX = (LOCAL_KILL_STREAK_INDEX % 5) + 1;
				LAST_LOCAL_KILL_MS = creationTime;
				ACTIVE_KILLCARD = new KillCard(creationTime, killer, victim, weaponId, tex, LOCAL_KILL_STREAK_INDEX);
				float pitch = Math.min(2.0f, 1.0f + (LOCAL_KILL_STREAK_INDEX - 1) * 0.08f);
				mc.player.playSound(getStreakSoundEventForWeapon(weaponId, LOCAL_KILL_STREAK_INDEX), 1.0f, pitch);
			}
		} catch (Throwable ignored) {}
	}

	public static void resetLocalKillStreak() {
		LOCAL_KILL_STREAK_INDEX = 0;
		LAST_LOCAL_KILL_MS = 0L;
	}

	private static net.minecraft.sounds.SoundEvent getStreakSoundEventForWeapon(String weaponId, int index) {
		try {
			if (weaponId != null) {
				String idLower = weaponId.toLowerCase();
				if (idLower.contains("kuronami")) {
					ResourceLocation rl;
					switch (index) {
						case 1 -> rl = ResourceLocation.fromNamespaceAndPath("doomsdayessentials", "kuronamiks");
						case 2 -> rl = ResourceLocation.fromNamespaceAndPath("doomsdayessentials", "kuronami2");
						case 3 -> rl = ResourceLocation.fromNamespaceAndPath("doomsdayessentials", "kuronami3");
						case 4 -> rl = ResourceLocation.fromNamespaceAndPath("doomsdayessentials", "kuronami4");
						case 5 -> rl = ResourceLocation.fromNamespaceAndPath("doomsdayessentials", "kuronami5");
						default -> rl = ResourceLocation.fromNamespaceAndPath("doomsdayessentials", "kuronamiks");
					}
					net.minecraft.sounds.SoundEvent se = ForgeRegistries.SOUND_EVENTS.getValue(rl);
					if (se != null) return se;
				}
			}
		} catch (Exception ignored) {}
		switch (index) {
			case 1: return ModSounds.KILL_STREAK_1.get();
			case 2: return ModSounds.KILL_STREAK_2.get();
			case 3: return ModSounds.KILL_STREAK_3.get();
			case 4: return ModSounds.KILL_STREAK_4.get();
			case 5: return ModSounds.KILL_STREAK_5.get();
			default: return ModSounds.KILL_STREAK_1.get();
		}
	}

	public static List<KillFeedManager.KillFeedEntry> getEntries() {
		long now = System.currentTimeMillis();
		ENTRIES.removeIf(entry -> now - entry.creationTime() > ENTRY_DURATION_MS);
		return List.copyOf(ENTRIES);
	}

	public static KillCard getActiveKillcard() {
		KillCard card = ACTIVE_KILLCARD;
		if (card == null) return null;
		if (System.currentTimeMillis() - card.creationTime > KILLCARD_DURATION_MS) {
			ACTIVE_KILLCARD = null;
			return null;
		}
		return card;
	}

	public record KillFeedEntry(
			UUID killerUUID,
			String killer,
			UUID victimUUID,
			String victim,
			String weaponName,
			String weaponId,
			String entityTypeId,
			long creationTime,
			ItemStack weapon
	) {}

	public record KillCard(
			long creationTime,
			String killer,
			String victim,
			String weaponId,
			ResourceLocation texture,
			int streakIndex
	) {}
} 