package org.lupz.doomsdayessentials.guild.block;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
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
import org.lupz.doomsdayessentials.guild.Guild;
import org.lupz.doomsdayessentials.guild.GuildsManager;

public class StorageBlock extends BaseEntityBlock {
    // Pixel-perfect hitbox based on storage.geo.json model bounds
    // Model coordinates: origin at center (0,0,0). Convert by: (modelX + 8)/16, modelY/16, (modelZ + 8)/16
    private static final VoxelShape SHAPE = Shapes.or(
        // Main chest body: origin[-5,2,-5] size[10,20,10]
        Shapes.box(3/16.0, 2/16.0, 3/16.0, 13/16.0, 22/16.0, 13/16.0),
        // Four legs at corners
        Shapes.box(3.3/16.0, 0, 3.3/16.0, 5.8/16.0, 2/16.0, 5.3/16.0),
        Shapes.box(3.3/16.0, 0, 10.3/16.0, 5.8/16.0, 2/16.0, 12.3/16.0),
        Shapes.box(10.2/16.0, 0, 10.3/16.0, 12.7/16.0, 2/16.0, 12.3/16.0),
        Shapes.box(10.2/16.0, 0, 3.3/16.0, 12.7/16.0, 2/16.0, 5.3/16.0)
    );

    public StorageBlock(Properties props) {
        super(props);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new StorageBlockEntity(pos, state);
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
            ServerLevel serverLevel = sp.serverLevel();
            GuildsManager gm = GuildsManager.get(serverLevel);
            Guild guild = gm.getGuildByMember(sp.getUUID());
            
            if (guild == null) {
                sp.sendSystemMessage(Component.literal("§cVocê não pertence a uma organização."));
                return InteractionResult.FAIL;
            }
            
            // Open guild storage menu
            sp.openMenu(new net.minecraft.world.SimpleMenuProvider(
                (id, inv, p) -> new org.lupz.doomsdayessentials.guild.menu.GuildStorageMenu(id, inv, 0),
                Component.literal("Cofre da Organização"))
            );
            return InteractionResult.CONSUME;
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
