package org.lupz.doomsdayessentials.professions.items;

import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.lupz.doomsdayessentials.EssentialsMod;
import org.lupz.doomsdayessentials.professions.items.EngineerHammerItem;

public final class ProfessionItems {
    private ProfessionItems() {}

    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, EssentialsMod.MOD_ID);

    public static final RegistryObject<Item> TRACKING_COMPASS = ITEMS.register("tracking_compass",
            () -> new TrackingCompassItem(new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> ENGINEER_HAMMER = ITEMS.register("engineer_hammer",
            () -> new EngineerHammerItem(new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> HUNTING_BOARD = ITEMS.register("hunting_board",
            () -> new HuntingBoardItem(new Item.Properties().stacksTo(1)));

    public static void register(IEventBus bus) {
        ITEMS.register(bus);
    }
} 