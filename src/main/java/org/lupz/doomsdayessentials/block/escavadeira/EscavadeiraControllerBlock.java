package org.lupz.doomsdayessentials.block.escavadeira;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lupz.doomsdayessentials.blockentity.EscavadeiraControllerBlockEntity;
import org.lupz.doomsdayessentials.registry.EscavadeiraRegistries;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class EscavadeiraControllerBlock extends BaseEntityBlock implements EntityBlock {
	private static final VoxelShape SHAPE = Shapes.box(-0.5, 0.0, -0.5, 1.5, 2.8, 1.5); // 2x2 footprint, ~2.8 high

	public EscavadeiraControllerBlock(Properties properties) {
		super(properties);
	}

	@Override
	public @NotNull InteractionResult use(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos, @NotNull Player player, @NotNull InteractionHand hand, @NotNull BlockHitResult hit) {
		if (!level.isClientSide) {
			BlockEntity be = level.getBlockEntity(pos);
			if (be instanceof MenuProvider provider && player instanceof ServerPlayer sp) {
				NetworkHooks.openScreen(sp, provider, pos);
			}
		}
		return InteractionResult.sidedSuccess(level.isClientSide);
	}

	@Override
	public net.minecraft.world.level.block.RenderShape getRenderShape(@NotNull BlockState state) {
		return net.minecraft.world.level.block.RenderShape.INVISIBLE;
	}

	@Override
	public @NotNull VoxelShape getShape(@NotNull BlockState state, @NotNull BlockGetter level, @NotNull BlockPos pos, @NotNull CollisionContext context) {
		return SHAPE;
	}

	@Override
	public @NotNull VoxelShape getCollisionShape(@NotNull BlockState state, @NotNull BlockGetter level, @NotNull BlockPos pos, @NotNull CollisionContext context) {
		return SHAPE;
	}

	@Override
	public @Nullable BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
		return new EscavadeiraControllerBlockEntity(pos, state);
	}

	@Override
	public @Nullable <T extends BlockEntity> BlockEntityTicker<T> getTicker(@NotNull Level level, @NotNull BlockState state, @NotNull BlockEntityType<T> type) {
		return level.isClientSide ? null : BaseEntityBlock.createTickerHelper(type, EscavadeiraRegistries.ESCAVADEIRA_CONTROLLER_BE, EscavadeiraControllerBlockEntity::serverTick);
	}
} 