package org.lupz.doomsdayessentials.event.eclipse.market;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.lupz.doomsdayessentials.EssentialsMod;

public final class MarketBlocks {
    private MarketBlocks(){}

    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, EssentialsMod.MOD_ID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, EssentialsMod.MOD_ID);

    public static final RegistryObject<Block> NIGHT_MARKET_BLOCK = BLOCKS.register("night_market_block",
            () -> new NightMarketBlock(net.minecraft.world.level.block.state.BlockBehaviour.Properties.of().strength(2.0f).noOcclusion()));

    public static final RegistryObject<BlockEntityType<NightMarketBlockEntity>> NIGHT_MARKET_BLOCK_ENTITY = BLOCK_ENTITIES.register("night_market_block_entity",
            () -> BlockEntityType.Builder.of(NightMarketBlockEntity::new, NIGHT_MARKET_BLOCK.get()).build(null));

    public static void register(IEventBus bus){
        BLOCKS.register(bus);
        BLOCK_ENTITIES.register(bus);
        // Block item registration via ModItems: fallback quick inline
        org.lupz.doomsdayessentials.item.ModItems.ITEMS.register("night_market_block", () -> new net.minecraft.world.item.BlockItem(NIGHT_MARKET_BLOCK.get(), new net.minecraft.world.item.Item.Properties()));
    }
} 