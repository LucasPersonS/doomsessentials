package org.lupz.doomsdayessentials.territory.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lupz.doomsdayessentials.EssentialsMod;

@Mod.EventBusSubscriber(modid = EssentialsMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class EventCommand {

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent e) {
        register(e.getDispatcher());
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        // Root command /event (admin only)
        var eventRoot = Commands.literal("event")
                .requires(src -> src.hasPermission(2))
                .then(Commands.literal("start")
                        .then(Commands.argument("area", StringArgumentType.word()).suggests(EventCommands::suggestAreaNames)
                                .then(Commands.argument("durationMinutes", IntegerArgumentType.integer(1))
                                        .then(Commands.argument("minPlayers", IntegerArgumentType.integer(1))
                                                .executes(EventCommands::start)))))
                .then(Commands.literal("status")
                        .then(Commands.argument("area", StringArgumentType.word()).suggests(EventCommands::suggestAreaNames)
                                .executes(EventCommands::status))
                        .executes(EventCommands::status))
                .then(Commands.literal("players")
                        .then(Commands.argument("area", StringArgumentType.word()).suggests(EventCommands::suggestAreaNames)
                                .then(Commands.argument("minPlayers", IntegerArgumentType.integer(1))
                                        .executes(EventCommands::setPlayers))))
                .then(Commands.literal("stop")
                        .then(Commands.argument("area", StringArgumentType.word()).suggests(EventCommands::suggestAreaNames)
                                .executes(EventCommands::stop))
                        .executes(EventCommands::stopAll));

        dispatcher.register(eventRoot);
    }
}
