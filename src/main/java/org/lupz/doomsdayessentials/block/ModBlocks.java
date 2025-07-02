package org.lupz.doomsdayessentials.block;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.lupz.doomsdayessentials.EssentialsMod;

/**
 * Contains all custom block declarations/registrations for the mod.
 */
public final class ModBlocks {

    private ModBlocks() {}

    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, EssentialsMod.MOD_ID);

    public static final RegistryObject<Block> MEDICAL_BED = BLOCKS.register("medical_bed", () ->
            new MedicalBedBlock(BlockBehaviour.Properties.copy(Blocks.WHITE_WOOL).noOcclusion()));

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
    }
} 