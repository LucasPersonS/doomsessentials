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
                    if (tag != null && tag.contains("GunId", Tag.TAG_STRING)) {
                        String gunId = tag.getString("GunId");
                        if (!gunId.isEmpty()) {
                            RarityTier v = RarityClientOverrides.getVariant(itemId + "|" + gunId);
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
