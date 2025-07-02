package org.lupz.doomsdayessentials.block;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.lupz.doomsdayessentials.EssentialsMod;
import org.lupz.doomsdayessentials.item.ModItems;

import java.util.function.Supplier;

/**
 * Contains all custom block declarations/registrations for the mod.
 */
public final class ModBlocks {

    private ModBlocks() {}

    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, EssentialsMod.MOD_ID);

    public static final RegistryObject<Block> MEDICAL_BED = registerBlock("medical_bed",
            () -> new MedicalBedBlock(BlockBehaviour.Properties.copy(Blocks.WHITE_WOOL).noOcclusion()));

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, EssentialsMod.MOD_ID);

    public static final RegistryObject<BlockEntityType<MedicalBedBlockEntity>> MEDICAL_BED_BLOCK_ENTITY =
            BLOCK_ENTITIES.register("medical_bed_block_entity", () ->
                    BlockEntityType.Builder.of(MedicalBedBlockEntity::new, MEDICAL_BED.get()).build(null));

    private static <T extends Block> RegistryObject<T> registerBlock(String name, Supplier<T> block) {
        RegistryObject<T> toReturn = BLOCKS.register(name, block);
        registerBlockItem(name, toReturn);
        return toReturn;
    }

    private static <T extends Block> RegistryObject<Item> registerBlockItem(String name, RegistryObject<T> block) {
        return ModItems.ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties()));
    }

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
        BLOCK_ENTITIES.register(eventBus);
    }
} 