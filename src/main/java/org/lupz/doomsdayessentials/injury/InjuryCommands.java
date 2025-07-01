package org.lupz.doomsdayessentials.injury;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.lupz.doomsdayessentials.config.EssentialsConfig;
import org.lupz.doomsdayessentials.injury.capability.InjuryCapability;
import org.lupz.doomsdayessentials.injury.network.InjuryNetwork;
import org.lupz.doomsdayessentials.injury.network.UpdateInjuryLevelPacket;

public class InjuryCommands {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            (LiteralArgumentBuilder<CommandSourceStack>) Commands.literal("injury")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("set")
                    .then(Commands.argument("level", IntegerArgumentType.integer(0))
                        .executes(ctx -> executeSetInjury(ctx, ctx.getSource().getPlayerOrException()))
                        .then(Commands.argument("player", EntityArgument.player())
                            .executes(ctx -> executeSetInjury(ctx, EntityArgument.getPlayer(ctx, "player")))
                        )
                    )
                )
                .then(Commands.literal("heal")
                    .executes(ctx -> executeHealInjury(ctx, ctx.getSource().getPlayerOrException()))
                    .then(Commands.argument("player", EntityArgument.player())
                        .executes(ctx -> executeHealInjury(ctx, EntityArgument.getPlayer(ctx, "player")))
                    )
                )
                .then(Commands.literal("info")
                    .executes(ctx -> executeInjuryInfo(ctx, ctx.getSource().getPlayerOrException()))
                    .then(Commands.argument("player", EntityArgument.player())
                        .executes(ctx -> executeInjuryInfo(ctx, EntityArgument.getPlayer(ctx, "player")))
                    )
                )
        );
    }

    private static int executeSetInjury(CommandContext<CommandSourceStack> context, ServerPlayer player) {
        int level = IntegerArgumentType.getInteger(context, "level");
        int maxLevel = EssentialsConfig.MAX_INJURY_LEVEL.get();
        if (level > maxLevel) {
            context.getSource().sendFailure(Component.translatable("command.injury.set.failure.too_high", maxLevel));
            return 0;
        }

        InjuryHelper.getCapability(player).ifPresent(cap -> {
            cap.setInjuryLevel(level);
            InjuryNetwork.sendToPlayer(new UpdateInjuryLevelPacket(level), player);
            context.getSource().sendSuccess(() -> Component.translatable("command.injury.set.success", player.getDisplayName(), level), true);
            if (player != context.getSource().getEntity()) {
                player.sendSystemMessage(Component.translatable("command.injury.set.notification", level));
            }
        });
        return 1;
    }

    private static int executeHealInjury(CommandContext<CommandSourceStack> context, ServerPlayer player) {
        InjuryHelper.getCapability(player).ifPresent(cap -> {
            int oldLevel = cap.getInjuryLevel();
            cap.setInjuryLevel(0);
            InjuryNetwork.sendToPlayer(new UpdateInjuryLevelPacket(0), player);
            context.getSource().sendSuccess(() -> Component.translatable("command.injury.heal.success", player.getDisplayName(), oldLevel), true);
            if (player != context.getSource().getEntity()) {
                player.sendSystemMessage(Component.translatable("command.injury.heal.notification"));
            }
        });
        return 1;
    }

    private static int executeInjuryInfo(CommandContext<CommandSourceStack> context, ServerPlayer player) {
        InjuryHelper.getCapability(player).ifPresent(cap -> {
            int level = cap.getInjuryLevel();
            int healCooldown = cap.getHealCooldown();
            context.getSource().sendSuccess(() -> Component.translatable("command.injury.info.success", player.getDisplayName(), level, healCooldown, healCooldown / 20), false);
        });
        return 1;
    }
} 