package org.lupz.doomsdayessentials;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.logging.LogUtils;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.commands.Commands;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.lupz.doomsdayessentials.block.ModBlocks;
import org.lupz.doomsdayessentials.client.ClientCombatRenderHandler;
import org.lupz.doomsdayessentials.combat.CombatManager;
import org.lupz.doomsdayessentials.combat.command.AreaCommand;
import org.lupz.doomsdayessentials.combat.command.CombatCommand;
import org.lupz.doomsdayessentials.command.DoomsHelpCommand;
import org.lupz.doomsdayessentials.command.SoundCommand;
import org.lupz.doomsdayessentials.config.EssentialsConfig;
import org.lupz.doomsdayessentials.effect.ModEffects;
import org.lupz.doomsdayessentials.item.ModItems;
import org.lupz.doomsdayessentials.injury.InjuryCommands;
import org.lupz.doomsdayessentials.injury.InjuryItems;
import org.lupz.doomsdayessentials.injury.network.InjuryNetwork;
import org.lupz.doomsdayessentials.network.PacketHandler;
import org.lupz.doomsdayessentials.professions.items.ProfessionItems;
import org.lupz.doomsdayessentials.professions.menu.ProfessionMenuTypes;
import org.lupz.doomsdayessentials.professions.menu.ProfissoesScreen;
import org.lupz.doomsdayessentials.professions.menu.EngineerCraftScreen;
import org.lupz.doomsdayessentials.professions.network.EngineerNetwork;
import org.lupz.doomsdayessentials.professions.shop.EngineerConfig;
import org.lupz.doomsdayessentials.professions.shop.EngineerShopUtil;
import org.lupz.doomsdayessentials.recycler.RecycleRecipeManager;
import org.lupz.doomsdayessentials.sound.ModSounds;
import org.lupz.doomsdayessentials.guild.GuildConfig;
import org.lupz.doomsdayessentials.guild.command.OrganizacaoCommand;
import org.lupz.doomsdayessentials.item.ModCreativeTab;
import org.slf4j.Logger;
import org.lupz.doomsdayessentials.client.renderer.RecycleBlockRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import org.lupz.doomsdayessentials.client.model.RecycleModel2;
import org.lupz.doomsdayessentials.entity.ModEntities;

@Mod(EssentialsMod.MOD_ID)
public class EssentialsMod {
    public static final String MOD_ID = "doomsdayessentials";
    public static final Logger LOGGER = LogUtils.getLogger();

