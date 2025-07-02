package org.lupz.doomsdayessentials.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class MedicalBedBlock extends BedBlock {

    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final EnumProperty<BedPart> PART = BlockStateProperties.BED_PART;

    // 9-pixel-tall cuboid (16×16×9)
    private static final VoxelShape SHAPE = Shapes.box(0, 0, 0, 1, 9f / 16f, 1);

    public MedicalBedBlock(Properties props) {
        super(DyeColor.WHITE, props);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(PART, BedPart.FOOT)
                .setValue(BlockStateProperties.OCCUPIED, Boolean.FALSE));
    }

    // ───────────────── Shape ─────────────────
    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPE;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    // ───────────────── Placement ─────────────────
    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        Direction dir = ctx.getHorizontalDirection();
        BlockPos pos = ctx.getClickedPos();
        Level level = ctx.getLevel();
        BlockPos headPos = pos.relative(dir.getClockWise());
        // second block must be replaceable
        if (!level.getBlockState(headPos).canBeReplaced(ctx)) {
            return null;
        }
        return this.defaultBlockState().setValue(FACING, dir).setValue(PART, BedPart.FOOT);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        Direction dir = state.getValue(FACING);
        BlockPos headPos = pos.relative(dir.getClockWise());
        level.setBlock(headPos, state.setValue(PART, BedPart.HEAD), 3);
    }

    // ───────────────── Breaking ─────────────────
    @Override
    public void playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide) {
            BedPart part = state.getValue(PART);
            Direction dir = state.getValue(FACING);
            BlockPos otherPos = (part == BedPart.FOOT) ? pos.relative(dir.getClockWise()) : pos.relative(dir.getCounterClockWise());
            BlockState otherState = level.getBlockState(otherPos);
            if (otherState.getBlock() == this && otherState.getValue(PART) != part) {
                level.setBlock(otherPos, Blocks.AIR.defaultBlockState(), 35);
                level.levelEvent(player, 2001, otherPos, Block.getId(otherState));
            }
        }
        super.playerWillDestroy(level, pos, state, player);
    }

    // ───────────────── Interaction (no sleeping) ─────────────────
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        // No special action; just pass-through so you can place items on it etc.
        return InteractionResult.PASS;
    }

    // ───────────────── State container ─────────────────
    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, PART, BlockStateProperties.OCCUPIED);
    }
} 