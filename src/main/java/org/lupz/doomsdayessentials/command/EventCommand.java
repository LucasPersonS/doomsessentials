package org.lupz.doomsdayessentials.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lupz.doomsdayessentials.EssentialsMod;
import org.lupz.doomsdayessentials.event.eclipse.EclipseEventManager;

@Mod.EventBusSubscriber(modid = EssentialsMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class EventCommand {

    private EventCommand() {}

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event){
        register(event.getDispatcher());
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher){
        dispatcher.register(
            Commands.literal("dooms").requires(src -> src.hasPermission(2))
                .then(Commands.literal("event")
                    .then(Commands.literal("eclipse")
                        .then(Commands.literal("iniciar")
                            .executes(ctx -> startEclipse(ctx.getSource(), 0.0f, 6.0f, 0.65f))
                            .then(Commands.argument("near", FloatArgumentType.floatArg(0.0f))
                                .then(Commands.argument("far", FloatArgumentType.floatArg(0.1f))
                                    .then(Commands.argument("overlayAlpha", FloatArgumentType.floatArg(0.0f,1.0f))
                                        .executes(ctx -> startEclipse(
                                            ctx.getSource(),
                                            FloatArgumentType.getFloat(ctx, "near"),
                                            FloatArgumentType.getFloat(ctx, "far"),
                                            FloatArgumentType.getFloat(ctx, "overlayAlpha")
                                        ))
                                    )
                                )
                            )
                        )
                        .then(Commands.literal("parar").executes(ctx -> stopEclipse(ctx.getSource())))
                    )
                )
        );
    }

    private static int startEclipse(CommandSourceStack src, float near, float far, float overlay){
        ServerLevel level = src.getLevel();
        EclipseEventManager.get().setFog(near, far, overlay, level);
        EclipseEventManager.get().start(level);
        src.sendSuccess(() -> Component.literal("Eclipse iniciado (near="+near+", far="+far+", overlay="+overlay+")"), true);
        return 1;
    }

    private static int stopEclipse(CommandSourceStack src){
        ServerLevel level = src.getLevel();
        EclipseEventManager.get().stop(level);
        src.sendSuccess(() -> Component.literal("Eclipse parado."), true);
        return 1;
    }
} 