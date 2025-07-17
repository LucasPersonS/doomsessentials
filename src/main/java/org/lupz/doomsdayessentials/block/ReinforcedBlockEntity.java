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
    private float maxDurability;
    private long lastRegenTick = 0;

    public ReinforcedBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.REINFORCED_BLOCK_ENTITY.get(), pos, state);
        Block b = state.getBlock();
        if (b instanceof ReinforcedBlock rb) {
            this.maxDurability = rb.getInitialDurability();
        } else {
            this.maxDurability = 1f;
        }
        this.durability = 15f;
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putFloat("durability", durability);
        tag.putFloat("max", maxDurability);
        tag.putLong("lastRegen", lastRegenTick);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        durability = tag.getFloat("durability");
        maxDurability = tag.contains("max")?tag.getFloat("max"):maxDurability;
        lastRegenTick = tag.getLong("lastRegen");
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

    public void setDurability(float d){
        this.durability = Math.min(d, maxDurability);
        setChanged();
    }

    public static void tick(net.minecraft.world.level.Level level, BlockPos pos, BlockState state, ReinforcedBlockEntity be){
        if(level.isClientSide) return;
        long gameTime = level.getGameTime();
        if(be.durability < be.maxDurability){
            if(gameTime - be.lastRegenTick >= 18000){ // 15 min
                be.durability = Math.min(be.durability + 10f, be.maxDurability);
                be.lastRegenTick = gameTime;
                be.setChanged();
            }
        } else {
            be.lastRegenTick = gameTime;
        }
    }
} 