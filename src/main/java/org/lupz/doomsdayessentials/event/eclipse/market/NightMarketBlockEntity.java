package org.lupz.doomsdayessentials.event.eclipse.market;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager.ControllerRegistrar;
import software.bernie.geckolib.util.GeckoLibUtil;

public class NightMarketBlockEntity extends BlockEntity implements MenuProvider, GeoAnimatable {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public NightMarketBlockEntity(BlockPos pos, BlockState state){ super(MarketBlocks.NIGHT_MARKET_BLOCK_ENTITY.get(), pos, state); }

    @Override
    public Component getDisplayName(){ return Component.literal("Mercado Negro"); }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new NightMarketMenu(id, inv);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }

    @Override
    public void registerControllers(ControllerRegistrar controllers) {
        // no animations yet
    }

    @Override
    public double getTick(Object blockEntity) {
        return level != null ? level.getGameTime() : 0.0;
    }
} 