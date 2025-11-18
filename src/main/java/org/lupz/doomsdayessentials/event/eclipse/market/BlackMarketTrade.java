package org.lupz.doomsdayessentials.event.eclipse.market;

import java.util.Collections;
import java.util.List;

/**
 * A single black market trade definition parsed from JSON.
 * Supports 1-2 buy items and 1..N sell items (N>1 will be bundled).
 */
public class BlackMarketTrade {
    public final String alias; // human-friendly id
    public final List<ItemStackSpec> buys; // size 1 or 2
    public final List<ItemStackSpec> sells; // size >= 1
    public final int maxUses;
    public final int xp;
    public final float priceMultiplier;
    public final List<String> tags; // for filtering/search
    public final String mod; // optional source mod tag

    public BlackMarketTrade(String alias,
                            List<ItemStackSpec> buys,
                            List<ItemStackSpec> sells,
                            int maxUses,
                            int xp,
                            float priceMultiplier,
                            List<String> tags,
                            String mod) {
        this.alias = alias;
        this.buys = buys == null ? List.of() : List.copyOf(buys);
        this.sells = sells == null ? List.of() : List.copyOf(sells);
        this.maxUses = Math.max(1, maxUses);
        this.xp = Math.max(0, xp);
        this.priceMultiplier = Math.max(0f, priceMultiplier);
        this.tags = tags == null ? Collections.emptyList() : List.copyOf(tags);
        this.mod = mod;
    }
}

