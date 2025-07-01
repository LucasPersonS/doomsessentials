package org.lupz.doomsdayessentials.professions.menu;

import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.lupz.doomsdayessentials.EssentialsMod;

/**
 * Holds menu type registrations for the profession GUI(s).
 */
public final class ProfessionMenuTypes {

    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(ForgeRegistries.MENU_TYPES, EssentialsMod.MOD_ID);

    public static final RegistryObject<MenuType<ProfissoesMenu>> PROFISSOES_MENU = MENUS.register(
            "profissoes_menu",
            () -> IForgeMenuType.create((windowId, inv, data) -> new ProfissoesMenu(windowId, inv, data)));

    private ProfessionMenuTypes() {}

    public static void register(IEventBus eventBus) {
        MENUS.register(eventBus);
    }
} 