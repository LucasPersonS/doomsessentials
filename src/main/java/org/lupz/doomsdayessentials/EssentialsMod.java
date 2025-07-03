package org.lupz.doomsdayessentials;

import com.mojang.logging.LogUtils;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.core.registries.Registries;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
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
import org.lupz.doomsdayessentials.sound.ModSounds;
import org.slf4j.Logger;

@Mod(EssentialsMod.MOD_ID)
public class EssentialsMod {
    public static final String MOD_ID = "doomsdayessentials";
    public static final Logger LOGGER = LogUtils.getLogger();

    public EssentialsMod() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        ModItems.register(modEventBus);
        InjuryItems.register(modEventBus);
        ProfessionItems.register(modEventBus);
        ModBlocks.register(modEventBus);
        ModSounds.register(modEventBus);
        ModEffects.register(modEventBus);
        ProfessionMenuTypes.register(modEventBus);

        modEventBus.addListener(this::commonSetup);

        // Register Configs
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, EssentialsConfig.SPEC, MOD_ID + "-essentials.toml");

        // Register ourselves for server and other game events we are interested in
        MinecraftForge.EVENT_BUS.register(this);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            CombatManager.get();
            PacketHandler.register();
            InjuryNetwork.register();
            org.lupz.doomsdayessentials.professions.ProfissaoManager.loadProfessions();
        });
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        AreaCommand.register(event.getServer().getCommands().getDispatcher());
        CombatCommand.register(event.getServer().getCommands().getDispatcher());
        SoundCommand.register(event.getServer().getCommands().getDispatcher());
        DoomsHelpCommand.register(event.getServer().getCommands().getDispatcher());
        InjuryCommands.register(event.getServer().getCommands().getDispatcher());

        org.lupz.doomsdayessentials.professions.commands.ProfissoesCommand.register(event.getServer().getCommands().getDispatcher());
        org.lupz.doomsdayessentials.professions.commands.MedicoCommand.register(event.getServer().getCommands().getDispatcher());
        org.lupz.doomsdayessentials.professions.commands.RastreadorCommand.register(event.getServer().getCommands().getDispatcher());
    }

    @Mod.EventBusSubscriber(modid = MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            event.enqueueWork(() -> {
                ClientCombatRenderHandler.init();
                MenuScreens.register(ProfessionMenuTypes.PROFISSOES_MENU.get(), ProfissoesScreen::new);
                MenuScreens.register(ProfessionMenuTypes.SHOP_MENU.get(), org.lupz.doomsdayessentials.professions.menu.ShopScreen::new);
            });
        }
    }
} 