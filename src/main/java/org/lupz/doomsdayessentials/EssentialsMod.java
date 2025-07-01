package org.lupz.doomsdayessentials;

import com.mojang.logging.LogUtils;
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
import org.lupz.doomsdayessentials.client.ClientCombatRenderHandler;
import org.lupz.doomsdayessentials.combat.CombatManager;
import org.lupz.doomsdayessentials.combat.command.AreaCommand;
import org.lupz.doomsdayessentials.combat.command.CombatCommand;
import org.lupz.doomsdayessentials.command.SoundCommand;
import org.lupz.doomsdayessentials.config.EssentialsConfig;
import org.lupz.doomsdayessentials.injury.InjuryCommands;
import org.lupz.doomsdayessentials.injury.InjuryEvents;
import org.lupz.doomsdayessentials.injury.InjuryItems;
import org.lupz.doomsdayessentials.injury.network.InjuryNetwork;
import org.lupz.doomsdayessentials.network.PacketHandler;
import org.lupz.doomsdayessentials.sound.ModSounds;
import org.lupz.doomsdayessentials.command.DoomsHelpCommand;
import org.slf4j.Logger;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.renderer.item.CompassItemPropertyFunction;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.item.CompassItem;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;

@Mod(EssentialsMod.MOD_ID)
public class EssentialsMod {
    public static final String MOD_ID = "doomsdayessentials";
    public static final Logger LOGGER = LogUtils.getLogger();

    public EssentialsMod() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        // Register the commonSetup method for modloading
        modEventBus.addListener(this::commonSetup);

        // Register sound events
        ModSounds.init();
        InjuryItems.register(modEventBus);
        org.lupz.doomsdayessentials.professions.items.ProfessionItems.register(modEventBus);

        // Register ourselves for server and other game events we are interested in
        MinecraftForge.EVENT_BUS.register(this);
        MinecraftForge.EVENT_BUS.register(new InjuryEvents());
        
        // Register profession related content
        org.lupz.doomsdayessentials.professions.menu.ProfessionMenuTypes.register(modEventBus);
        
        // Register config
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, EssentialsConfig.SPEC);
        // Register profession specific config
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, org.lupz.doomsdayessentials.config.ProfessionConfig.SPEC, MOD_ID + "-profession.toml");
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            CombatManager.get(); // Initialize
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

        // profession commands
        org.lupz.doomsdayessentials.professions.commands.ProfissoesCommand.register(event.getServer().getCommands().getDispatcher());
        org.lupz.doomsdayessentials.professions.commands.MedicoCommand.register(event.getServer().getCommands().getDispatcher());
    }

    @Mod.EventBusSubscriber(modid = MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            event.enqueueWork(ClientCombatRenderHandler::init);
            event.enqueueWork(() -> net.minecraft.client.gui.screens.MenuScreens.register(
                    org.lupz.doomsdayessentials.professions.menu.ProfessionMenuTypes.PROFISSOES_MENU.get(),
                    org.lupz.doomsdayessentials.professions.menu.ProfissoesScreen::new));
            // Register compass angle property for the Tracking Compass (same logic as vanilla compass)
            event.enqueueWork(() -> ItemProperties.register(
                    org.lupz.doomsdayessentials.professions.items.ProfessionItems.TRACKING_COMPASS.get(),
                    new ResourceLocation("angle"),
                    new CompassItemPropertyFunction((ClientLevel level, ItemStack stack, Entity entity) -> {
                        return CompassItem.isLodestoneCompass(stack) ? CompassItem.getLodestonePosition(stack.getOrCreateTag()) : CompassItem.getSpawnPosition(level);
                    })));
        }
    }
} 