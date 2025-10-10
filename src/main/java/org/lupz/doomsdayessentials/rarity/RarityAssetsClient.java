package org.lupz.doomsdayessentials.rarity;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.lupz.doomsdayessentials.EssentialsMod;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Client-only assets mapping from item id -> rarity tier, loaded from
 * assets/doomsdayessentials/rarity/rarity.json
 *
 * {
 *   "common": ["minecraft:iron_ingot", ...],
 *   "uncommon": [ ... ],
 *   ...
 * }
 */
public final class RarityAssetsClient {
    private RarityAssetsClient() {}

    private static final Gson GSON = new Gson();
    private static final ResourceLocation CONFIG_RL = ResourceLocation.fromNamespaceAndPath(EssentialsMod.MOD_ID, "rarity/rarity.json");

    private static boolean loaded;
    private static final Map<String, RarityManager.RarityTier> ITEM_TO_TIER = new HashMap<>();

    public static void reload() {
        loaded = false;
        ITEM_TO_TIER.clear();
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
                for (Map.Entry<String, JsonElement> e : root.entrySet()) {
                    String tierKey = e.getKey().toLowerCase(Locale.ROOT);
                    RarityManager.RarityTier tier = RarityManager.RarityTier.fromString(tierKey);
                    if (tier == null || !e.getValue().isJsonArray()) continue;
                    for (JsonElement el : e.getValue().getAsJsonArray()) {
                        try {
                            String id = el.getAsString();
                            ITEM_TO_TIER.put(id, tier);
                        } catch (Exception ignored) {}
                    }
                }
            }
        } catch (Exception ignored) {}
    }

    /** Resolve rarity from assets mapping, or null. */
    public static RarityManager.RarityTier getRarityFromAssets(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return null;
        ensureLoaded();
        var key = net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(stack.getItem());
        if (key == null) return null;
        return ITEM_TO_TIER.get(key.toString());
    }
}
