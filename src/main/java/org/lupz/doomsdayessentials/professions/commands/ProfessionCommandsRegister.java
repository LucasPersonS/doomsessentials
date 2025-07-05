package org.lupz.doomsdayessentials.professions.commands;

import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lupz.doomsdayessentials.EssentialsMod;
import org.lupz.doomsdayessentials.professions.commands.EngenheiroCommand;

/**
 * Ensures that all profession-related commands are registered during the standard
 * {@link net.minecraftforge.event.RegisterCommandsEvent} lifecycle stage.  We also
 * register them from {@code EssentialsMod.onServerStarting}, but some environments
 * (e.g. datapack reloads) only fire the Forge command-registration event.  By
 * subscribing here we guarantee the commands are available at all times.
 */
@Mod.EventBusSubscriber(modid = EssentialsMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ProfessionCommandsRegister {

    private ProfessionCommandsRegister() {}

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        var dispatcher = event.getDispatcher();
        ProfissoesCommand.register(dispatcher);
        MedicoCommand.register(dispatcher);
        RastreadorCommand.register(dispatcher);
        EngenheiroCommand.register(dispatcher);
    }
} 