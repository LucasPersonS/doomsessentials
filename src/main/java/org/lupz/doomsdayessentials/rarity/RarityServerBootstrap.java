package org.lupz.doomsdayessentials.rarity;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.lupz.doomsdayessentials.EssentialsMod;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Server-side bootstrap loader to auto-assign item rarities at server start.
 *
 * Priority order for loading:
 * 1) External config file: <gameDir>/config/doomsdayessentials/weaponsrarity.md (if present)
 * 2) Mod assets JSON: /assets/doomsdayessentials/rarity/rarity.json (packaged with the mod)
 *
 * If both are present, the external config is applied first, then assets JSON fills in any remaining.
 */
public final class RarityServerBootstrap {
    private static final Gson GSON = new Gson();

    private RarityServerBootstrap() {}

    /** Entry point to be called on server starting. */
    public static void loadAtServerStart() {
        Map<String, RarityManager.RarityTier> items = new HashMap<>();
        Map<String, RarityManager.RarityTier> variants = new HashMap<>();

        // Only use packaged assets JSON for persistent defaults

        // 2) Fallback to packaged assets JSON
        try (InputStream is = RarityServerBootstrap.class.getResourceAsStream("/assets/" + EssentialsMod.MOD_ID + "/rarity/rarity.json")) {
            if (is != null) {
                applyJson(items, variants, new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8)));
            } else {
                EssentialsMod.LOGGER.warn("rarity.json not found in assets; no default rarities will be applied");
            }
        } catch (IOException e) {
            EssentialsMod.LOGGER.warn("Failed to load assets rarity.json", e);
        }

        // Apply to server registry in a single broadcast
        if (!items.isEmpty() || !variants.isEmpty()) {
            RarityServerRegistry.replaceAll(items, variants);
            EssentialsMod.LOGGER.info("Applied {} item and {} variant rarity entries at server start", items.size(), variants.size());
        } else {
            EssentialsMod.LOGGER.info("No item rarity entries found to apply at server start");
        }
    }

    private static void applyJson(Map<String, RarityManager.RarityTier> items,
                                  Map<String, RarityManager.RarityTier> variants,
                                  BufferedReader br) {
        try {
            JsonObject root = GSON.fromJson(br, JsonObject.class);
            for (Map.Entry<String, JsonElement> e : root.entrySet()) {
                String tierKey = e.getKey().toLowerCase(Locale.ROOT);
                RarityManager.RarityTier tier = RarityManager.RarityTier.fromString(tierKey);
                if (tier == null || !e.getValue().isJsonArray()) continue;
                for (JsonElement el : e.getValue().getAsJsonArray()) {
                    try {
                        String id = el.getAsString();
                        items.putIfAbsent(id, tier);
                    } catch (Exception ignored) {}
                }
            }
            // Parse variants object if present: { "variants": { "common": ["item|variant", ...], ... } }
            if (root.has("variants") && root.get("variants").isJsonObject()) {
                JsonObject varRoot = root.getAsJsonObject("variants");
                for (Map.Entry<String, JsonElement> e : varRoot.entrySet()) {
                    String tierKey = e.getKey().toLowerCase(Locale.ROOT);
                    RarityManager.RarityTier tier = RarityManager.RarityTier.fromString(tierKey);
                    if (tier == null || !e.getValue().isJsonArray()) continue;
                    for (JsonElement el : e.getValue().getAsJsonArray()) {
                        try {
                            String composite = el.getAsString();
                            variants.putIfAbsent(composite, tier);
                        } catch (Exception ignored) {}
                    }
                }
            }
        } catch (Exception ex) {
            EssentialsMod.LOGGER.warn("Failed to parse assets rarity.json", ex);
        }
    }
}
