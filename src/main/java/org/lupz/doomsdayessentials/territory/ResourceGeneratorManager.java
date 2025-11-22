package org.lupz.doomsdayessentials.territory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
// Tick listener removed – production now timestamp based
import net.minecraftforge.registries.ForgeRegistries;
import org.lupz.doomsdayessentials.EssentialsMod;
// import kept intentionally if TerritoryAreaLoader side-effects are required

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
        data.dominationExpiry = System.currentTimeMillis() + 72L * 60L * 60L * 1000L;
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
        if (d.dominationExpiry > 0 && now >= d.dominationExpiry) {
            var am = org.lupz.doomsdayessentials.combat.AreaManager.get();
            var current = am.getArea(d.areaName);
            if (current != null) {
                am.deleteArea(d.areaName);
                var danger = new org.lupz.doomsdayessentials.combat.ManagedArea(
                        d.areaName,
                        org.lupz.doomsdayessentials.combat.AreaType.DANGER,
                        current.getDimension(),
                        current.getPos1(),
                        current.getPos2()
                );
                am.addArea(danger);
            }
            d.ownerGuild = null;
            d.claimTimestamp = 0L;
            d.dominationExpiry = 0L;
            for (ResourceAreaData.LootEntry e : d.lootEntries) e.stored = 0;
            save();
            EssentialsMod.LOGGER.info("Domination for area {} expired; reverted to DANGER and cleared ownership", d.areaName);
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

    /**
     * Deposits items from a player's inventory into the guild storage (generators) matching item ids.
     * Returns the total number of items removed from the player's inventory and added to storage.
     */
    public int depositFromInventory(net.minecraft.server.level.ServerPlayer player, String guildName) {
        java.util.List<ResourceAreaData> dests = getGeneratorsForGuild(guildName);
        if (dests.isEmpty()) return 0;
        int moved = 0;
        // Build quick index: itemId -> list of entries to store into
        java.util.Map<String, java.util.List<ResourceAreaData.LootEntry>> byId = new java.util.HashMap<>();
        for (ResourceAreaData d : dests) {
            accrue(d);
            for (ResourceAreaData.LootEntry e : d.lootEntries) {
                byId.computeIfAbsent(e.id, k -> new java.util.ArrayList<>()).add(e);
            }
        }

        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.isEmpty()) continue;
            String id = ForgeRegistries.ITEMS.getKey(stack.getItem()).toString();
            java.util.List<ResourceAreaData.LootEntry> slots = byId.get(id);
            if (slots == null || slots.isEmpty()) continue; // only deposit items produced by the guild generators

            int remaining = stack.getCount();
            for (ResourceAreaData.LootEntry e : slots) {
                int free = 0;
                for (ResourceAreaData d : dests) {
                    for (ResourceAreaData.LootEntry le : d.lootEntries) {
                        if (le == e) { free = Math.max(0, d.storageCap - e.stored); break; }
                    }
                }
                if (free <= 0) continue;
                int add = Math.min(free, remaining);
                if (add <= 0) continue;
                e.stored += add;
                remaining -= add;
                moved += add;
                if (remaining == 0) break;
            }
            if (moved > 0 && remaining != stack.getCount()) {
                stack.shrink(stack.getCount() - remaining);
                player.getInventory().setItem(i, remaining > 0 ? stack : ItemStack.EMPTY);
            }
        }
        if (moved > 0) save();
        return moved;
    }

    /**
     * Moves up to totalCount items from all generator storages owned by defender to attacker at random.
     * Returns the number of items transferred.
     */
    public int plunder(String defenderGuild, String attackerGuild, int totalCount) {
        java.util.Map<String, Integer> details = plunderDetailed(defenderGuild, attackerGuild, totalCount);
        int sum = 0;
        for (Integer v : details.values()) sum += v;
        return sum;
    }

    public java.util.Map<String, Integer> plunderDetailed(String defenderGuild, String attackerGuild, int totalCount) {
        java.util.Map<String, Integer> movedById = new java.util.HashMap<>();
        if (totalCount <= 0) return movedById;
        java.util.List<ResourceAreaData> sources = getGeneratorsForGuild(defenderGuild);
        if (sources.isEmpty()) return movedById;
        int available = 0;
        for (ResourceAreaData d : sources) {
            accrue(d);
            for (ResourceAreaData.LootEntry e : d.lootEntries) available += e.stored;
        }
        if (available == 0) return movedById;
        int toMove = Math.min(totalCount, available);

        for (ResourceAreaData d : sources) {
            for (ResourceAreaData.LootEntry e : d.lootEntries) {
                if (toMove <= 0) break;
                if (e.stored <= 0) continue;
                int take = Math.min(e.stored, toMove);
                e.stored -= take;
                toMove -= take;
                movedById.merge(e.id, take, Integer::sum);
            }
            if (toMove <= 0) break;
        }

        if (!movedById.isEmpty()) {
            java.util.List<ResourceAreaData> dests = getGeneratorsForGuild(attackerGuild);
            ResourceAreaData target = dests.isEmpty() ? null : dests.get(0);
            if (target == null) {
                target = createIfAbsent("plunder_" + attackerGuild);
                target.ownerGuild = attackerGuild;
                target.storageCap = Math.max(target.storageCap, 9999);
                dests = java.util.List.of(target);
            }
            for (var entry : movedById.entrySet()) {
                String id = entry.getKey();
                int amt = entry.getValue();
                ResourceAreaData.LootEntry match = null;
                for (ResourceAreaData.LootEntry te : target.lootEntries) {
                    if (java.util.Objects.equals(te.id, id)) { match = te; break; }
                }
                if (match == null) {
                    match = new ResourceAreaData.LootEntry(id, 0);
                    target.lootEntries.add(match);
                }
                match.stored = Math.min(target.storageCap, match.stored + amt);
            }
            save();
        }
        return movedById;
    }

    private void load() {
        loadInternal();
    }
} 
