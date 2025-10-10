package org.lupz.doomsdayessentials.killfeed.client;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.ForgeRegistries;
import org.lupz.doomsdayessentials.EssentialsMod;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Data-driven mapping from weapon ids (e.g., "tacz:ak47") to killcard texture and sound.
 * JSON file location: assets/doomsdayessentials/killcards/killcards.json
 *
 * {
 *   "defaults": {
 *     "texture": "doomsdayessentials:textures/gui/killcards/default.png",
 *     "sound": "doomsdayessentials:kill_notify"
 *   },
 *   "weapons": {
 *     "tacz:ak47": {
 *       "texture": "doomsdayessentials:textures/gui/killcards/tacz/ak47.png",
 *       "sound": "doomsdayessentials:kill_ak47"
 *     }
 *   }
 * }
 */
public final class KillcardAssets {
	private KillcardAssets() {}

	private static final Gson GSON = new Gson();
	private static final ResourceLocation CONFIG_RL = ResourceLocation.fromNamespaceAndPath(EssentialsMod.MOD_ID, "killcards/killcards.json");

	private static boolean loaded;
	private static ResourceLocation defaultTexture = ResourceLocation.fromNamespaceAndPath(EssentialsMod.MOD_ID, "textures/gui/killcards/default_kill.png");
	private static ResourceLocation defaultSound = ResourceLocation.fromNamespaceAndPath(EssentialsMod.MOD_ID, "kill_notify");
	private static final Map<String, Entry> weaponIdToEntry = new HashMap<>();

	public static ResourceLocation getTextureForWeapon(String weaponId) {
		ensureLoaded();
		Entry e = weaponIdToEntry.get(weaponId);
		return e != null && e.texture != null ? e.texture : defaultTexture;
	}

	public static SoundEvent getSoundForWeapon(String weaponId) {
		ensureLoaded();
		Entry e = weaponIdToEntry.get(weaponId);
		ResourceLocation rl = (e != null && e.sound != null) ? e.sound : defaultSound;
		
		// Debug log
		System.out.println("KillcardAssets: Looking for weaponId: " + weaponId);
		System.out.println("KillcardAssets: Found entry: " + (e != null));
		if (e != null) {
			System.out.println("KillcardAssets: Entry texture: " + e.texture);
			System.out.println("KillcardAssets: Entry sound: " + e.sound);
		}
		System.out.println("KillcardAssets: Using sound RL: " + rl);
		
		SoundEvent ev = ForgeRegistries.SOUND_EVENTS.getValue(rl);
		if (ev == null) {
			// Fallback if not registered
			System.out.println("KillcardAssets: Sound not found in registry, using fallback");
			return org.lupz.doomsdayessentials.sound.ModSounds.KILL_NOTIFY.get();
		}
		System.out.println("KillcardAssets: Found sound event: " + ev);
		return ev;
	}

	public static void reload() {
		loaded = false;
		weaponIdToEntry.clear();
		ensureLoaded();
	}

	private static void ensureLoaded() {
		if (loaded) return;
		loaded = true;
		try {
			var rm = Minecraft.getInstance().getResourceManager();
			var opt = rm.getResource(CONFIG_RL);
			if (opt.isEmpty()) return;
			try (var is = opt.get().open()) {
				BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
				JsonObject root = GSON.fromJson(br, JsonObject.class);
				JsonObject defaults = root.has("defaults") && root.get("defaults").isJsonObject() ? root.getAsJsonObject("defaults") : null;
				if (defaults != null) {
					if (defaults.has("texture")) {
						defaultTexture = rlOr(defaults.get("texture"), defaultTexture);
					}
					if (defaults.has("sound")) {
						defaultSound = rlOr(defaults.get("sound"), defaultSound);
					}
				}
				JsonObject weapons = root.has("weapons") && root.get("weapons").isJsonObject() ? root.getAsJsonObject("weapons") : null;
				if (weapons != null) {
					for (var entry : weapons.entrySet()) {
						String id = entry.getKey();
						JsonObject obj = entry.getValue().getAsJsonObject();
						ResourceLocation tex = obj.has("texture") ? rlOr(obj.get("texture"), null) : null;
						ResourceLocation snd = obj.has("sound") ? rlOr(obj.get("sound"), null) : null;
						weaponIdToEntry.put(id, new Entry(tex, snd));
					}
				}
			}
		} catch (Exception ignored) {}
	}

	private static ResourceLocation rlOr(JsonElement el, ResourceLocation fallback) {
		try {
			String s = el.getAsString();
			return ResourceLocation.parse(s);
		} catch (Exception e) {
			return fallback;
		}
	}

	private record Entry(ResourceLocation texture, ResourceLocation sound) {}
} 