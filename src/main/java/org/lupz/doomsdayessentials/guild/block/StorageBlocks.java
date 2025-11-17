package org.lupz.doomsdayessentials.guild.block;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.lupz.doomsdayessentials.EssentialsMod;

public final class StorageBlocks {
    private StorageBlocks() {}

    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, EssentialsMod.MOD_ID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, EssentialsMod.MOD_ID);

    public static final RegistryObject<Block> STORAGE_BLOCK = BLOCKS.register("storage_block",
            () -> new StorageBlock(net.minecraft.world.level.block.state.BlockBehaviour.Properties.of().strength(50.0f, 1200.0f).noOcclusion()));

    public static final RegistryObject<BlockEntityType<StorageBlockEntity>> STORAGE_BLOCK_ENTITY = BLOCK_ENTITIES.register("storage_block_entity",
            () -> BlockEntityType.Builder.of(StorageBlockEntity::new, STORAGE_BLOCK.get()).build(null));

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
        BLOCK_ENTITIES.register(bus);
        // Register block item with GeckoLib renderer
        org.lupz.doomsdayessentials.item.ModItems.ITEMS.register("storage_block", 
            () -> new org.lupz.doomsdayessentials.item.StorageBlockItem(STORAGE_BLOCK.get(), new net.minecraft.world.item.Item.Properties()));
    }
}
