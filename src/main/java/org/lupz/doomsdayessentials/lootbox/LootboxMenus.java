package org.lupz.doomsdayessentials.lootbox;

import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.lupz.doomsdayessentials.EssentialsMod;

public final class LootboxMenus {
	private LootboxMenus() {}

	public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(ForgeRegistries.MENU_TYPES, EssentialsMod.MOD_ID);

	public static final RegistryObject<MenuType<LootboxMenu>> LOOTBOX_MENU = MENUS.register("lootbox_menu",
			() -> IForgeMenuType.create((windowId, inv, data) -> {
				String rarity = data != null ? data.readUtf() : LootboxManager.R_INCOMUM;
				java.util.List<net.minecraft.world.item.ItemStack> pool = new java.util.ArrayList<>();
				if (data != null && data.isReadable()) {
					int size = data.readVarInt();
					for (int i = 0; i < size; i++) pool.add(data.readItem());
				}
				if (pool.isEmpty()) pool = LootboxManager.getAllAsStacks(rarity);
				return new LootboxMenu(windowId, inv, rarity, pool);
			}));

	public static void register(IEventBus bus) { MENUS.register(bus); }
} 