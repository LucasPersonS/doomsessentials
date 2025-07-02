package org.lupz.doomsdayessentials.item;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.BlockItem;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.lupz.doomsdayessentials.EssentialsMod;
import org.lupz.doomsdayessentials.block.ModBlocks;

/**
 * General item registry for the mod. Use this for block items or misc items that do not belong in another subsystem.
 */
public final class ModItems {

    private ModItems() {}

    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, EssentialsMod.MOD_ID);

    // ADD_MEDICAL_BED_ITEM
    public static final RegistryObject<Item> MEDICAL_BED = ITEMS.register("medical_bed", () ->
            new BlockItem(ModBlocks.MEDICAL_BED.get(), new Item.Properties()));

    public static void register(IEventBus bus) {
        ITEMS.register(bus);
    }
} 