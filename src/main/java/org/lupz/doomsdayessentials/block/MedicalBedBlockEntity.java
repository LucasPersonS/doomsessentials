package org.lupz.doomsdayessentials.block;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.lupz.doomsdayessentials.config.EssentialsConfig;
import org.lupz.doomsdayessentials.injury.InjuryHelper;

import java.util.UUID;

public class MedicalBedBlockEntity extends BlockEntity {

    private UUID restingPlayerUUID;
    private long healingFinishTime;

    public MedicalBedBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.MEDICAL_BED_BLOCK_ENTITY.get(), pos, state);
    }

    public void startHealing(ServerPlayer player) {
        this.restingPlayerUUID = player.getUUID();
        long healingTimeMillis = EssentialsConfig.HEALING_BED_TIME_MINUTES.get() * 60 * 1000;
        this.healingFinishTime = System.currentTimeMillis() + healingTimeMillis;
        setChanged();
    }

    public static void tick(Level level, BlockPos pos, BlockState state, MedicalBedBlockEntity blockEntity) {
        if (level.isClientSide || blockEntity.restingPlayerUUID == null) {
            return;
        }

        if (System.currentTimeMillis() >= blockEntity.healingFinishTime) {
            ServerPlayer player = (ServerPlayer) level.getPlayerByUUID(blockEntity.restingPlayerUUID);
            if (player != null) {
                InjuryHelper.getCapability(player).ifPresent(cap -> {
                    if (cap.getInjuryLevel() > 0) {
                        cap.setInjuryLevel(cap.getInjuryLevel() - 1);
                        player.sendSystemMessage(Component.literal("§aVocê se sente melhor e suas feridas foram tratadas."));
                    }
                });
            }
            blockEntity.stopHealing();
        }
    }
    
    public void stopHealing() {
        this.restingPlayerUUID = null;
        this.healingFinishTime = 0;
        level.setBlock(worldPosition, getBlockState().setValue(MedicalBedBlock.OCCUPIED, false), 3);
        setChanged();
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        if (restingPlayerUUID != null) {
            tag.putUUID("RestingPlayerUUID", restingPlayerUUID);
            tag.putLong("HealingFinishTime", healingFinishTime);
        }
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.hasUUID("RestingPlayerUUID")) {
            this.restingPlayerUUID = tag.getUUID("RestingPlayerUUID");
            this.healingFinishTime = tag.getLong("HealingFinishTime");
        }
    }
}
