package org.lupz.doomsdayessentials.block;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.lupz.doomsdayessentials.block.ModBlocks;

/**
 * Stores durability for a single reinforced block.
 */
public class ReinforcedBlockEntity extends BlockEntity {
    private float durability;

    public ReinforcedBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.REINFORCED_BLOCK_ENTITY.get(), pos, state);
        Block b = state.getBlock();
        if (b instanceof ReinforcedBlock rb) {
            this.durability = rb.getInitialDurability();
        } else {
            this.durability = 1f;
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putFloat("durability", durability);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        durability = tag.getFloat("durability");
    }

    public float getDurability() {
        return durability;
    }

    public void decreaseDurability(float amount) {
        durability -= amount;
        if (durability <= 0) {
            if (level != null) level.destroyBlock(worldPosition, false);
        } else setChanged();
    }
} 