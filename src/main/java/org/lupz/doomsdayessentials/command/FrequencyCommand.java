package org.lupz.doomsdayessentials.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lupz.doomsdayessentials.EssentialsMod;
import org.lupz.doomsdayessentials.frequency.capability.FrequencyCapabilityProvider;
import org.lupz.doomsdayessentials.network.PacketHandler;
import org.lupz.doomsdayessentials.network.packet.s2c.SyncFrequencyPacket;

import java.util.List;

@Mod.EventBusSubscriber(modid = EssentialsMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class FrequencyCommand {
    private FrequencyCommand() {}

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("frequency")
            .requires(src -> src.hasPermission(2))
            // Self set: /frequency set <value> [nosound] [noimage]
            .then(Commands.literal("set")
                .then(Commands.argument("value", IntegerArgumentType.integer(0, 100))
                    .executes(ctx -> doSet(ctx.getSource(), ctx.getSource().getPlayer(), IntegerArgumentType.getInteger(ctx, "value"), List.of()))
                    .then(Commands.literal("nosound")
                        .executes(ctx -> doSet(ctx.getSource(), ctx.getSource().getPlayer(), IntegerArgumentType.getInteger(ctx, "value"), List.of("nosound")))
                        .then(Commands.literal("noimage")
                            .executes(ctx -> doSet(ctx.getSource(), ctx.getSource().getPlayer(), IntegerArgumentType.getInteger(ctx, "value"), List.of("nosound","noimage")))
                        )
                    )
                    .then(Commands.literal("noimage")
                        .executes(ctx -> doSet(ctx.getSource(), ctx.getSource().getPlayer(), IntegerArgumentType.getInteger(ctx, "value"), List.of("noimage")))
                        .then(Commands.literal("nosound")
                            .executes(ctx -> doSet(ctx.getSource(), ctx.getSource().getPlayer(), IntegerArgumentType.getInteger(ctx, "value"), List.of("noimage","nosound")))
                        )
                    )
                )
                // Targeted set: /frequency set <player> <value> [nosound] [noimage]
                .then(Commands.argument("player", EntityArgument.player())
                    .then(Commands.argument("value", IntegerArgumentType.integer(0, 100))
                        .executes(ctx -> doSet(ctx.getSource(), EntityArgument.getPlayer(ctx, "player"), IntegerArgumentType.getInteger(ctx, "value"), List.of()))
                        .then(Commands.literal("nosound")
                            .executes(ctx -> doSet(ctx.getSource(), EntityArgument.getPlayer(ctx, "player"), IntegerArgumentType.getInteger(ctx, "value"), List.of("nosound")))
                            .then(Commands.literal("noimage")
                                .executes(ctx -> doSet(ctx.getSource(), EntityArgument.getPlayer(ctx, "player"), IntegerArgumentType.getInteger(ctx, "value"), List.of("nosound","noimage")))
                            )
                        )
                        .then(Commands.literal("noimage")
                            .executes(ctx -> doSet(ctx.getSource(), EntityArgument.getPlayer(ctx, "player"), IntegerArgumentType.getInteger(ctx, "value"), List.of("noimage")))
                            .then(Commands.literal("nosound")
                                .executes(ctx -> doSet(ctx.getSource(), EntityArgument.getPlayer(ctx, "player"), IntegerArgumentType.getInteger(ctx, "value"), List.of("noimage","nosound")))
                            )
                        )
                    )
                )
            )
            // Self get and targeted get remain
            .then(Commands.literal("get")
                .executes(ctx -> doGet(ctx.getSource(), ctx.getSource().getPlayer()))
                .then(Commands.argument("player", EntityArgument.player())
                    .executes(ctx -> doGet(ctx.getSource(), EntityArgument.getPlayer(ctx, "player")))
                )
            )
        );
    }

    private static int doSet(CommandSourceStack src, ServerPlayer target, int value, List<String> flags) {
        if (target == null) return 0;
        boolean nosound = flags.contains("nosound");
        boolean noimage = flags.contains("noimage");
        target.getCapability(FrequencyCapabilityProvider.FREQUENCY_CAPABILITY).ifPresent(cap -> {
            cap.setLevel(value);
            cap.setLastServerUpdateMs(System.currentTimeMillis());
            cap.setSoundsEnabled(!nosound);
            cap.setImagesEnabled(!noimage);
            // Sync level to client; flags are client-read via capability, no packet needed here
            PacketHandler.CHANNEL.send(net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> target), new SyncFrequencyPacket(value, !nosound, !noimage));
        });
        src.sendSuccess(() -> net.minecraft.network.chat.Component.literal("Frequência de " + target.getName().getString() + " definida para " + value + "%" + (nosound?" (sem som)":"") + (noimage?" (sem imagens)":"")), true);
        return 1;
    }

    private static int doGet(CommandSourceStack src, ServerPlayer target) {
        if (target == null) return 0;
        target.getCapability(FrequencyCapabilityProvider.FREQUENCY_CAPABILITY).ifPresent(cap -> {
            src.sendSuccess(() -> net.minecraft.network.chat.Component.literal("Frequência de " + target.getName().getString() + ": " + cap.getLevel() + "%" + (cap.isSoundsEnabled()?"":" (sem som)") + (cap.isImagesEnabled()?"":" (sem imagens)")), false);
        });
        return 1;
    }
} 