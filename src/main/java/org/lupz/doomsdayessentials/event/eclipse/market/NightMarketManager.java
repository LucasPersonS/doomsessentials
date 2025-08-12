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
    private static final MerchantOffers OFFERS = new MerchantOffers();

    private NightMarketManager(){}

    public static MerchantOffers getOffers(Level lvl){
        // Return a copy so GUI snapshot is stable
        MerchantOffers copy = new MerchantOffers();
        copy.addAll(OFFERS);
        return copy;
    }

    public static boolean addOffer(String buy1Id, int buy1Count, String buy2Id, int buy2Count, String sellId, int sellCount, int maxUses, int xp, float priceMult){
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
        OFFERS.add(offer);
        return true;
    }

    public static boolean clear(){ OFFERS.clear(); return true; }

    private static Item itemOf(String id){
        ResourceLocation rl = id.contains(":") ? ResourceLocation.tryParse(id) : ResourceLocation.fromNamespaceAndPath("minecraft", id);
        if (rl == null) return null;
        return ForgeRegistries.ITEMS.getValue(rl);
    }
} 