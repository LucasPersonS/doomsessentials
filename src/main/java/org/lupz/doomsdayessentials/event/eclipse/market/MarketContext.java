package org.lupz.doomsdayessentials.event.eclipse.market;

import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;

/**
 * Holds per-market runtime state: offers, alias mapping, active preset name,
 * and (future) edit sessions with undo/redo.
 */
public final class MarketContext {
    private final java.util.UUID marketId;
    private final MerchantOffers offers = new MerchantOffers();
    private final java.util.IdentityHashMap<MerchantOffer, String> aliases = new java.util.IdentityHashMap<>();
    private String activePreset = "";

    public MarketContext(java.util.UUID marketId){ this.marketId = marketId; }

    public java.util.UUID id(){ return marketId; }

    public MerchantOffers offers(){ return offers; }

    public void setActivePreset(String preset){ this.activePreset = preset==null?"":preset; }
    public String getActivePreset(){ return this.activePreset; }

    public void registerAlias(MerchantOffer offer, String alias){ if (offer!=null && alias!=null) aliases.put(offer, alias); }
    public String aliasOf(MerchantOffer offer){ return aliases.getOrDefault(offer, ""); }
}

