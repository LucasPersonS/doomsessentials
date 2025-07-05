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
import org.lupz.doomsdayessentials.block.TotemBlock;
import org.lupz.doomsdayessentials.block.TotemTopBlock;
import org.lupz.doomsdayessentials.block.ReinforcedBlock;
import org.lupz.doomsdayessentials.block.ReinforcedBlockEntity;
import org.lupz.doomsdayessentials.block.MedicalBedBlock;
import org.lupz.doomsdayessentials.block.MedicalBedItem;

import java.util.function.Supplier;

/**
 * Contains all custom block declarations/registrations for the mod.
 */
public final class ModBlocks {

    private ModBlocks() {}

    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, EssentialsMod.MOD_ID);

    // Custom registration for Medical Bed with tooltip-aware item
    public static final RegistryObject<Block> MEDICAL_BED = BLOCKS.register("medical_bed",
            () -> new MedicalBedBlock(BlockBehaviour.Properties.copy(Blocks.WHITE_WOOL).noOcclusion()));

    public static final RegistryObject<Item> MEDICAL_BED_ITEM = ModItems.ITEMS.register("medical_bed",
            () -> new MedicalBedItem(MEDICAL_BED.get(), new Item.Properties()));

    public static final RegistryObject<Block> TOTEM_BLOCK = registerBlock("totem_block",
            () -> new TotemBlock(BlockBehaviour.Properties.copy(Blocks.OAK_PLANKS).strength(2.5F).noOcclusion()));

    // Upper part of the totem – no item form
    public static final RegistryObject<Block> TOTEM_BLOCK_TOP = BLOCKS.register("totem_block_top",
            () -> new TotemTopBlock(BlockBehaviour.Properties.copy(Blocks.OAK_PLANKS).strength(2.5F).noOcclusion()));

    // Reinforced construction blocks
    public static final RegistryObject<Block> REINFORCED_STONE = registerBlock("reinforced_stone",
            () -> new ReinforcedBlock(BlockBehaviour.Properties.copy(Blocks.STONE).strength(-1.0F, 3600000.0F)));

    public static final RegistryObject<Block> PRIMAL_STEEL_BLOCK = registerBlock("primal_steel_block",
            () -> new ReinforcedBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).strength(-1.0F, 3600000.0F)));

    public static final RegistryObject<Block> MOLTEN_STEEL_BLOCK = registerBlock("molten_steel_block",
            () -> new ReinforcedBlock(BlockBehaviour.Properties.copy(Blocks.NETHERITE_BLOCK).strength(-1.0F, 3600000.0F)));

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, EssentialsMod.MOD_ID);

    public static final RegistryObject<BlockEntityType<MedicalBedBlockEntity>> MEDICAL_BED_BLOCK_ENTITY =
            BLOCK_ENTITIES.register("medical_bed_block_entity", () ->
                    BlockEntityType.Builder.of(MedicalBedBlockEntity::new, MEDICAL_BED.get()).build(null));

    public static final RegistryObject<BlockEntityType<ReinforcedBlockEntity>> REINFORCED_BLOCK_ENTITY =
            BLOCK_ENTITIES.register("reinforced_block_entity", () ->
                    BlockEntityType.Builder.of(ReinforcedBlockEntity::new, REINFORCED_STONE.get(), PRIMAL_STEEL_BLOCK.get(), MOLTEN_STEEL_BLOCK.get()).build(null));

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