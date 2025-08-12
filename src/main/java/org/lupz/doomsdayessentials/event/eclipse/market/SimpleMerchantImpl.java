package org.lupz.doomsdayessentials.event.eclipse.market;

import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.Merchant;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;

public class SimpleMerchantImpl implements Merchant {
    private final Component name;
    private final MerchantOffers offers;
    private Player tradingPlayer;
    private int villagerXp;
    private final boolean clientSide;

    public SimpleMerchantImpl(Component name, MerchantOffers offers){
        this(name, offers, false);
    }

    public SimpleMerchantImpl(Component name, MerchantOffers offers, boolean clientSide){
        this.name = name;
        this.offers = offers;
        this.clientSide = clientSide;
    }

    public Component getName(){ return name; }

    @Override
    public void setTradingPlayer(Player player) { this.tradingPlayer = player; }

    @Override
    public Player getTradingPlayer() { return tradingPlayer; }

    @Override
    public MerchantOffers getOffers() { return offers; }

    @Override
    public void overrideOffers(MerchantOffers newOffers) { this.offers.clear(); this.offers.addAll(newOffers); }

    @Override
    public void notifyTrade(MerchantOffer offer) { villagerXp += offer.getXp(); }

    @Override
    public void notifyTradeUpdated(ItemStack stack) {}

    @Override
    public int getVillagerXp() { return villagerXp; }

    @Override
    public void overrideXp(int xp) { this.villagerXp = xp; }

    @Override
    public SoundEvent getNotifyTradeSound() { return SoundEvents.VILLAGER_YES; }

    @Override
    public boolean isClientSide() { return clientSide; }

    @Override
    public boolean showProgressBar() { return false; }
} 