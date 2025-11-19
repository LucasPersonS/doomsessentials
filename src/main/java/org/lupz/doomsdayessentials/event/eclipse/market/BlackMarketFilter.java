package org.lupz.doomsdayessentials.event.eclipse.market;

import java.util.Locale;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Simple filter for trades based on alias, tags and mod field.
 */
public class BlackMarketFilter implements Predicate<BlackMarketTrade> {
    private final String query;
    private final Set<String> tagWhitelist;
    private final String mod;

    public BlackMarketFilter(String query, Set<String> tagWhitelist, String mod){
        this.query = query == null ? null : query.toLowerCase(Locale.ROOT);
        this.tagWhitelist = tagWhitelist;
        this.mod = mod == null ? null : mod.toLowerCase(Locale.ROOT);
    }

    @Override
    public boolean test(BlackMarketTrade t){
        if (query != null && !(t.alias.toLowerCase(Locale.ROOT).contains(query))) return false;
        if (tagWhitelist != null && !tagWhitelist.isEmpty()) {
            boolean matched = t.tags.stream().anyMatch(tagWhitelist::contains);
            if (!matched) return false;
        }
        if (mod != null) {
            if (t.mod == null || !t.mod.toLowerCase(Locale.ROOT).equals(mod)) return false;
        }
        return true;
    }
}

