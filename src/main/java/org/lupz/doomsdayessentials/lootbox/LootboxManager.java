package org.lupz.doomsdayessentials.lootbox;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.loading.FMLPaths;
import org.lupz.doomsdayessentials.EssentialsMod;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public final class LootboxManager {
	private static final Path FILE_PATH = FMLPaths.CONFIGDIR.get().resolve("doomsdayessentials/lootboxes.json");
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

	public static final String R_INCOMUM = "incomum";
	public static final String R_RARA = "rara";
	public static final String R_EPICA = "epica";
	public static final String R_LENDARIA = "lendaria";
	public static final List<String> RARITIES = List.of(R_INCOMUM, R_RARA, R_EPICA, R_LENDARIA);

	private static final Map<String, List<LootEntry>> byRarity = new HashMap<>();
	private static final Random RNG = new Random();

	private LootboxManager() {}

	public static void load() {
		byRarity.clear();
		ensureFile();
		if (!Files.exists(FILE_PATH)) return;
		try (BufferedReader br = Files.newBufferedReader(FILE_PATH, StandardCharsets.UTF_8)) {
			Type type = new TypeToken<Map<String, List<LootEntry>>>(){}.getType();
			Map<String, List<LootEntry>> loaded = GSON.fromJson(br, type);
			if (loaded != null) {
				for (String r : RARITIES) {
					List<LootEntry> list = new ArrayList<>(loaded.getOrDefault(r, Collections.emptyList()));
					// Normalize default chance if missing/zero
					for (int i = 0; i < list.size(); i++) {
						LootEntry e = list.get(i);
						if (e.chance() <= 0.0) list.set(i, new LootEntry(e.snbt(), 1.0));
					}
					byRarity.put(r, list);
				}
			}
		} catch (IOException e) {
			EssentialsMod.LOGGER.error("Failed to read lootboxes.json", e);
		}
	}

	public static void save() {
		ensureFile();
		JsonObject root = new JsonObject();
		for (String r : RARITIES) {
			JsonArray arr = new JsonArray();
			for (LootEntry e : byRarity.getOrDefault(r, Collections.emptyList())) {
				JsonObject obj = new JsonObject();
				obj.addProperty("snbt", e.snbt());
				obj.addProperty("chance", e.chance());
				arr.add(obj);
			}
			root.add(r, arr);
		}
		try (BufferedWriter bw = Files.newBufferedWriter(FILE_PATH, StandardCharsets.UTF_8)) {
			bw.write(GSON.toJson(root));
		} catch (IOException e) {
			EssentialsMod.LOGGER.error("Failed to write lootboxes.json", e);
		}
	}

	private static void ensureFile() {
		try {
			Files.createDirectories(FILE_PATH.getParent());
			if (!Files.exists(FILE_PATH)) {
				JsonObject root = new JsonObject();
				for (String r : RARITIES) root.add(r, new JsonArray());
				Files.writeString(FILE_PATH, GSON.toJson(root), StandardCharsets.UTF_8);
			}
		} catch (IOException e) {
			EssentialsMod.LOGGER.error("Failed to ensure lootboxes.json path", e);
		}
	}

	public static boolean addItem(String rarity, ItemStack stack) {
		return addItem(rarity, stack, 1.0);
	}

	public static boolean addItem(String rarity, ItemStack stack, double chance) {
		if (!RARITIES.contains(rarity)) return false;
		ItemStack copy = stack.copy();
		copy.setCount(1);
		CompoundTag tag = new CompoundTag();
		copy.save(tag);
		String snbt = tag.toString();
		double normalized = chance <= 0 ? 1.0 : chance;
		byRarity.computeIfAbsent(rarity, k -> new ArrayList<>()).add(new LootEntry(snbt, normalized));
		save();
		return true;
	}

	public static List<ItemStack> getAllAsStacks(String rarity) {
		List<ItemStack> out = new ArrayList<>();
		for (LootEntry e : byRarity.getOrDefault(rarity, Collections.emptyList())) {
			ItemStack s = e.toStack();
			if (!s.isEmpty()) out.add(s);
		}
		return out;
	}

	public static List<ItemStack> getRandomSample(String rarity, int count) {
		List<LootEntry> src = byRarity.getOrDefault(rarity, Collections.emptyList());
		if (src.isEmpty()) return Collections.emptyList();
		List<ItemStack> list = new ArrayList<>();
		for (int i = 0; i < count; i++) {
			LootEntry picked = pickWeighted(src);
			ItemStack s = picked != null ? picked.toStack().copy() : ItemStack.EMPTY;
			list.add(s.isEmpty() ? ItemStack.EMPTY : s);
		}
		return list;
	}

	public static List<LootEntry> getEntries(String rarity) {
		List<LootEntry> list = byRarity.getOrDefault(rarity, Collections.emptyList());
		return java.util.Collections.unmodifiableList(list);
	}

	private static LootEntry pickWeighted(List<LootEntry> entries) {
		double total = 0;
		for (LootEntry e : entries) total += Math.max(0.0, e.chance());
		if (total <= 0) return null;
		double r = RNG.nextDouble() * total;
		double acc = 0;
		for (LootEntry e : entries) {
			acc += Math.max(0.0, e.chance());
			if (r <= acc) return e;
		}
		return entries.get(entries.size() - 1);
	}

	public record LootEntry(String snbt, double chance) {
		public ItemStack toStack() {
			try {
				CompoundTag tag = TagParser.parseTag(snbt);
				return ItemStack.of(tag);
			} catch (Exception e) {
				return ItemStack.EMPTY;
			}
		}
	}
} 