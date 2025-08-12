package org.lupz.doomsdayessentials.event.eclipse.market;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager.ControllerRegistrar;
import software.bernie.geckolib.util.GeckoLibUtil;

public class NightMarketEntity extends PathfinderMob implements GeoEntity {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public NightMarketEntity(EntityType<? extends PathfinderMob> type, Level level){
        super(type, level);
    }

    @Override
    protected void registerGoals(){
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new LookAtPlayerGoal(this, Player.class, 8.0f));
    }

    @Override
    public boolean isPersistenceRequired(){ return true; }

    @Override
    public Component getName(){
        return Component.literal("Mercador Noturno");
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand){
        if (!level().isClientSide && player instanceof ServerPlayer sp){
            sp.openMenu(new net.minecraft.world.MenuProvider() {
                @Override public Component getDisplayName(){ return Component.literal("Mercado Negro"); }
                @Override public AbstractContainerMenu createMenu(int id, net.minecraft.world.entity.player.Inventory inv, Player p){
                    return new NightMarketMenu(id, inv);
                }
            });
        }
        return InteractionResult.sidedSuccess(level().isClientSide);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }

    @Override
    public void registerControllers(ControllerRegistrar controllers) {
        // No animations yet
    }
} 