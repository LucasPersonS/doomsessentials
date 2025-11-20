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
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import org.lupz.doomsdayessentials.event.eclipse.market.admin.NightMarketAdminOpenPacket;
import org.lupz.doomsdayessentials.network.PacketHandler;
import org.lupz.doomsdayessentials.event.eclipse.market.MarketPresetManager;

public class NightMarketBlock extends BaseEntityBlock {
    // Pixel-perfect hitbox based on night_market.geo.json model bounds
    // Model coordinates: origin at center (0,0,0). Convert by: (modelX + 8)/16, modelY/16, (modelZ + 8)/16
    // Note: Model is multi-block (6 blocks wide). Focusing on main interactable base/tent area.
    private static final VoxelShape SHAPE = Shapes.or(
        // Main floor/base structures from origin[-20,0,-21] to [20,12,17]
        Shapes.box((-20+8)/16.0, 0, (-21+8)/16.0, (20+8)/16.0, 12/16.0, (17+8)/16.0),
        // Main tent canopy for interaction
        Shapes.box((-19+8)/16.0, 9/16.0, (-8+8)/16.0, (14+8)/16.0, 17/16.0, (8+8)/16.0)
    );

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
            BlockEntity be = level.getBlockEntity(pos);
            java.util.UUID marketId = be instanceof NightMarketBlockEntity nbe ? nbe.getMarketId() : java.util.UUID.fromString("00000000-0000-0000-0000-000000000000");
            MerchantOffers offers = NightMarketManager.getOffersMutable(marketId);
            sp.displayClientMessage(Component.literal("Market " + marketId + " offers=" + offers.size()), false);
            // If player is sneaking and has permission level >= 2 (OP), open Admin UI instead
            if (player.isShiftKeyDown() && sp.hasPermissions(2)) {
                String json = MarketPresetManager.exportOffersToJson(level, offers);
                String activePreset = (be instanceof NightMarketBlockEntity nbe2) ? nbe2.getActivePreset() : "";
                PacketHandler.CHANNEL.send(net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> sp),
                        new NightMarketAdminOpenPacket(marketId, pos, json, activePreset));
                return InteractionResult.CONSUME;
            }
            SimpleMerchantImpl merchant = new SimpleMerchantImpl(Component.literal("Mercado Negro"), offers);
            merchant.setTradingPlayer(sp);
            java.util.OptionalInt opened = sp.openMenu(new net.minecraft.world.MenuProvider() {
                @Override public Component getDisplayName() { return Component.literal("Mercado Negro"); }
                @Override public AbstractContainerMenu createMenu(int containerId, net.minecraft.world.entity.player.Inventory inv, Player p) {
                    return new MerchantMenu(containerId, inv, merchant);
                }
            });
            if (opened.isPresent()) {
                sp.sendMerchantOffers(opened.getAsInt(), offers, 0, merchant.getVillagerXp(), merchant.showProgressBar(), false);
            }
            return InteractionResult.CONSUME;
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
