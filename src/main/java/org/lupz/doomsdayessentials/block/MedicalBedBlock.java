package org.lupz.doomsdayessentials.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import org.lupz.doomsdayessentials.professions.ProfissaoManager;

import java.util.List;

public class MedicalBedBlock extends BaseEntityBlock {

    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final EnumProperty<BedPart> PART = BlockStateProperties.BED_PART;
    public static final BooleanProperty OCCUPIED = BlockStateProperties.OCCUPIED;
    private static final VoxelShape SHAPE = Shapes.box(0, 0, 0, 1, 9f / 16f, 1);

    public MedicalBedBlock(Properties props) {
        super(props);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(PART, BedPart.FOOT)
                .setValue(BlockStateProperties.OCCUPIED, Boolean.FALSE));
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) {
            return InteractionResult.CONSUME;
        }

        BlockPos headPos = state.getValue(PART) == BedPart.HEAD ? pos : pos.relative(state.getValue(FACING));
        BlockState headState = level.getBlockState(headPos);

        if (headState.getValue(BlockStateProperties.OCCUPIED)) {
            player.sendSystemMessage(Component.literal("§cThis bed is already occupied."));
            return InteractionResult.SUCCESS;
        }

        if (!isMedico(player)) {
            player.sendSystemMessage(Component.literal("§cOnly medics can use this bed."));
            return InteractionResult.SUCCESS;
        }

        List<Player> nearbyPlayers = level.getEntitiesOfClass(Player.class, new AABB(pos).inflate(5), p -> p != player);
        if (nearbyPlayers.isEmpty()) {
            player.sendSystemMessage(Component.literal("§cNo injured player nearby to treat."));
            return InteractionResult.SUCCESS;
        }

        Player targetPlayer = nearbyPlayers.get(0); // For now, just take the first one found
        targetPlayer.startSleeping(headPos);
        level.setBlock(headPos, headState.setValue(BlockStateProperties.OCCUPIED, true), 3);

        BlockEntity blockEntity = level.getBlockEntity(headPos);
        if (blockEntity instanceof MedicalBedBlockEntity) {
            ((MedicalBedBlockEntity) blockEntity).startHealing((ServerPlayer) targetPlayer);
            player.sendSystemMessage(Component.literal("§aYou have put " + targetPlayer.getName().getString() + " to rest."));
            targetPlayer.sendSystemMessage(Component.literal("§aA medic is treating your wounds."));
        }

        return InteractionResult.SUCCESS;
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
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPE;
    }
    
    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MedicalBedBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, ModBlocks.MEDICAL_BED_BLOCK_ENTITY.get(), MedicalBedBlockEntity::tick);
    }
    
    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, PART, OCCUPIED);
    }
} 