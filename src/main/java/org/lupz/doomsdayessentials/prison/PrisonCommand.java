package org.lupz.doomsdayessentials.prison;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lupz.doomsdayessentials.EssentialsMod;

@Mod.EventBusSubscriber(modid = EssentialsMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class PrisonCommand {

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent e) {
        register(e.getDispatcher());
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        var root = Commands.literal("prisao").requires(src -> src.hasPermission(0));

        var tempo = Commands.literal("tempo")
            .executes(ctx -> {
                ServerPlayer sp = ctx.getSource().getPlayerOrException();
                int secs = PrisonManager.get().getRemainingTime(sp.getUUID());
                if (secs <= 0) {
                    ctx.getSource().sendSuccess(() -> Component.literal("§aVocê não está preso."), false);
                } else {
                    int m = secs / 60; int s = secs % 60;
                    ctx.getSource().sendSuccess(() -> Component.literal(String.format("§eTempo restante: %02dm%02ds", m, s)), false);
                }
                return 1;
            })
            .then(Commands.argument("alvo", EntityArgument.player())
                .requires(src -> src.hasPermission(2))
                .executes(ctx -> {
                    ServerPlayer tgt = EntityArgument.getPlayer(ctx, "alvo");
                    int secs = PrisonManager.get().getRemainingTime(tgt.getUUID());
                    int m = secs / 60; int s = secs % 60;
                    ctx.getSource().sendSuccess(() -> Component.literal(String.format("§e%1$s: %02dm%02ds", tgt.getName().getString(), m, s)), false);
                    return 1;
                })
            );

        var jail = Commands.literal("jail")
            .requires(src -> src.hasPermission(2))
            .then(Commands.argument("alvo", EntityArgument.player())
                .then(Commands.argument("area", StringArgumentType.word())
                    .then(Commands.argument("duracaoSegundos", IntegerArgumentType.integer(1))
                        .executes(ctx -> {
                            ServerPlayer tgt = EntityArgument.getPlayer(ctx, "alvo");
                            String area = StringArgumentType.getString(ctx, "area");
                            int secs = IntegerArgumentType.getInteger(ctx, "duracaoSegundos");
                            boolean ok = PrisonManager.get().jailPlayer(tgt, area, secs);
                            if (ok) ctx.getSource().sendSuccess(() -> Component.literal("§cJogador preso."), true);
                            else ctx.getSource().sendFailure(Component.literal("Área de prisão inválida."));
                            return ok ? 1 : 0;
                        })
                    )
                )
            );

        var prender = Commands.literal("prender")
            .requires(src -> src.hasPermission(2))
            .then(Commands.argument("alvo", EntityArgument.player())
                .then(Commands.argument("area", StringArgumentType.word())
                    .then(Commands.argument("duracaoSegundos", IntegerArgumentType.integer(1))
                        .executes(ctx -> {
                            ServerPlayer tgt = EntityArgument.getPlayer(ctx, "alvo");
                            String area = StringArgumentType.getString(ctx, "area");
                            int secs = IntegerArgumentType.getInteger(ctx, "duracaoSegundos");
                            boolean ok = PrisonManager.get().jailPlayer(tgt, area, secs);
                            if (ok) ctx.getSource().sendSuccess(() -> Component.literal("§cJogador preso."), true);
                            else ctx.getSource().sendFailure(Component.literal("Área de prisão inválida."));
                            return ok ? 1 : 0;
                        })
                    )
                )
            );

        var liberar = Commands.literal("liberar")
            .requires(src -> src.hasPermission(2))
            .then(Commands.argument("alvo", EntityArgument.player())
                .executes(ctx -> {
                    ServerPlayer tgt = EntityArgument.getPlayer(ctx, "alvo");
                    boolean ok = PrisonManager.get().releasePlayer(tgt);
                    if (ok) ctx.getSource().sendSuccess(() -> Component.literal("§aLiberado."), true);
                    else ctx.getSource().sendFailure(Component.literal("Jogador não está preso."));
                    return ok ? 1 : 0;
                })
            );

        var soltar = Commands.literal("soltar")
            .requires(src -> src.hasPermission(2))
            .then(Commands.argument("alvo", EntityArgument.player())
                .executes(ctx -> {
                    ServerPlayer tgt = EntityArgument.getPlayer(ctx, "alvo");
                    boolean ok = PrisonManager.get().releasePlayer(tgt);
                    if (ok) ctx.getSource().sendSuccess(() -> Component.literal("§aLiberado."), true);
                    else ctx.getSource().sendFailure(Component.literal("Jogador não está preso."));
                    return ok ? 1 : 0;
                })
            );

        dispatcher.register(root.then(tempo).then(jail).then(prender).then(liberar).then(soltar));
    }
}
