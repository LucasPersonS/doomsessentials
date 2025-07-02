package org.lupz.doomsdayessentials.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import java.util.UUID;

public class MedicalBedBlockEntity extends BlockEntity {

    public MedicalBedBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.MEDICAL_BED_BLOCK_ENTITY.get(), pos, state);
    }
}
