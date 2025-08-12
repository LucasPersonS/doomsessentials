package org.lupz.doomsdayessentials.event.eclipse.market;

import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.lupz.doomsdayessentials.EssentialsMod;

public final class NightMarketMenus {
    private NightMarketMenus(){}
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(ForgeRegistries.MENU_TYPES, EssentialsMod.MOD_ID);

    public static final RegistryObject<MenuType<NightMarketMenu>> NIGHT_MARKET_MENU = MENUS.register(
            "night_market",
            () -> IForgeMenuType.create((windowId, inv, data) -> new NightMarketMenu(windowId, inv)));

    public static void register(IEventBus bus){ MENUS.register(bus); }

    public static void clientSetup(FMLClientSetupEvent e){
        e.enqueueWork(() -> MenuScreens.register(NIGHT_MARKET_MENU.get(), NightMarketScreen::new));
    }
} 