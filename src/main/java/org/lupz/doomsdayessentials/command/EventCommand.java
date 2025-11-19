package org.lupz.doomsdayessentials.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lupz.doomsdayessentials.EssentialsMod;
import org.lupz.doomsdayessentials.event.eclipse.EclipseEventManager;
import org.lupz.doomsdayessentials.kofh.KofhConfig;
import org.lupz.doomsdayessentials.kofh.KofhEventManager;

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
                    .then(Commands.literal("kofh")
                        .then(Commands.literal("iniciar")
                            .then(Commands.argument("area", StringArgumentType.string())
                                .executes(ctx -> startKofh(ctx.getSource(), StringArgumentType.getString(ctx, "area"), KofhConfig.TIMER_DURATION_SECONDS.get()))
                                .then(Commands.argument("duracaoSegundos", IntegerArgumentType.integer(30, 36000))
                                    .executes(ctx -> startKofh(
                                        ctx.getSource(),
                                        StringArgumentType.getString(ctx, "area"),
                                        IntegerArgumentType.getInteger(ctx, "duracaoSegundos")
                                    ))
                                )
                            )
                        )
                        .then(Commands.literal("parar")
                            .executes(ctx -> stopKofh(ctx.getSource()))
                        )
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

    private static int startKofh(CommandSourceStack src, String areaName, int durationSeconds) {
        var server = src.getServer();
        boolean ok = KofhEventManager.get().start(server, areaName, durationSeconds);
        if (ok) {
            src.sendSuccess(() -> Component.literal("KOFH iniciado em '" + areaName + "' por " + durationSeconds + "s."), true);
            return 1;
        } else {
            src.sendFailure(Component.literal("Falha ao iniciar KOFH: area desconhecida '" + areaName + "'."));
            return 0;
        }
    }

    private static int stopKofh(CommandSourceStack src) {
        var server = src.getServer();
        KofhEventManager.get().stop(server);
        src.sendSuccess(() -> Component.literal("KOFH parado."), true);
        return 1;
    }
}
