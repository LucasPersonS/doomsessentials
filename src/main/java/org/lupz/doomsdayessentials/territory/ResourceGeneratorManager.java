package org.lupz.doomsdayessentials.territory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
// Tick listener removed – production now timestamp based
import net.minecraftforge.registries.ForgeRegistries;
import org.lupz.doomsdayessentials.EssentialsMod;
import org.lupz.doomsdayessentials.territory.TerritoryAreaLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Loads generator definitions, tracks ownership and stored items, produces loot over time.
 */
public class ResourceGeneratorManager {

    private static final ResourceGeneratorManager INSTANCE = new ResourceGeneratorManager();
    public static ResourceGeneratorManager get() { return INSTANCE; }

    private final Map<String, ResourceAreaData> generators = new HashMap<>(); // areaName->data
    private final Path saveFile;
    private final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private ResourceGeneratorManager() {
        Path cfgDir = Path.of("config", "doomsdayessentials");
        this.saveFile = cfgDir.resolve("resource_generators.json");
        if (!Files.exists(cfgDir)) {
            try { Files.createDirectories(cfgDir); } catch (IOException e) { e.printStackTrace(); }
        }
        TerritoryAreaLoader.load();
        load();
        MinecraftForge.EVENT_BUS.register(this);
    }

    // ---------------------------------------------------------
    // Public API
    // ---------------------------------------------------------
    public ResourceAreaData get(String areaName) {
        ResourceAreaData d = generators.get(areaName.toLowerCase());
        if (d != null) accrue(d);
        return d;
    }

    public ResourceAreaData createIfAbsent(String areaName) {
        return generators.computeIfAbsent(areaName.toLowerCase(), k -> new ResourceAreaData(areaName, new java.util.ArrayList<>(), 64));
    }

    public String getOwner(String areaName) {
        ResourceAreaData d = generators.get(areaName);
        return d != null ? d.ownerGuild : null;
    }

    public boolean isAreaOwned(String areaName) {
        return getOwner(areaName) != null;
    }

    public void claimArea(String areaName, String guild) {
        ResourceAreaData data = generators.get(areaName.toLowerCase());
        if (data == null) {
            EssentialsMod.LOGGER.info("Creating new generator entry for area {} on claim", areaName);
            data = new ResourceAreaData(areaName, new java.util.ArrayList<>(), 400);
            generators.put(areaName.toLowerCase(), data);
        }
        data.ownerGuild = guild;
        data.claimTimestamp = System.currentTimeMillis();
        data.lastTimestamp = System.currentTimeMillis();
        save();
    }

    public void unclaimArea(String areaName) {
        ResourceAreaData d = generators.get(areaName);
        if (d != null) {
            d.ownerGuild = null;
            for (var e : d.lootEntries) e.stored = 0;
            save();
        }
    }

    /** Collects all stored items for the guild and returns total stacks given. */
    public int collectForGuild(ServerPlayer player, String guildName) {
        int given = 0;
        for (ResourceAreaData d : generators.values()) {
            int totalStored = d.lootEntries.stream().mapToInt(e->e.stored).sum();
            if (!guildName.equals(d.ownerGuild) || totalStored == 0) continue;
            accrue(d);
            for (ResourceAreaData.LootEntry entry : d.lootEntries) {
                if (entry.stored == 0) continue;
                var itemReg = ForgeRegistries.ITEMS.getValue(ResourceLocation.tryParse(entry.id));
                if (itemReg == null || itemReg == net.minecraft.world.level.block.Blocks.AIR.asItem()) {
                    EssentialsMod.LOGGER.error("Invalid loot item id {} for generator {}", entry.id, d.areaName);
                    continue;
                }
                int initial = entry.stored;
                int remaining = initial;
                while (remaining > 0) {
                    int stackSize = Math.min(itemReg.getMaxStackSize(), remaining);
                    ItemStack stack = new ItemStack(itemReg, stackSize);
                    if (!player.getInventory().add(stack)) {
                        player.drop(stack, false);
                    }
                    remaining -= stackSize;
                }
                given += initial;
                entry.stored = 0;
            }
            d.lastTimestamp = System.currentTimeMillis();
        }
        if (given > 0) save();
        return given;
    }

    // ---------------------------------------------------------
    // Production helper – accrues items based on timestamps
    // ---------------------------------------------------------
    private void accrue(ResourceAreaData d) {
        if (d.ownerGuild == null) return;
        long now = System.currentTimeMillis();
        // Expire after 48h uncollected
        if (d.claimTimestamp > 0 && now - d.claimTimestamp > 172_800_000L) {
            org.lupz.doomsdayessentials.combat.AreaManager.get().deleteArea(d.areaName);
            generators.remove(d.areaName.toLowerCase());
            save();
            EssentialsMod.LOGGER.info("Generator area {} expired and was removed", d.areaName);
            return;
        }
        double hours = (now - d.lastTimestamp) / 3_600_000.0;
        if (hours <= 0) return;
        boolean dirty = false;
        for (ResourceAreaData.LootEntry entry : d.lootEntries) {
            if (entry.stored >= d.storageCap) continue;
            int produced = (int) Math.floor(hours * entry.perHour);
            if (produced <= 0) continue;
            entry.stored = Math.min(d.storageCap, entry.stored + produced);
            dirty = true;
        }
        if (dirty) {
            d.lastTimestamp = now;
            save();
        }
    }

    // ---------------------------------------------------------
    // Tick production
    // ---------------------------------------------------------
    // Tick listener removed – production now timestamp based

    // ---------------------------------------------------------
    // Persistence helpers
    // ---------------------------------------------------------
    public void reload() {
        loadInternal();
    }

    private void loadInternal() {
        generators.clear();
        if (!Files.exists(saveFile)) {
            EssentialsMod.LOGGER.warn("resource_generators.json not found, creating default empty file.");
            save();
            return;
        }
        try {
            String jsonString = Files.readString(saveFile);
            if (jsonString.isEmpty()) return;
            JsonObject root = JsonParser.parseString(jsonString).getAsJsonObject();
            for (Map.Entry<String, JsonElement> entry : root.entrySet()) {
                if (!entry.getValue().isJsonObject()) continue;
                ResourceAreaData d = ResourceAreaData.fromJson(entry.getKey(), entry.getValue().getAsJsonObject());
                generators.put(entry.getKey().toLowerCase(), d);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void save() {
        JsonObject root = new JsonObject();
        for (ResourceAreaData d : generators.values()) {
            root.add(d.areaName, d.toJson());
        }
        try {
            Files.writeString(saveFile, GSON.toJson(root));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void deleteGenerator(String areaName) {
        if (generators.remove(areaName.toLowerCase()) != null) {
            save();
        }
    }

    // For config packers
    public List<ResourceAreaData> getGeneratorsForGuild(String guildName) {
        java.util.List<ResourceAreaData> list = new java.util.ArrayList<>();
        for (ResourceAreaData d : generators.values()) {
            if (guildName.equals(d.ownerGuild)) {
                accrue(d);
                list.add(d);
            }
        }
        return list;
    }

    private void load() {
        loadInternal();
    }
} 