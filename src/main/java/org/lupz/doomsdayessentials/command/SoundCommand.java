package org.lupz.doomsdayessentials.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lupz.doomsdayessentials.EssentialsMod;
import org.lupz.doomsdayessentials.sound.ModSounds;

@Mod.EventBusSubscriber(modid = EssentialsMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class SoundCommand {

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("playsoundessentials")
                .requires(src -> src.hasPermission(2))
                .then(Commands.argument("sound", StringArgumentType.word())
                    .suggests((ctx, builder) -> {
                        builder.suggest("frequencia1");
                        builder.suggest("frequencia2");
                        return builder.buildFuture();
                    })
                    .executes(ctx -> playSound(ctx, null))
                    .then(Commands.argument("player", EntityArgument.player())
                        .executes(ctx -> playSound(ctx, EntityArgument.getPlayer(ctx, "player")))
                    )
                )
        );
    }

    private static int playSound(CommandContext<CommandSourceStack> ctx, ServerPlayer player) throws CommandSyntaxException {
        String soundName = StringArgumentType.getString(ctx, "sound");
        ServerPlayer target = player;
        if (target == null) {
            if (ctx.getSource().getEntity() instanceof ServerPlayer) {
                target = (ServerPlayer) ctx.getSource().getEntity();
            } else {
                ctx.getSource().sendFailure(Component.literal("You must specify a player to play the sound for."));
                return 0;
            }
        }

        final ServerPlayer finalTarget = target;
        switch (soundName) {
            case "frequencia1":
                target.playNotifySound(ModSounds.FREQUENCIA1.get(), SoundSource.MASTER, 1.0f, 1.0f);
                ctx.getSource().sendSuccess(() -> Component.literal("Playing frequencia1 for " + finalTarget.getName().getString()), false);
                break;
            case "frequencia2":
                target.playNotifySound(ModSounds.FREQUENCIA2.get(), SoundSource.MASTER, 1.0f, 1.0f);
                ctx.getSource().sendSuccess(() -> Component.literal("Playing frequencia2 for " + finalTarget.getName().getString()), false);
                break;
            default:
                ctx.getSource().sendFailure(Component.literal("Unknown sound: " + soundName));
                return 0;
        }

        return 1;
    }
} 