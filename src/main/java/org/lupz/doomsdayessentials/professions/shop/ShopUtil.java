package org.lupz.doomsdayessentials.professions.shop;

import net.minecraft.resources.ResourceLocation;
import org.lupz.doomsdayessentials.config.EssentialsConfig;

import java.util.HashMap;
import java.util.Map;

/**
 * Parses the configurable shop entries defined in essentials config.
 */
public final class ShopUtil {

    public record Entry(ResourceLocation outputId, int outputCount, ResourceLocation costId, int costCount) {}

    private ShopUtil() {}

    /**
     * Returns a map alias -> entry. Alias is the path of the output item (lower-case, no namespace).
     */
    public static Map<String, Entry> getEntries() {
        Map<String, Entry> map = new HashMap<>();
        for (String line : EssentialsConfig.SHOP_ITEMS.get()) {
            String[] parts = line.split(",");
            try {
                if (parts.length == 3) {
                    // Legacy format: outputId,costId,costCount (output count =1)
                    ResourceLocation output = new ResourceLocation(parts[0]);
                    ResourceLocation cost = new ResourceLocation(parts[1]);
                    int costCount = Integer.parseInt(parts[2]);
                    map.put(output.getPath(), new Entry(output, 1, cost, costCount));
                } else if (parts.length == 4) {
                    // New format: outputId,outputCount,costId,costCount
                    ResourceLocation output = new ResourceLocation(parts[0]);
                    int outputCount = Integer.parseInt(parts[1]);
                    ResourceLocation cost = new ResourceLocation(parts[2]);
                    int costCount = Integer.parseInt(parts[3]);
                    map.put(output.getPath(), new Entry(output, outputCount, cost, costCount));
                }
            } catch (Exception ignored) {
            }
        }
        return map;
    }
} 