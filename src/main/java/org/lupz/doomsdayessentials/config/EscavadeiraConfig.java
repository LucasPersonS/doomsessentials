package org.lupz.doomsdayessentials.config;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.electronwill.nightconfig.core.io.WritingMode;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.loading.FMLPaths;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

public final class EscavadeiraConfig {
	public static final String MODID = "doomsdayessentials";
	private static final Path CONFIG_PATH = FMLPaths.CONFIGDIR.get().resolve("doomsdayessentials-escavadeira.toml");

	public static int fuelConsumptionTicks;
	public static int productionIntervalTicks;
	public static boolean attractHostileMobs;
	public static int soundIntervalTicks;
	public static List<ResourceEntry> resources;

	private static final Random RANDOM = new Random();

	static {
		load();
	}

	public static void load() {
		CommentedFileConfig cfg = CommentedFileConfig.builder(CONFIG_PATH)
			.sync()
			.preserveInsertionOrder()
			.writingMode(WritingMode.REPLACE)
			.build();
		cfg.load();

		fuelConsumptionTicks = cfg.getOrElse("fuelConsumptionTicks", 200);
		productionIntervalTicks = cfg.getOrElse("productionIntervalTicks", 100);
		attractHostileMobs = cfg.getOrElse("attractHostileMobs", false);
		soundIntervalTicks = cfg.getOrElse("soundIntervalTicks", 60);

		List<String> defaults = getDefaultResources();
		List<String> rawList = cfg.get("resources");
		if (rawList == null || rawList.isEmpty()) {
			cfg.set("resources", defaults);
			rawList = defaults;
		}
		resources = new ArrayList<>();
		for (String s : rawList) {
			ResourceEntry e = ResourceEntry.parse(s);
			if (e != null) resources.add(e);
		}

		cfg.save();
		cfg.close();
	}

	private static List<String> getDefaultResources() {
		List<String> list = new ArrayList<>();
		list.add("minecraft:iron_ore;min=1;max=3;chance=0.45");
		list.add("minecraft:copper_ore;min=2;max=5;chance=0.55");
		list.add("minecraft:gold_ore;min=1;max=2;chance=0.18");
		list.add("minecraft:coal;min=2;max=6;chance=0.40");
		list.add("minecraft:stone;min=4;max=12;chance=0.80");
		return list;
	}

	public static ResourceEntry pickRandomResource() {
		if (resources.isEmpty()) return null;
		double roll = RANDOM.nextDouble();
		double cumulative = 0.0;
		for (ResourceEntry e : resources) {
			cumulative += e.chance;
			if (roll <= cumulative) return e;
		}
		return resources.get(resources.size() - 1);
	}

	public static final class ResourceEntry {
		public final ResourceLocation itemId;
		public final int min;
		public final int max;
		public final double chance;

		public ResourceEntry(ResourceLocation itemId, int min, int max, double chance) {
			this.itemId = itemId;
			this.min = min;
			this.max = max;
			this.chance = chance;
		}

		public static ResourceEntry parse(String s) {
			try {
				String[] parts = s.split(";");
				ResourceLocation id = new ResourceLocation(parts[0].trim().toLowerCase(Locale.ROOT));
				int min = 1, max = 1; double chance = 1.0;
				for (int i = 1; i < parts.length; i++) {
					String p = parts[i];
					String[] kv = p.split("=");
					if (kv.length != 2) continue;
					String key = kv[0].trim();
					String val = kv[1].trim();
					switch (key) {
						case "min": min = Integer.parseInt(val); break;
						case "max": max = Integer.parseInt(val); break;
						case "chance": chance = Double.parseDouble(val); break;
					}
				}
				if (min < 1) min = 1;
				if (max < min) max = min;
				if (chance < 0) chance = 0; if (chance > 1) chance = 1;
				return new ResourceEntry(id, min, max, chance);
			} catch (Exception ex) {
				return null;
			}
		}
	}
} 