package org.lupz.doomsdayessentials.professions.menu;

import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.lupz.doomsdayessentials.EssentialsMod;
import org.lupz.doomsdayessentials.professions.menu.EngineerCraftMenu;

/**
 * Holds menu type registrations for the profession GUI(s).
 */
public final class ProfessionMenuTypes {

    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(ForgeRegistries.MENU_TYPES, EssentialsMod.MOD_ID);

    public static final RegistryObject<MenuType<ProfissoesMenu>> PROFISSOES_MENU = MENUS.register(
            "profissoes_menu",
            () -> IForgeMenuType.create((windowId, inv, data) -> new ProfissoesMenu(windowId, inv, data)));

    public static final RegistryObject<MenuType<org.lupz.doomsdayessentials.professions.menu.ShopMenu>> SHOP_MENU = MENUS.register(
            "shop_menu",
            () -> IForgeMenuType.create((windowId, inv, data) -> new org.lupz.doomsdayessentials.professions.menu.ShopMenu(windowId, inv)));

    public static final RegistryObject<MenuType<EngineerCraftMenu>> ENGINEER_CRAFT = MENUS.register(
            "engineer_craft_menu",
            () -> IForgeMenuType.create((windowId, inv, data) -> new EngineerCraftMenu(windowId, inv)));

    public static final RegistryObject<MenuType<org.lupz.doomsdayessentials.professions.menu.MedicRewardMenu>> MEDIC_REWARD_MENU = MENUS.register(
            "medic_reward_menu",
            () -> IForgeMenuType.create((windowId, inv, data) -> new org.lupz.doomsdayessentials.professions.menu.MedicRewardMenu(windowId, inv)));

    public static final RegistryObject<MenuType<org.lupz.doomsdayessentials.territory.menu.TerritoryRewardMenu>> TERRITORY_REWARD_MENU = MENUS.register(
            "territory_reward_menu",
            () -> IForgeMenuType.create((windowId, inv, data) -> new org.lupz.doomsdayessentials.territory.menu.TerritoryRewardMenu(windowId, inv)));

    public static final RegistryObject<MenuType<org.lupz.doomsdayessentials.menu.RecycleMenu>> RECYCLE_MENU = MENUS.register(
            "recycle_menu",
            () -> IForgeMenuType.create((windowId, inv, data) -> {
                net.minecraft.core.BlockPos pos = data != null ? data.readBlockPos() : inv.player.blockPosition();
                var be = (org.lupz.doomsdayessentials.block.RecycleBlockEntity) inv.player.level().getBlockEntity(pos);
                return new org.lupz.doomsdayessentials.menu.RecycleMenu(windowId, inv, be);
            }));

    private ProfessionMenuTypes() {}

    public static void register(IEventBus eventBus) {
        MENUS.register(eventBus);
    }
} 