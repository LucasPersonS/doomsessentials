package org.lupz.doomsdayessentials.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.network.NetworkHooks;
import org.lupz.doomsdayessentials.professions.menu.BountyBoardMenuProvider;

public class HuntingBoardBlock extends HorizontalDirectionalBlock implements EntityBlock {
	public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
	
	// Pixel-perfect hitbox based on hunting_board.json model bounds
	// Model spans X[-16.3,15.7] (32 pixels wide), Y[6.7,30], Z[-2,3] in model units
	// Board is 2 blocks wide, thin depth, wall-mounted
	private static final VoxelShape SHAPE_NORTH = Shapes.box(
		(-16.3+8)/16.0, 6.7/16.0, (-2+8)/16.0,  // min: -8.3, 6.7, 6
		(15.7+8)/16.0, 30.0/16.0, (3+8)/16.0    // max: 23.7, 30, 11
	);
	private static final VoxelShape SHAPE_SOUTH = Shapes.box(
		(-16.3+8)/16.0, 6.7/16.0, (13-3)/16.0,
		(15.7+8)/16.0, 30.0/16.0, (13+2)/16.0
	);
	private static final VoxelShape SHAPE_WEST = Shapes.box(
		(-2+8)/16.0, 6.7/16.0, (-16.3+8)/16.0,
		(3+8)/16.0, 30.0/16.0, (15.7+8)/16.0
	);
	private static final VoxelShape SHAPE_EAST = Shapes.box(
		(13-2)/16.0, 6.7/16.0, (-16.3+8)/16.0,
		(13+3)/16.0, 30.0/16.0, (15.7+8)/16.0
	);

	public HuntingBoardBlock(Properties props) { super(props.noOcclusion()); this.registerDefaultState(this.stateDefinition.any().setValue(FACING, net.minecraft.core.Direction.NORTH)); }

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext ctx) {
		return this.defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite());
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING);
	}

	@Override
	public BlockState rotate(BlockState state, Rotation rot) { return state.setValue(FACING, rot.rotate(state.getValue(FACING))); }
	@Override
	public BlockState mirror(BlockState state, Mirror mirror) { return rotate(state, mirror.getRotation(state.getValue(FACING))); }

	@Override
	public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
		if (!level.isClientSide && player instanceof net.minecraft.server.level.ServerPlayer sp) {
			NetworkHooks.openScreen(sp, new BountyBoardMenuProvider());
			return InteractionResult.SUCCESS;
		}
		return InteractionResult.CONSUME;
	}

	@Override
	public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return switch (state.getValue(FACING)) {
			case NORTH -> SHAPE_NORTH;
			case SOUTH -> SHAPE_SOUTH;
			case WEST -> SHAPE_WEST;
			case EAST -> SHAPE_EAST;
			default -> SHAPE_NORTH;
		};
	}

	@Override
	public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return getShape(state, level, pos, context);
	}

	@Override
	public net.minecraft.world.level.block.RenderShape getRenderShape(BlockState state) {
		return net.minecraft.world.level.block.RenderShape.ENTITYBLOCK_ANIMATED;
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new HuntingBoardBlockEntity(pos, state);
	}
} 