package org.lupz.doomsdayessentials.event.eclipse.market;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class NightMarketBlock extends BaseEntityBlock {
    private static final VoxelShape SHAPE = Block.box(-8, 0, -8, 24, 32, 24);

    public NightMarketBlock(Properties props){ super(props); }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new NightMarketBlockEntity(pos, state);
    }

    @Override
    public VoxelShape getShape(BlockState state, net.minecraft.world.level.BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, net.minecraft.world.level.BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide && player instanceof ServerPlayer sp) {
            MerchantOffers offers = NightMarketManager.getOffers(level);
            SimpleMerchantImpl merchant = new SimpleMerchantImpl(Component.literal("Mercado Negro"), offers);
            merchant.setTradingPlayer(sp);
            sp.openMenu(new net.minecraft.world.MenuProvider() {
                @Override public Component getDisplayName() { return Component.literal("Mercado Negro"); }
                @Override public AbstractContainerMenu createMenu(int containerId, net.minecraft.world.entity.player.Inventory inv, Player p) {
                    return new MerchantMenu(containerId, inv, merchant);
                }
            });
            return InteractionResult.CONSUME;
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
} 