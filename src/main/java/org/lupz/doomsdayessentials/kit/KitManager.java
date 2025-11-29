package org.lupz.doomsdayessentials.kit;

import com.google.gson.*;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.loading.FMLPaths;
import org.lupz.doomsdayessentials.EssentialsMod;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Manages kit storage and retrieval.
 * Kits are stored in JSON format: config/doomsdayessentials/kits.json
 */
public final class KitManager {
    private static final Path FILE_PATH = FMLPaths.CONFIGDIR.get().resolve("doomsdayessentials/kits.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    // Valid kit tiers
    public static final String TIER_FREE = "free";
    public static final String TIER_SOBREVIVENTE = "sobrevivente";
    public static final String TIER_INFECTADO = "infectado";
    public static final String TIER_DISSOLUTO = "dissoluto";
    public static final List<String> TIERS = List.of(TIER_FREE, TIER_SOBREVIVENTE, TIER_INFECTADO, TIER_DISSOLUTO);

    // Storage: tier -> cooldownType -> Kit
    private static final Map<String, Map<Kit.CooldownType, Kit>> kits = new HashMap<>();

    private KitManager() {
    }

    /**
     * Loads kits from disk.
     */
    public static void load() {
        kits.clear();
        ensureFile();
        if (!Files.exists(FILE_PATH))
            return;

        try (BufferedReader br = Files.newBufferedReader(FILE_PATH, StandardCharsets.UTF_8)) {
            JsonObject root = GSON.fromJson(br, JsonObject.class);
            if (root == null)
                return;

            for (String tier : TIERS) {
                if (!root.has(tier))
                    continue;
                JsonObject tierObj = root.getAsJsonObject(tier);

                for (Kit.CooldownType cooldownType : Kit.CooldownType.values()) {
                    String cooldownId = cooldownType.getId();
                    if (!tierObj.has(cooldownId))
                        continue;

                    JsonObject cooldownObj = tierObj.getAsJsonObject(cooldownId);
                    if (!cooldownObj.has("items"))
                        continue;

                    JsonArray itemsArray = cooldownObj.getAsJsonArray("items");
                    List<String> snbts = new ArrayList<>();
                    for (JsonElement elem : itemsArray) {
                        if (elem.isJsonPrimitive()) {
                            snbts.add(elem.getAsString());
                        }
                    }

                    Kit kit = new Kit(tier, cooldownType, snbts);
                    kits.computeIfAbsent(tier, k -> new HashMap<>()).put(cooldownType, kit);
                }
            }

            EssentialsMod.LOGGER.info("Loaded {} kits from disk", countKits());
        } catch (IOException e) {
            EssentialsMod.LOGGER.error("Failed to read kits.json", e);
        }
    }

    /**
     * Saves kits to disk.
     */
    public static void save() {
        ensureFile();
        JsonObject root = new JsonObject();

        for (String tier : TIERS) {
            JsonObject tierObj = new JsonObject();
            Map<Kit.CooldownType, Kit> tierKits = kits.get(tier);

            if (tierKits != null) {
                for (Kit.CooldownType cooldownType : Kit.CooldownType.values()) {
                    Kit kit = tierKits.get(cooldownType);
                    if (kit != null) {
                        JsonObject cooldownObj = new JsonObject();
                        JsonArray itemsArray = new JsonArray();
                        for (String snbt : kit.getItemSnbts()) {
                            itemsArray.add(snbt);
                        }
                        cooldownObj.add("items", itemsArray);
                        tierObj.add(cooldownType.getId(), cooldownObj);
                    }
                }
            }

            root.add(tier, tierObj);
        }

        try (BufferedWriter bw = Files.newBufferedWriter(FILE_PATH, StandardCharsets.UTF_8)) {
            bw.write(GSON.toJson(root));
            EssentialsMod.LOGGER.info("Saved {} kits to disk", countKits());
        } catch (IOException e) {
            EssentialsMod.LOGGER.error("Failed to write kits.json", e);
        }
    }

    private static void ensureFile() {
        try {
            Files.createDirectories(FILE_PATH.getParent());
            if (!Files.exists(FILE_PATH)) {
                JsonObject root = new JsonObject();
                for (String tier : TIERS) {
                    root.add(tier, new JsonObject());
                }
                Files.writeString(FILE_PATH, GSON.toJson(root), StandardCharsets.UTF_8);
            }
        } catch (IOException e) {
            EssentialsMod.LOGGER.error("Failed to ensure kits.json path", e);
        }
    }

    /**
     * Creates or updates a kit for the given tier and cooldown type.
     */
    public static boolean createKit(String tier, Kit.CooldownType cooldownType, List<ItemStack> items) {
        if (!TIERS.contains(tier.toLowerCase()))
            return false;

        String normalizedTier = tier.toLowerCase();
        Kit kit = Kit.fromItems(normalizedTier, cooldownType, items);
        kits.computeIfAbsent(normalizedTier, k -> new HashMap<>()).put(cooldownType, kit);
        save();
        return true;
    }

    /**
     * Gets a kit for the given tier and cooldown type.
     */
    public static Kit getKit(String tier, Kit.CooldownType cooldownType) {
        Map<Kit.CooldownType, Kit> tierKits = kits.get(tier.toLowerCase());
        if (tierKits == null)
            return null;
        return tierKits.get(cooldownType);
    }

    /**
     * Checks if a kit exists for the given tier and cooldown type.
     */
    public static boolean hasKit(String tier, Kit.CooldownType cooldownType) {
        return getKit(tier, cooldownType) != null;
    }

    /**
     * Gets all kits for a specific tier.
     */
    public static Map<Kit.CooldownType, Kit> getKitsForTier(String tier) {
        Map<Kit.CooldownType, Kit> tierKits = kits.get(tier.toLowerCase());
        return tierKits != null ? new HashMap<>(tierKits) : new HashMap<>();
    }

    /**
     * Deletes a kit.
     */
    public static boolean deleteKit(String tier, Kit.CooldownType cooldownType) {
        Map<Kit.CooldownType, Kit> tierKits = kits.get(tier.toLowerCase());
        if (tierKits == null)
            return false;

        Kit removed = tierKits.remove(cooldownType);
        if (removed != null) {
            save();
            return true;
        }
        return false;
    }

    /**
     * Returns the total number of kits.
     */
    private static int countKits() {
        int count = 0;
        for (Map<Kit.CooldownType, Kit> tierKits : kits.values()) {
            count += tierKits.size();
        }
        return count;
    }

    /**
     * Validates if a tier name is valid.
     */
    public static boolean isValidTier(String tier) {
        return TIERS.contains(tier.toLowerCase());
    }
}
