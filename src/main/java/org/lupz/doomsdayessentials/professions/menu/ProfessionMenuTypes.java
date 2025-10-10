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

    public static final RegistryObject<MenuType<org.lupz.doomsdayessentials.professions.menu.ShopMenu>> SHOP_MENU = MENUS.register(
            "shop_menu",
            () -> IForgeMenuType.create((windowId, inv, data) -> new org.lupz.doomsdayessentials.professions.menu.ShopMenu(windowId, inv)));

    public static final RegistryObject<MenuType<EngineerCraftMenu>> ENGINEER_CRAFT = MENUS.register(
            "engineer_craft_menu",
            () -> IForgeMenuType.create((windowId, inv, data) -> new EngineerCraftMenu(windowId, inv)));

    public static final RegistryObject<MenuType<org.lupz.doomsdayessentials.professions.menu.MedicRewardMenu>> MEDIC_REWARD_MENU = MENUS.register(
            "medic_reward_menu",
            () -> IForgeMenuType.create((windowId, inv, data) -> new org.lupz.doomsdayessentials.professions.menu.MedicRewardMenu(windowId, inv)));

    public static final RegistryObject<MenuType<BountyBoardMenu>> BOUNTY_BOARD_MENU = MENUS.register(
            "bounty_board_menu",
            () -> IForgeMenuType.create((windowId, inv, data) -> new BountyBoardMenu(windowId, inv)));

    public static final RegistryObject<MenuType<org.lupz.doomsdayessentials.territory.menu.TerritoryRewardMenu>> TERRITORY_REWARD_MENU = MENUS.register(
            "territory_reward_menu",
            () -> IForgeMenuType.create((windowId, inv, data) -> new org.lupz.doomsdayessentials.territory.menu.TerritoryRewardMenu(windowId, inv)));

    public static final RegistryObject<MenuType<org.lupz.doomsdayessentials.territory.menu.GeneratorInfoMenu>> GENERATOR_INFO_MENU = MENUS.register(
            "generator_info_menu",
            () -> net.minecraftforge.common.extensions.IForgeMenuType.create((windowId, inv, data) -> {
                String area = data != null ? data.readUtf() : "";
                return new org.lupz.doomsdayessentials.territory.menu.GeneratorInfoMenu(windowId, inv, area);
            }));

    public static final RegistryObject<MenuType<org.lupz.doomsdayessentials.menu.RecycleMenu>> RECYCLE_MENU = MENUS.register(
            "recycle_menu",
            () -> IForgeMenuType.create((windowId, inv, data) -> {
                net.minecraft.core.BlockPos pos = data != null ? data.readBlockPos() : inv.player.blockPosition();
                var be = (org.lupz.doomsdayessentials.block.RecycleBlockEntity) inv.player.level().getBlockEntity(pos);
                return new org.lupz.doomsdayessentials.menu.RecycleMenu(windowId, inv, be);
            }));

    public static final RegistryObject<MenuType<org.lupz.doomsdayessentials.guild.menu.GuildMainMenu>> GUILD_MAIN_MENU = MENUS.register(
            "guild_main_menu",
            () -> IForgeMenuType.create((windowId, inv, data) -> new org.lupz.doomsdayessentials.guild.menu.GuildMainMenu(windowId, inv)));

    public static final RegistryObject<MenuType<org.lupz.doomsdayessentials.guild.menu.GuildStorageMenu>> GUILD_STORAGE_MENU = MENUS.register(
            "guild_storage_menu",
            () -> IForgeMenuType.create((windowId, inv, data) -> {
                int page = data != null ? data.readVarInt() : 0;
                return new org.lupz.doomsdayessentials.guild.menu.GuildStorageMenu(windowId, inv, page);
            }));

    public static final RegistryObject<MenuType<org.lupz.doomsdayessentials.guild.menu.GuildMembersMenu>> GUILD_MEMBERS_MENU = MENUS.register(
            "guild_members_menu",
            () -> IForgeMenuType.create((windowId, inv, data) -> new org.lupz.doomsdayessentials.guild.menu.GuildMembersMenu(windowId, inv)));

    public static final RegistryObject<MenuType<org.lupz.doomsdayessentials.guild.menu.GuildMemberActionsMenu>> GUILD_MEMBER_ACTIONS_MENU = MENUS.register(
            "guild_member_actions_menu",
            () -> IForgeMenuType.create((windowId, inv, data) -> {
                java.util.UUID target = data != null && data.readableBytes() >= 16 ? data.readUUID() : java.util.UUID.randomUUID();
                return new org.lupz.doomsdayessentials.guild.menu.GuildMemberActionsMenu(windowId, inv, target);
            }));

    public static final RegistryObject<MenuType<org.lupz.doomsdayessentials.guild.menu.GuildAlliancesMenu>> GUILD_ALLIANCES_MENU = MENUS.register(
            "guild_alliances_menu",
            () -> IForgeMenuType.create((windowId, inv, data) -> new org.lupz.doomsdayessentials.guild.menu.GuildAlliancesMenu(windowId, inv)));

    public static final RegistryObject<MenuType<org.lupz.doomsdayessentials.guild.menu.GuildStorageLogMenu>> GUILD_STORAGE_LOG_MENU = MENUS.register(
            "guild_storage_log_menu",
            () -> IForgeMenuType.create((windowId, inv, data) -> {
                String g = data != null ? data.readUtf() : "";
                return new org.lupz.doomsdayessentials.guild.menu.GuildStorageLogMenu(windowId, inv, g);
            }));

    public static final RegistryObject<MenuType<org.lupz.doomsdayessentials.guild.menu.GuildUpgradeMenu>> GUILD_UPGRADE_MENU = MENUS.register(
            "guild_upgrade_menu",
            () -> IForgeMenuType.create((windowId, inv, data) -> new org.lupz.doomsdayessentials.guild.menu.GuildUpgradeMenu(windowId, inv)));

    public static final RegistryObject<MenuType<org.lupz.doomsdayessentials.guild.menu.GuildResourceDepositMenu>> GUILD_RESOURCE_DEPOSIT_MENU = MENUS.register(
            "guild_resource_deposit_menu",
            () -> IForgeMenuType.create((windowId, inv, data) -> new org.lupz.doomsdayessentials.guild.menu.GuildResourceDepositMenu(windowId, inv)));

    private ProfessionMenuTypes() {}

    public static void register(IEventBus eventBus) {
        MENUS.register(eventBus);
    }
} 