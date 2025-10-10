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

	// GUI-only items for navigation
	public static final RegistryObject<Item> GUI_BACK = ITEMS.register("gui_back", () -> new Item(new Item.Properties()));
	public static final RegistryObject<Item> GUI_NEXT = ITEMS.register("gui_next", () -> new Item(new Item.Properties()));
	public static final RegistryObject<Item> GUI_REWARD = ITEMS.register("gui_reward", () -> new Item(new Item.Properties()));

	// Lootbox items and keys
	public static final RegistryObject<LootboxItem> LOOTBOX_INCOMUM = ITEMS.register("lootbox_incomum", () -> new LootboxItem(new Item.Properties().stacksTo(16), "incomum"));
	public static final RegistryObject<LootboxItem> LOOTBOX_RARA = ITEMS.register("lootbox_rara", () -> new LootboxItem(new Item.Properties().stacksTo(16), "rara"));
	public static final RegistryObject<LootboxItem> LOOTBOX_EPICA = ITEMS.register("lootbox_epica", () -> new LootboxItem(new Item.Properties().stacksTo(16), "epica"));
	public static final RegistryObject<LootboxItem> LOOTBOX_LENDARIA = ITEMS.register("lootbox_lendaria", () -> new LootboxItem(new Item.Properties().stacksTo(16), "lendaria"));

	public static final RegistryObject<LootboxKeyItem> KEY_INCOMUM = ITEMS.register("key_incomum", () -> new LootboxKeyItem(new Item.Properties().stacksTo(64), "incomum"));
	public static final RegistryObject<LootboxKeyItem> KEY_RARA = ITEMS.register("key_rara", () -> new LootboxKeyItem(new Item.Properties().stacksTo(64), "rara"));
	public static final RegistryObject<LootboxKeyItem> KEY_EPICA = ITEMS.register("key_epica", () -> new LootboxKeyItem(new Item.Properties().stacksTo(64), "epica"));
	public static final RegistryObject<LootboxKeyItem> KEY_LENDARIA = ITEMS.register("key_lendaria", () -> new LootboxKeyItem(new Item.Properties().stacksTo(64), "lendaria"));

	public static void register(IEventBus bus) {
		ITEMS.register(bus);
	}
} 