    public EssentialsMod(FMLJavaModLoadingContext context) {
        IEventBus modEventBus = context.getModEventBus();

        ModItems.register(modEventBus);
        InjuryItems.register(modEventBus);
        ProfessionItems.register(modEventBus);
        ModBlocks.register(modEventBus);
        ModSounds.register(modEventBus);
                 ModEntities.register(modEventBus);
         org.lupz.doomsdayessentials.event.eclipse.market.MarketEntities.register(modEventBus);
         org.lupz.doomsdayessentials.event.eclipse.market.MarketBlocks.register(modEventBus);
         ModEffects.register(modEventBus);
         ProfessionMenuTypes.register(modEventBus);
         org.lupz.doomsdayessentials.event.eclipse.market.NightMarketMenus.register(modEventBus);
         ModCreativeTab.register(modEventBus);

        modEventBus.addListener(this::commonSetup);

        // Register Configs
        context.registerConfig(ModConfig.Type.COMMON, EssentialsConfig.SPEC, MOD_ID + "-essentials.toml");
        // Guild / Organizacao config
        context.registerConfig(ModConfig.Type.COMMON, GuildConfig.SPEC, MOD_ID + "-guilds.toml");
        // Engineer config
        context.registerConfig(ModConfig.Type.COMMON, EngineerConfig.SPEC, MOD_ID + "-engineer.toml");

        // Load engineer recipes
        EngineerShopUtil.loadConfig();

        // Register ourselves for server and other game events we are interested in
        MinecraftForge.EVENT_BUS.register(this);
        MinecraftForge.EVENT_BUS.addListener(this::registerCommands);

        // Force class loading to ensure EventBusSubscriber is registered
        try {
            Class.forName("org.lupz.doomsdayessentials.guild.command.OrganizacaoCommand");
            Class.forName("org.lupz.doomsdayessentials.combat.command.AreaCommand");
            Class.forName("org.lupz.doomsdayessentials.combat.command.CombatCommand");
            Class.forName("org.lupz.doomsdayessentials.command.SoundCommand");
            Class.forName("org.lupz.doomsdayessentials.command.DoomsHelpCommand");
            Class.forName("org.lupz.doomsdayessentials.injury.InjuryCommands");
            Class.forName("org.lupz.doomsdayessentials.professions.commands.ProfessionCommandsRegister");
            Class.forName("org.lupz.doomsdayessentials.command.RecyclerCommand");
            Class.forName("org.lupz.doomsdayessentials.territory.command.TerritoryCommand");
            Class.forName("org.lupz.doomsdayessentials.territory.TerritoryAccessEvents");
            Class.forName("org.lupz.doomsdayessentials.command.DItemCommand"); // ensure registration
            Class.forName("org.lupz.doomsdayessentials.command.EventCommand"); // ensure registration of /dooms event
            // Eclipse infection command removed; replaced by Frequency system
            Class.forName("org.lupz.doomsdayessentials.command.EclipseScoreCommand");
            Class.forName("org.lupz.doomsdayessentials.command.NightMarketCommand");
        } catch (ClassNotFoundException e) {
            LOGGER.error("Failed to load command class", e);
        }
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            CombatManager.get();
            org.lupz.doomsdayessentials.territory.TerritoryEventManager.get();
            org.lupz.doomsdayessentials.territory.ResourceGeneratorManager.get();
            PacketHandler.register();
            InjuryNetwork.register();
            EngineerNetwork.register();
            org.lupz.doomsdayessentials.professions.ProfissaoManager.loadProfessions();
            RecycleRecipeManager.loadRecipes();
            org.lupz.doomsdayessentials.item.BlockedItemManager.get();
            org.lupz.doomsdayessentials.utils.SkyColorManager.get();
        });
    }

    public void registerCommands(RegisterCommandsEvent event) {
        // Removed old CustomParticleManager testparticle command.
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
    }

    @Mod.EventBusSubscriber(modid = MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            // CustomParticleManager removed – no particle preloading.
            event.enqueueWork(() -> {
                ClientCombatRenderHandler.init();
                MenuScreens.register(ProfessionMenuTypes.PROFISSOES_MENU.get(), ProfissoesScreen::new);
                MenuScreens.register(ProfessionMenuTypes.SHOP_MENU.get(), org.lupz.doomsdayessentials.professions.menu.ShopScreen::new);
                MenuScreens.register(ProfessionMenuTypes.ENGINEER_CRAFT.get(), org.lupz.doomsdayessentials.professions.menu.EngineerCraftScreen::new);
                MenuScreens.register(ProfessionMenuTypes.MEDIC_REWARD_MENU.get(), org.lupz.doomsdayessentials.professions.menu.MedicRewardScreen::new);
                MenuScreens.register(ProfessionMenuTypes.TERRITORY_REWARD_MENU.get(), org.lupz.doomsdayessentials.territory.menu.TerritoryRewardScreen::new);
                MenuScreens.register(ProfessionMenuTypes.BOUNTY_BOARD_MENU.get(), org.lupz.doomsdayessentials.professions.menu.BountyBoardScreen::new);
                                 MenuScreens.register(ProfessionMenuTypes.RECYCLE_MENU.get(), org.lupz.doomsdayessentials.menu.RecycleScreen::new);
                 org.lupz.doomsdayessentials.event.eclipse.market.NightMarketMenus.clientSetup(event);
 
                                  // Register Recycle block renderer
                 BlockEntityRenderers.register(ModBlocks.RECYCLE_BLOCK_ENTITY.get(), RecycleBlockRenderer::new);
                 // Night Market block renderer (GeoLib)
                 net.minecraft.client.renderer.blockentity.BlockEntityRenderers.register(
                     org.lupz.doomsdayessentials.event.eclipse.market.MarketBlocks.NIGHT_MARKET_BLOCK_ENTITY.get(),
                     ctx -> new org.lupz.doomsdayessentials.client.renderer.NightMarketBlockRenderer()
                 );

                // Faceless renderer
                net.minecraft.client.renderer.entity.EntityRenderers.register(org.lupz.doomsdayessentials.entity.ModEntities.FACELESS.get(), org.lupz.doomsdayessentials.client.renderer.FacelessRenderer::new);
                // Sentry renderer
                net.minecraft.client.renderer.entity.EntityRenderers.register(org.lupz.doomsdayessentials.entity.ModEntities.SENTRY.get(), org.lupz.doomsdayessentials.client.renderer.SentryRenderer::new);
                // Night Market renderer
                net.minecraft.client.renderer.entity.EntityRenderers.register(org.lupz.doomsdayessentials.event.eclipse.market.MarketEntities.NIGHT_MARKET.get(), org.lupz.doomsdayessentials.client.renderer.NightMarketRenderer::new);

                // Register GeckoLib crown armor renderer
                // Crown armor renderer is now supplied via initializeClient in the item class.
            });
        }

        @SubscribeEvent
        public static void registerLayerDefinitions(net.minecraftforge.client.event.EntityRenderersEvent.RegisterLayerDefinitions event) {
            event.registerLayerDefinition(org.lupz.doomsdayessentials.client.model.CrownModel.LAYER_LOCATION,
                    org.lupz.doomsdayessentials.client.model.CrownModel::createBodyLayer);
            event.registerLayerDefinition(RecycleModel2.LAYER_LOCATION, RecycleModel2::createBodyLayer);
        }
    }
} 