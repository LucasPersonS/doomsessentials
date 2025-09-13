package org.lupz.doomsdayessentials.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

public class HuntingBoardBlockEntity extends BlockEntity implements GeoBlockEntity {
	private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
	public HuntingBoardBlockEntity(BlockPos pos, BlockState state) {
		super(org.lupz.doomsdayessentials.block.ModBlocks.HUNTING_BOARD_BLOCK_ENTITY.get(), pos, state);
	}
	@Override
	public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {}
	@Override
	public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }
} 