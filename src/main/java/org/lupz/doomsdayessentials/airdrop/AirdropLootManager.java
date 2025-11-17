package org.lupz.doomsdayessentials.airdrop;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraftforge.fml.loading.FMLPaths;
import org.lupz.doomsdayessentials.EssentialsMod;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.HashSet;

/**
 * Loads airdrop loot configuration from JSON and populates the airdrop container using loot tables.
 * Supports vanilla, datapacks, and other mods via resource locations.
 */
public final class AirdropLootManager {
    private static final Path FILE_PATH = FMLPaths.CONFIGDIR.get().resolve("doomsdayessentials/airdrop_loot.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static final List<Entry> ENTRIES = new ArrayList<>();

    private AirdropLootManager() {}

    public static void load() {
        ensureFile();
        ENTRIES.clear();
        if (!Files.exists(FILE_PATH)) return;
        try (BufferedReader br = Files.newBufferedReader(FILE_PATH, StandardCharsets.UTF_8)) {
            JsonElement je = com.google.gson.JsonParser.parseReader(br);
            if (je != null && je.isJsonObject()) {
                JsonObject root = je.getAsJsonObject();
                JsonArray arr = root.has("tables") && root.get("tables").isJsonArray() ? root.getAsJsonArray("tables") : new JsonArray();
                for (JsonElement e : arr) {
                    if (!e.isJsonObject()) continue;
                    JsonObject obj = e.getAsJsonObject();
                    String loc = obj.has("loot_table") ? obj.get("loot_table").getAsString() : "";
                    int rolls = obj.has("rolls") ? Math.max(1, obj.get("rolls").getAsInt()) : 1;
                    double weight = obj.has("weight") ? Math.max(0.0, obj.get("weight").getAsDouble()) : 1.0;
                    if (!loc.isEmpty()) ENTRIES.add(new Entry(loc, rolls, weight));
                }
            }
        } catch (Exception e) {
            EssentialsMod.LOGGER.error("Failed to read airdrop_loot.json", e);
        }
    }

    public static void ensureFile() {
        try {
            Files.createDirectories(FILE_PATH.getParent());
            if (!Files.exists(FILE_PATH)) {
                JsonObject root = new JsonObject();
                JsonArray arr = new JsonArray();
                JsonObject ex = new JsonObject();
                ex.addProperty("loot_table", "minecraft:chests/simple_dungeon");
                ex.addProperty("rolls", 1);
                ex.addProperty("weight", 1.0);
                arr.add(ex);
                root.add("tables", arr);
                try (BufferedWriter bw = Files.newBufferedWriter(FILE_PATH, StandardCharsets.UTF_8)) {
                    bw.write(GSON.toJson(root));
                }
            }
        } catch (Exception e) {
            EssentialsMod.LOGGER.error("Failed to ensure airdrop_loot.json path", e);
        }
    }

    public static void populate(SimpleContainer cont, ServerLevel level, ServerPlayer opener, net.minecraft.world.phys.Vec3 origin) {
        if (ENTRIES.isEmpty()) load();
        RandomSource rng = opener.getRandom();
        int slot = 0;
        for (Entry entry : ENTRIES) {
            ResourceLocation rl = ResourceLocation.tryParse(entry.lootTable);
            if (rl == null) continue;
            LootTable table = level.getServer().getLootData().getLootTable(rl);
            if (table == null) continue;
            LootParams.Builder builder = new LootParams.Builder(level)
                    .withParameter(LootContextParams.ORIGIN, origin)
                    .withLuck(opener.getLuck());
            LootParams params = builder.create(LootContextParamSets.CHEST);
            for (int i = 0; i < entry.rolls; i++) {
                List<ItemStack> items = table.getRandomItems(params);
                for (ItemStack s : items) {
                    if (s.isEmpty()) continue;
                    // Place items sequentially; if full, stop
                    while (slot < cont.getContainerSize() && !cont.getItem(slot).isEmpty()) slot++;
                    if (slot >= cont.getContainerSize()) return;
                    cont.setItem(slot, s.copy());
                }
            }
        }
    }

    private record Entry(String lootTable, int rolls, double weight) {}

    public static List<ResourceLocation> getLootTables() {
        if (ENTRIES.isEmpty()) load();
        return ENTRIES.stream().map(e -> ResourceLocation.parse(e.lootTable)).toList();
    }

    public static boolean addLootTable(ResourceLocation lootTable) {
        String lootTableStr = lootTable.toString();
        if (ENTRIES.stream().anyMatch(e -> e.lootTable.equals(lootTableStr))) {
            return false; // Already exists
        }
        // Default values for rolls and weight
        ENTRIES.add(new Entry(lootTableStr, 3, 1.0));
        save();
        return true;
    }

    public static boolean removeLootTable(ResourceLocation lootTable) {
        String lootTableStr = lootTable.toString();
        boolean removed = ENTRIES.removeIf(e -> e.lootTable.equals(lootTableStr));
        if (removed) {
            save();
        }
        return removed;
    }

    public static void reloadLootTables() {
        ENTRIES.clear();
        load();
    }

    public static void clearLootTables() {
        ENTRIES.clear();
        save();
    }

    private static void save() {
        try {
            Files.createDirectories(FILE_PATH.getParent());
            JsonObject root = new JsonObject();
            JsonArray arr = new JsonArray();
            for (Entry entry : ENTRIES) {
                JsonObject obj = new JsonObject();
                obj.addProperty("loot_table", entry.lootTable);
                obj.addProperty("rolls", entry.rolls);
                obj.addProperty("weight", entry.weight);
                arr.add(obj);
            }
            root.add("tables", arr);
            try (BufferedWriter bw = Files.newBufferedWriter(FILE_PATH, StandardCharsets.UTF_8)) {
                bw.write(GSON.toJson(root));
            }
        } catch (Exception e) {
            EssentialsMod.LOGGER.error("Failed to save airdrop_loot.json", e);
        }
    }
}
