package org.lupz.doomsdayessentials.item;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.BlockItem;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.lupz.doomsdayessentials.EssentialsMod;
import org.lupz.doomsdayessentials.block.ModBlocks;
import org.lupz.doomsdayessentials.item.CrownItem;

/**
 * General item registry for the mod. Use this for block items or misc items that do not belong in another subsystem.
 */
public final class ModItems {

    private ModItems() {}

    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, EssentialsMod.MOD_ID);

    public static final RegistryObject<CrownItem> CROWN = ITEMS.register("crown", () ->
            new CrownItem(new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> SCRAPMETAL = ITEMS.register("scrapmetal", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> METAL_FRAGMENTS = ITEMS.register("metal_fragments", () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> METALBLADE = ITEMS.register("metalblade", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> SHEETMETAL = ITEMS.register("sheetmetal", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> GEARS = ITEMS.register("gears", () -> new Item(new Item.Properties()));

    public static void register(IEventBus bus) {
        ITEMS.register(bus);
    }
} 