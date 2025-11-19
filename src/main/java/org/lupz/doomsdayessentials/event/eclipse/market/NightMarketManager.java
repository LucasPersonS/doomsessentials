package org.lupz.doomsdayessentials.event.eclipse.market;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.ForgeRegistries;

public final class NightMarketManager {
    // Per-market offers and alias mappings
    private static final java.util.Map<java.util.UUID, MerchantOffers> MARKET_OFFERS = new java.util.HashMap<>();
    private static final java.util.Map<java.util.UUID, java.util.IdentityHashMap<MerchantOffer, String>> MARKET_ALIASES = new java.util.HashMap<>();

    private NightMarketManager(){}

    /** Get offers for a specific market. Returns a copy for GUI stability. */
    public static MerchantOffers getOffers(Level lvl, java.util.UUID marketId){
        MerchantOffers offers = MARKET_OFFERS.computeIfAbsent(marketId, id -> new MerchantOffers());
        MerchantOffers copy = new MerchantOffers();
        copy.addAll(offers);
        return copy;
    }

    /** Internal: get mutable offers reference for a specific market. */
    public static MerchantOffers getOffersMutable(java.util.UUID marketId){
        return MARKET_OFFERS.computeIfAbsent(marketId, id -> new MerchantOffers());
    }

    /** Replace the offers for a market with the provided set. */
    public static void setOffers(java.util.UUID marketId, MerchantOffers newOffers){
        MerchantOffers target = MARKET_OFFERS.computeIfAbsent(marketId, id -> new MerchantOffers());
        target.clear();
        target.addAll(newOffers);
    }

    /** Backwards-compatible: global offers (legacy). */
    @Deprecated
    public static MerchantOffers getOffers(Level lvl){
        // Use a single global market key
        return getOffers(lvl, java.util.UUID.fromString("00000000-0000-0000-0000-000000000000"));
    }

    public static boolean addOfferTo(java.util.UUID marketId, String buy1Id, int buy1Count, String buy2Id, int buy2Count, String sellId, int sellCount, int maxUses, int xp, float priceMult){
        Item buy1 = itemOf(buy1Id); if (buy1 == null) return false;
        Item sell = itemOf(sellId); if (sell == null) return false;
        ItemStack buy1s = new ItemStack(buy1, Math.max(1,buy1Count));
        ItemStack sellS = new ItemStack(sell, Math.max(1,sellCount));
        MerchantOffer offer;
        if (buy2Id != null && !buy2Id.isBlank()){
            Item buy2 = itemOf(buy2Id); if (buy2 == null) return false;
            ItemStack buy2s = new ItemStack(buy2, Math.max(1,buy2Count));
            offer = new MerchantOffer(buy1s, buy2s, sellS, maxUses, xp, priceMult);
        }else{
            offer = new MerchantOffer(buy1s, sellS, maxUses, xp, priceMult);
        }
        // Advanced validation: prevent exact duplicate offers
        MerchantOffers existing = MARKET_OFFERS.computeIfAbsent(marketId, id -> new MerchantOffers());
        for (MerchantOffer o : existing){
            boolean sameBuys = o.getBaseCostA().getItem() == buy1s.getItem()
                    && o.getBaseCostA().getCount() == buy1s.getCount()
                    && (o.getCostB().isEmpty() ? (buy2Id == null || buy2Id.isBlank()) : (itemOf(buy2Id) == o.getCostB().getItem() && o.getCostB().getCount() == Math.max(1,buy2Count)));
            boolean sameSell = o.getResult().getItem() == sellS.getItem() && o.getResult().getCount() == sellS.getCount();
            boolean sameParams = o.getMaxUses() == maxUses && o.getXp() == xp && Math.abs(o.getPriceMultiplier() - priceMult) < 1e-6;
            if (sameBuys && sameSell && sameParams){
                return false; // duplicate
            }
        }
        existing.add(offer);
        return true;
    }

    /** Backwards-compatible method targeting the global market. */
    @Deprecated
    public static boolean addOffer(String buy1Id, int buy1Count, String buy2Id, int buy2Count, String sellId, int sellCount, int maxUses, int xp, float priceMult){
        return addOfferTo(java.util.UUID.fromString("00000000-0000-0000-0000-000000000000"), buy1Id, buy1Count, buy2Id, buy2Count, sellId, sellCount, maxUses, xp, priceMult);
    }

    public static boolean clear(){
        MARKET_OFFERS.values().forEach(MerchantOffers::clear);
        MARKET_ALIASES.clear();
        return true;
    }

    /** Clear offers and aliases only for a specific market. */
    public static boolean clearMarket(java.util.UUID marketId){
        MerchantOffers offers = MARKET_OFFERS.get(marketId);
        if (offers != null) offers.clear();
        java.util.IdentityHashMap<MerchantOffer, String> aliases = MARKET_ALIASES.get(marketId);
        if (aliases != null) aliases.clear();
        return true;
    }

    /** Replace current offers with those produced from black_market.json */
    public static void reloadFromJson(net.minecraft.world.level.Level lvl){
        try {
            BlackMarketConfigManager.reload();
            MerchantOffers newOffers = BlackMarketConfigManager.toOffers(lvl, new BlackMarketFilter(null, java.util.Set.of(), null));
            java.util.UUID global = java.util.UUID.fromString("00000000-0000-0000-0000-000000000000");
            MerchantOffers target = MARKET_OFFERS.computeIfAbsent(global, id -> new MerchantOffers());
            target.clear();
            target.addAll(newOffers);
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    /** Reload JSON into a specific market (copy of config). */
    public static void reloadIntoMarket(net.minecraft.world.level.Level lvl, java.util.UUID marketId){
        try {
            BlackMarketConfigManager.reload();
            MerchantOffers newOffers = BlackMarketConfigManager.toOffers(lvl, new BlackMarketFilter(null, java.util.Set.of(), null));
            MerchantOffers target = MARKET_OFFERS.computeIfAbsent(marketId, id -> new MerchantOffers());
            target.clear();
            target.addAll(newOffers);
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    public static void registerAlias(java.util.UUID marketId, MerchantOffer offer, String alias){
        if (offer != null && alias != null){
            MARKET_ALIASES.computeIfAbsent(marketId, id -> new java.util.IdentityHashMap<>()).put(offer, alias);
        }
    }

    /** Backwards-compatible alias registration (global market). */
    @Deprecated
    public static void registerAlias(MerchantOffer offer, String alias){
        registerAlias(java.util.UUID.fromString("00000000-0000-0000-0000-000000000000"), offer, alias);
    }

    public static String aliasOf(MerchantOffer offer){
        // Search across all markets for this offer's alias
        for (var map : MARKET_ALIASES.values()){
            String a = map.get(offer);
            if (a != null) return a;
        }
        return "";
    }

    private static Item itemOf(String id){
        ResourceLocation rl = id.contains(":") ? ResourceLocation.tryParse(id) : ResourceLocation.fromNamespaceAndPath("minecraft", id);
        if (rl == null) return null;
        return ForgeRegistries.ITEMS.getValue(rl);
    }
} 
