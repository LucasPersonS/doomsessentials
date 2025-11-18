package org.lupz.doomsdayessentials.event.eclipse.market;

import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.Level;

import java.util.*;

/**
 * Minimal server-side edit session manager for Night Market offers.
 * Keeps a linear history of JSON snapshots and supports undo/redo.
 */
public final class MarketEditSession {
    private static final Map<UUID, MarketEditSession> SESSIONS = new HashMap<>();

    private final UUID marketId;
    private final Deque<String> past = new ArrayDeque<>(); // snapshots before current
    private final Deque<String> future = new ArrayDeque<>(); // snapshots after current
    private String currentJson;
    private String initialJson;

    private MarketEditSession(UUID marketId){ this.marketId = marketId; }

    public static boolean start(Level lvl, UUID marketId){
        MarketEditSession s = new MarketEditSession(marketId);
        s.currentJson = MarketPresetManager.exportOffersToJson(lvl, NightMarketManager.getOffers(lvl, marketId));
        s.initialJson = s.currentJson;
        SESSIONS.put(marketId, s);
        return true;
    }

    public static boolean snapshot(Level lvl, UUID marketId){
        MarketEditSession s = SESSIONS.get(marketId);
        if (s == null) return false;
        if (s.currentJson != null) s.past.push(s.currentJson);
        s.future.clear();
        s.currentJson = MarketPresetManager.exportOffersToJson(lvl, NightMarketManager.getOffers(lvl, marketId));
        return true;
    }

    public static boolean undo(Level lvl, UUID marketId){
        MarketEditSession s = SESSIONS.get(marketId);
        if (s == null) return false;
        if (s.past.isEmpty()) return false;
        String prev = s.past.pop();
        if (s.currentJson != null) s.future.push(s.currentJson);
        s.currentJson = prev;
        try{
            MerchantOffers offers = BlackMarketConfigManager.toOffersFromJson(prev, lvl, new BlackMarketFilter(null, Set.of(), null));
            NightMarketManager.setOffers(marketId, offers);
            return true;
        }catch(Exception e){ return false; }
    }

    public static boolean redo(Level lvl, UUID marketId){
        MarketEditSession s = SESSIONS.get(marketId);
        if (s == null) return false;
        if (s.future.isEmpty()) return false;
        String next = s.future.pop();
        if (s.currentJson != null) s.past.push(s.currentJson);
        s.currentJson = next;
        try{
            MerchantOffers offers = BlackMarketConfigManager.toOffersFromJson(next, lvl, new BlackMarketFilter(null, Set.of(), null));
            NightMarketManager.setOffers(marketId, offers);
            return true;
        }catch(Exception e){ return false; }
    }

    public static boolean commit(UUID marketId){
        return SESSIONS.remove(marketId) != null;
    }

    public static boolean cancel(Level lvl, UUID marketId){
        MarketEditSession s = SESSIONS.remove(marketId);
        if (s == null) return false;
        try{
            MerchantOffers offers = BlackMarketConfigManager.toOffersFromJson(s.initialJson, lvl, new BlackMarketFilter(null, Set.of(), null));
            NightMarketManager.setOffers(marketId, offers);
            return true;
        }catch(Exception e){ return false; }
    }

    public static boolean hasSession(UUID marketId){ return SESSIONS.containsKey(marketId); }
}
