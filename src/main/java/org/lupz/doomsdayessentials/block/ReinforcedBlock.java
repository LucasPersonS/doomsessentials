package org.lupz.doomsdayessentials.block;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;
import org.lupz.doomsdayessentials.guild.GuildConfig;

/**
 * Reinforced building block with configurable durability that can be damaged
 * during wars (damage logic to be added later).
 */
public class ReinforcedBlock extends BaseEntityBlock {

    public ReinforcedBlock(BlockBehaviour.Properties props) {
        super(props);
    }

    /** Returns the initial durability pulled from config. */
    public float getInitialDurability() {
        if (this == ModBlocks.REINFORCED_STONE.get()) {
            return GuildConfig.REINFORCED_STONE_DURABILITY.get().floatValue();
        } else if (this == ModBlocks.PRIMAL_STEEL_BLOCK.get()) {
            return GuildConfig.PRIMAL_STEEL_DURABILITY.get().floatValue();
        } else if (this == ModBlocks.MOLTEN_STEEL_BLOCK.get()) {
            return GuildConfig.MOLTEN_STEEL_DURABILITY.get().floatValue();
        }
        return 1.0f;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ReinforcedBlockEntity(pos, state);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide) {
            if (!player.getItemInHand(hand).is(net.minecraft.world.item.Items.STICK)) {
                return InteractionResult.PASS;
            }
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof ReinforcedBlockEntity rbe) {
                float durability = rbe.getDurability();
                player.displayClientMessage(Component.literal(String.format("Durabilidade: %.1f", durability)), true);
            }
        }
        return InteractionResult.SUCCESS;
    }
} 