package org.lupz.doomsdayessentials.rarity;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.Tag;
import org.lupz.doomsdayessentials.EssentialsMod;

import java.util.Locale;

/**
 * Server/client utility for reading and setting item rarity via NBT.
 * Also resolves default rarity from assets mapping on client when NBT is absent.
 */
public final class RarityManager {
    private RarityManager() {}

    public static final String NBT_KEY = "Rarity"; // stored inside stack root tag

    public enum RarityTier {
        COMMON("common"),
        UNCOMMON("uncommon"),
        RARE("rare"),
        EPIC("epic"),
        LEGENDARY("legendary");

        public final String id;
        RarityTier(String id) { this.id = id; }
        public static RarityTier fromString(String s) {
            if (s == null) return null;
            String k = s.toLowerCase(Locale.ROOT);
            for (RarityTier t : values()) if (t.id.equals(k)) return t;
            return null;
        }
    }

    /** Set rarity NBT on the given stack. */
    public static void setRarity(ItemStack stack, RarityTier tier) {
        if (stack == null || stack.isEmpty() || tier == null) return;
        var tag = stack.getOrCreateTag();
        tag.putString(NBT_KEY, tier.id);
    }

    /** Read rarity from NBT, or null if not present. */
    public static RarityTier getRarityNBT(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return null;
        var tag = stack.getTag();
        if (tag == null || !tag.contains(NBT_KEY, Tag.TAG_STRING)) return null;
        return RarityTier.fromString(tag.getString(NBT_KEY));
    }

    /**
     * Resolve rarity for an item.
     * Priority: server-synced overrides -> client assets mapping -> null (none)
     * NBT is intentionally ignored to avoid conflicts with mods (e.g., TACZ) reusing keys.
     */
    public static RarityTier resolveRarity(ItemStack stack) {
        // Prefer server-synced client overrides
        try {
            var key = ForgeRegistries.ITEMS.getKey(stack.getItem());
            if (key != null) {
                String itemId = key.toString();
                // Variant check (e.g., TACZ GunId)
                try {
                    var tag = stack.getTag();
                    if (tag != null) {
                        // Support common TACZ NBT key variants
                        String gunId = null;
                        for (String k : new String[]{"GunId", "gunId", "gun_id"}) {
                            if (tag.contains(k, Tag.TAG_STRING)) {
                                gunId = tag.getString(k);
                                if (gunId != null && !gunId.isEmpty()) break;
                            }
                        }
                        if (gunId != null && !gunId.isEmpty()) {
                            // Normalize: ensure namespace and lowercase for consistent matching
                            String itemKey = itemId.toLowerCase(java.util.Locale.ROOT);
                            String normalizedGunId = gunId.contains(":") ? gunId : ("tacz:" + gunId);
                            normalizedGunId = normalizedGunId.toLowerCase(java.util.Locale.ROOT);
                            String rawLowerGunId = gunId.toLowerCase(java.util.Locale.ROOT);
                            // Try normalized key first (preferred)
                            RarityTier v = RarityClientOverrides.getVariant(itemKey + "|" + normalizedGunId);
                            if (v != null) return v;
                            // Fallback: try raw gunId (in case server stored without namespace)
                            v = RarityClientOverrides.getVariant(itemKey + "|" + rawLowerGunId);
                            if (v != null) return v;
                        }
                    }
                } catch (Throwable ignored2) {}
                // Base item override
                RarityTier ov = RarityClientOverrides.getByItemId(itemId);
                if (ov != null) return ov;
            }
        } catch (Throwable ignored) {}
        // Fallback to assets mapping on client; ignore on dedicated server
        try {
            // Prefer variant fallback when GunId is present
            RarityTier vt = RarityAssetsClient.getVariantRarityFromAssets(stack);
            if (vt != null) return vt;
            return RarityAssetsClient.getRarityFromAssets(stack);
        } catch (Throwable t) {
            return null;
        }
    }

    /** Get texture path for a rarity tier. */
    public static ResourceLocation getTexture(RarityTier tier) {
        if (tier == null) return null;
        return ResourceLocation.fromNamespaceAndPath(EssentialsMod.MOD_ID, "textures/gui/rarity/" + tier.id + ".png");
    }
}
