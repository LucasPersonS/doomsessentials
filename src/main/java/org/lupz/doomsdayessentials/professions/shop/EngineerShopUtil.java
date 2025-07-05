package org.lupz.doomsdayessentials.professions.shop;

import net.minecraft.resources.ResourceLocation;
import java.util.*;

/**
 * Static list of engineer craftable items and their costs.
 */
public final class EngineerShopUtil {

    public record Entry(ResourceLocation outputId, int outputCount, Map<ResourceLocation,Integer> costs) {
        public ResourceLocation costId() { return costs.keySet().stream().findFirst().orElse(null); }
        public int costCount() { return costs.values().stream().findFirst().orElse(0); }
    }

    private EngineerShopUtil() {}

    private static final Map<String, Entry> ENTRIES = new HashMap<>();
    static {
        // initially empty; add via /engenheiro craft add
    }

    public static Map<String, Entry> getEntries() { return ENTRIES; }

    public static void addOrReplace(String alias, Entry entry) {
        ENTRIES.put(alias, entry);
    }

    // Serialisation helpers -----------------------------------------------------------------

    /**
     * Convert entry to config line.
     */
    public static String toConfigLine(Entry e) {
        StringBuilder sb = new StringBuilder();
        sb.append(e.outputId()).append(",").append(e.outputCount());
        e.costs().forEach((id,cnt) -> sb.append(",").append(id).append(",").append(cnt));
        return sb.toString();
    }

    /** Parse config string into entry. */
    public static Entry parse(String line) {
        String[] parts = line.split(",");
        if (parts.length < 4 || parts.length % 2 != 0) return null;
        try {
            ResourceLocation out = new ResourceLocation(parts[0]);
            int outCount = Integer.parseInt(parts[1]);
            Map<ResourceLocation,Integer> costs = new LinkedHashMap<>();
            for (int i=2;i<parts.length;i+=2) {
                ResourceLocation costId = new ResourceLocation(parts[i]);
                int costCount = Integer.parseInt(parts[i+1]);
                costs.put(costId,costCount);
            }
            return new Entry(out,outCount,costs);
        } catch (Exception ignored){return null;}
    }
} 