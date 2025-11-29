package org.lupz.doomsdayessentials.kit.menu;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.lupz.doomsdayessentials.EssentialsMod;

public final class KitMenus {
    private static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(ForgeRegistries.MENU_TYPES,
            EssentialsMod.MOD_ID);

    public static final RegistryObject<MenuType<KitMenu>> KIT_MENU = MENUS.register("kit_menu",
            () -> IForgeMenuType.create((windowId, inv, data) -> {
                String cooldownTypeId = data.readUtf();
                org.lupz.doomsdayessentials.kit.Kit.CooldownType cooldownType = org.lupz.doomsdayessentials.kit.Kit.CooldownType
                        .fromId(cooldownTypeId);
                if (cooldownType == null)
                    cooldownType = org.lupz.doomsdayessentials.kit.Kit.CooldownType.DAILY;
                return new KitMenu(windowId, inv, cooldownType);
            }));

    public static void register(IEventBus bus) {
        MENUS.register(bus);
    }
}
