package org.lupz.doomsdayessentials.killlog;

import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.lupz.doomsdayessentials.EssentialsMod;

public final class KillLogMenus {
	private KillLogMenus() {}

	public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(ForgeRegistries.MENU_TYPES, EssentialsMod.MOD_ID);

	public static final RegistryObject<MenuType<org.lupz.doomsdayessentials.killlog.menu.KillLogMenu>> KILLLOG_MENU = MENUS.register(
		"killlog_menu",
		() -> IForgeMenuType.create((windowId, inv, data) -> {
			if (data != null && data.isReadable()) {
				return new org.lupz.doomsdayessentials.killlog.menu.KillLogMenu(windowId, inv, data);
			}
			return new org.lupz.doomsdayessentials.killlog.menu.KillLogMenu(windowId, inv);
		})
	);

	public static void register(IEventBus bus) { MENUS.register(bus); }
} 