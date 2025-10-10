package org.lupz.doomsdayessentials.rarity;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/** Client-side overrides synced from server for itemId -> rarity tier. */
public final class RarityClientOverrides {
    // Base overrides by item registry id, e.g., "minecraft:diamond_sword"
    private static final Map<String, RarityManager.RarityTier> ITEM_TO_TIER = new HashMap<>();
    // Variant overrides by composite key, e.g., "tacz:modern_kinetic_gun|doomsday:kuronami_red"
    private static final Map<String, RarityManager.RarityTier> VARIANT_TO_TIER = new HashMap<>();

    private RarityClientOverrides() {}

    // Back-compat: if only item data provided, clear variants
    public static synchronized void replaceAll(Map<String, RarityManager.RarityTier> data) {
        ITEM_TO_TIER.clear();
        if (data != null) ITEM_TO_TIER.putAll(data);
        VARIANT_TO_TIER.clear();
    }

    public static synchronized void replaceAll(Map<String, RarityManager.RarityTier> items,
                                               Map<String, RarityManager.RarityTier> variants) {
        ITEM_TO_TIER.clear();
        if (items != null) ITEM_TO_TIER.putAll(items);
        VARIANT_TO_TIER.clear();
        if (variants != null) VARIANT_TO_TIER.putAll(variants);
    }

    public static synchronized RarityManager.RarityTier getByItemId(String itemId) {
        return ITEM_TO_TIER.get(itemId);
    }

    public static synchronized RarityManager.RarityTier getVariant(String variantKey) {
        return VARIANT_TO_TIER.get(variantKey);
    }

    public static synchronized Map<String, RarityManager.RarityTier> snapshotItems() {
        return Collections.unmodifiableMap(new HashMap<>(ITEM_TO_TIER));
    }

    public static synchronized Map<String, RarityManager.RarityTier> snapshotVariants() {
        return Collections.unmodifiableMap(new HashMap<>(VARIANT_TO_TIER));
    }

    // Back-compat alias
    public static synchronized RarityManager.RarityTier get(String itemId) {
        return getByItemId(itemId);
    }
}
