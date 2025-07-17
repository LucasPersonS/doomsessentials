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
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.lupz.doomsdayessentials.EssentialsMod;

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
        load();
        MinecraftForge.EVENT_BUS.register(this);
    }

    // ---------------------------------------------------------
    // Public API
    // ---------------------------------------------------------
    public ResourceAreaData get(String areaName) { return generators.get(areaName.toLowerCase()); }

    public ResourceAreaData createIfAbsent(String areaName) {
        return generators.computeIfAbsent(areaName.toLowerCase(), k -> new ResourceAreaData(areaName, "minecraft:stone", 1, 64));
    }

    public String getOwner(String areaName) {
        ResourceAreaData d = generators.get(areaName);
        return d != null ? d.ownerGuild : null;
    }

    public boolean isAreaOwned(String areaName) {
        return getOwner(areaName) != null;
    }

    public void claimArea(String areaName, String guild) {
        ResourceAreaData data = generators.get(areaName);
        if (data == null) {
            EssentialsMod.LOGGER.warn("Trying to claim unknown generator area {}", areaName);
            return;
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
    // Tick production
    // ---------------------------------------------------------
    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        // run every minute (1200 ticks)
        if (ServerLifecycleHooks.getCurrentServer() == null) return;
        long now = System.currentTimeMillis();
        if (now - lastProdCheck < 60_000) return;
        lastProdCheck = now;
        boolean dirty = false;
        java.util.Set<String> toRemove = new java.util.HashSet<>();
        for (ResourceAreaData d : generators.values()) {
            if (d.ownerGuild == null) continue;
            // Expiration check (48h)
            if (d.claimTimestamp > 0 && now - d.claimTimestamp > 172_800_000L) {
                // remove area and generator
                org.lupz.doomsdayessentials.combat.AreaManager.get().deleteArea(d.areaName);
                toRemove.add(d.areaName.toLowerCase());
                EssentialsMod.LOGGER.info("Generator area {} expired and was removed", d.areaName);
                continue;
            }
            double hours = (now - d.lastTimestamp) / 3_600_000.0;
            if (hours <= 0) continue;
            boolean areaDirty=false;
            for (ResourceAreaData.LootEntry entry : d.lootEntries) {
                if (entry.stored >= d.storageCap) continue;
                int produced = (int) Math.floor(hours * entry.perHour);
                if (produced <= 0) continue;
                entry.stored = Math.min(d.storageCap, entry.stored + produced);
                areaDirty=true;
            }
            if (areaDirty) {
                d.lastTimestamp = now;
                dirty = true;
            }
        }
        if (dirty) save();
        if (!toRemove.isEmpty()) {
            toRemove.forEach(generators::remove);
            save();
        }
    }
    private long lastProdCheck = 0;
    private final java.util.Set<String> toRemove = new java.util.HashSet<>();

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
        return generators.values().stream().filter(d -> guildName.equals(d.ownerGuild)).toList();
    }

    private void load() {
        loadInternal();
    }
} 