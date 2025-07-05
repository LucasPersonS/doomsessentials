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
        data.lastTimestamp = System.currentTimeMillis();
        save();
    }

    public void unclaimArea(String areaName) {
        ResourceAreaData d = generators.get(areaName);
        if (d != null) {
            d.ownerGuild = null;
            d.storedItems = 0;
            save();
        }
    }

    /** Collects all stored items for the guild and returns total stacks given. */
    public int collectForGuild(ServerPlayer player, String guildName) {
        int given = 0;
        for (ResourceAreaData d : generators.values()) {
            if (!guildName.equals(d.ownerGuild) || d.storedItems == 0) continue;
            Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(d.lootId));
            if (item == null || item == net.minecraft.world.level.block.Blocks.AIR.asItem()) {
                EssentialsMod.LOGGER.error("Invalid loot item id {} for generator {}", d.lootId, d.areaName);
                continue;
            }
            int remaining = d.storedItems;
            while (remaining > 0) {
                int stackSize = Math.min(item.getMaxStackSize(), remaining);
                ItemStack stack = new ItemStack(item, stackSize);
                if (!player.getInventory().add(stack)) {
                    player.drop(stack, false);
                }
                remaining -= stackSize;
            }
            given += d.storedItems;
            d.storedItems = 0;
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
        for (ResourceAreaData d : generators.values()) {
            if (d.ownerGuild == null) continue;
            if (d.storedItems >= d.storageCap) continue;
            double hours = (now - d.lastTimestamp) / 3_600_000.0;
            if (hours <= 0) continue;
            int produced = (int) Math.floor(hours * d.itemsPerHour);
            if (produced <= 0) continue;
            d.storedItems = Math.min(d.storageCap, d.storedItems + produced);
            d.lastTimestamp = now;
            dirty = true;
        }
        if (dirty) save();
    }
    private long lastProdCheck = 0;

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

    // For config packers
    public List<ResourceAreaData> getGeneratorsForGuild(String guildName) {
        return generators.values().stream().filter(d -> guildName.equals(d.ownerGuild)).toList();
    }

    private void load() {
        loadInternal();
    }
} 