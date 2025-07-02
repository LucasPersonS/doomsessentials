package org.lupz.doomsdayessentials.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import org.lupz.doomsdayessentials.professions.ProfissaoManager;
import org.lupz.doomsdayessentials.injury.InjuryHelper;

public class MedicalBedBlock extends BaseEntityBlock {

    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final EnumProperty<BedPart> PART = BlockStateProperties.BED_PART;
    public static final BooleanProperty OCCUPIED = BlockStateProperties.OCCUPIED;
    private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 9, 16);

    public MedicalBedBlock(Properties props) {
        super(props);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(PART, BedPart.FOOT)
                .setValue(BlockStateProperties.OCCUPIED, Boolean.FALSE));
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, net.minecraft.world.InteractionHand hand, net.minecraft.world.phys.BlockHitResult hit) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (!(player instanceof ServerPlayer medico)) {
            return InteractionResult.FAIL;
        }

        if (!isMedico(medico)) {
            player.sendSystemMessage(Component.literal("§cOnly medics can use this bed."));
            return InteractionResult.SUCCESS;
        }

        BlockPos headPos = state.getValue(PART) == BedPart.HEAD ? pos : pos.relative(state.getValue(FACING));

        // Find nearest injured player within 5 blocks of the bed's head
        Player target = findNearestInjuredPlayer(medico, headPos, 5.0);

        if (target == null || !(target instanceof ServerPlayer targetPlayer)) {
            medico.sendSystemMessage(Component.translatable("profession.medico.no_target"));
            return InteractionResult.SUCCESS;
        }

        // Teleport target onto the bed
        targetPlayer.teleportTo(headPos.getX() + 0.5, headPos.getY() + 0.5, headPos.getZ() + 0.5);

        // Lock them to the bed
        targetPlayer.getPersistentData().putBoolean("healingBedLock", true);
        targetPlayer.getPersistentData().putLong("healingBedStartTime", level.getGameTime());
        targetPlayer.getPersistentData().putInt("healingBedX", headPos.getX());
        targetPlayer.getPersistentData().putInt("healingBedY", headPos.getY());
        targetPlayer.getPersistentData().putInt("healingBedZ", headPos.getZ());

        medico.sendSystemMessage(Component.translatable("profession.medico.bed.success", target.getDisplayName()));
        targetPlayer.sendSystemMessage(Component.translatable("profession.medico.bed.target", medico.getDisplayName()));

        // Set occupied state
        level.setBlock(headPos, level.getBlockState(headPos).setValue(OCCUPIED, true), 3);
        BlockPos footPos = headPos.relative(state.getValue(FACING).getOpposite());
        if(level.getBlockState(footPos).is(this)) {
            level.setBlock(footPos, level.getBlockState(footPos).setValue(OCCUPIED, true), 3);
        }

        return InteractionResult.SUCCESS;
    }

    private Player findNearestInjuredPlayer(Player source, BlockPos searchCenter, double range) {
        Player nearest = null;
        double nearestDistSq = Double.MAX_VALUE;
        AABB searchBox = new AABB(searchCenter).inflate(range);

        for (Player candidate : source.level().getEntitiesOfClass(Player.class, searchBox)) {
            if (candidate == source) continue; // Don't target self

            boolean isInjured = InjuryHelper.getCapability(candidate)
                    .map(cap -> cap.getInjuryLevel() > 0)
                    .orElse(false);

            if (isInjured) {
                double distSq = candidate.distanceToSqr(searchCenter.getX(), searchCenter.getY(), searchCenter.getZ());
                if (distSq < nearestDistSq) {
                    nearestDistSq = distSq;
                    nearest = candidate;
                }
            }
        }
        return nearest;
    }

    private boolean isMedico(Player player) {
        String profession = ProfissaoManager.getProfession(player.getUUID());
        return profession != null && profession.equalsIgnoreCase("medico");
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        Direction direction = ctx.getHorizontalDirection();
        BlockPos blockpos = ctx.getClickedPos();
        BlockPos blockpos1 = blockpos.relative(direction);
        return ctx.getLevel().getBlockState(blockpos1).canBeReplaced(ctx) ? this.defaultBlockState().setValue(FACING, direction) : null;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide) {
            BlockPos headPos = pos.relative(state.getValue(FACING));
            level.setBlock(headPos, state.setValue(PART, BedPart.HEAD), 3);
        }
    }

    @Override
    public void playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide) {
            BedPart bedpart = state.getValue(PART);
            BlockPos otherPos = (bedpart == BedPart.FOOT) ? pos.relative(state.getValue(FACING)) : pos.relative(state.getValue(FACING).getOpposite());
            BlockState otherState = level.getBlockState(otherPos);
            if (otherState.is(this) && otherState.getValue(PART) != bedpart) {
                level.setBlock(otherPos, Blocks.AIR.defaultBlockState(), 35);
                level.levelEvent(player, 2001, otherPos, Block.getId(otherState));
            }
        }
        super.playerWillDestroy(level, pos, state, player);
    }
    
    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public VoxelShape getShape(BlockState state, net.minecraft.world.level.BlockGetter level, BlockPos pos, net.minecraft.world.phys.shapes.CollisionContext ctx) {
        return SHAPE;
    }
    
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MedicalBedBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> net.minecraft.world.level.block.entity.BlockEntityTicker<T> getTicker(net.minecraft.world.level.Level level, BlockState state, net.minecraft.world.level.block.entity.BlockEntityType<T> type) {
        return null;
    }
    
    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, PART, OCCUPIED);
    }
} 