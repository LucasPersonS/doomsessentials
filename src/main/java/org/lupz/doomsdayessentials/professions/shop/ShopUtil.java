package org.lupz.doomsdayessentials.professions.shop;

import net.minecraft.resources.ResourceLocation;
import org.lupz.doomsdayessentials.config.EssentialsConfig;

import java.util.*;

/**
 * Parses the configurable shop entries defined in essentials config.
 */
public final class ShopUtil {

    public record Entry(ResourceLocation outputId, int outputCount, Map<ResourceLocation,Integer> costs) {
        public ResourceLocation costId() { return costs.keySet().stream().findFirst().orElse(null); }
        public int costCount() { return costs.values().stream().findFirst().orElse(0); }
    }

    private ShopUtil() {}

    /**
     * Returns a map alias -> entry. Alias is the path of the output item (lower-case, no namespace).
     */
    public static Map<String, Entry> getEntries() {
        Map<String, Entry> map = new LinkedHashMap<>();
        for (String line : EssentialsConfig.SHOP_ITEMS.get()) {
            String[] parts = line.split(",");
            try {
                if(parts.length>=4 && parts.length%2==0) {
                    ResourceLocation output = ResourceLocation.tryParse(parts[0]);
                    int outCount = Integer.parseInt(parts[1]);
                    Map<ResourceLocation,Integer> costs = new LinkedHashMap<>();
                    for(int i=2;i<parts.length;i+=2){
                        ResourceLocation cid = ResourceLocation.tryParse(parts[i]);
                        int cc = Integer.parseInt(parts[i+1]);
                        costs.put(cid, cc);
                    }
                    map.put(output.getPath(), new Entry(output, outCount, costs));
                }
                // fallback legacy handled above? else ignore
            } catch (Exception ignored) {
            }
        }
        return map;
    }
} 