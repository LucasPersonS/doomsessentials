package org.lupz.doomsdayessentials.professions.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.lupz.doomsdayessentials.professions.RastreadorProfession;
import org.lupz.doomsdayessentials.professions.capability.TrackerCapabilityProvider;

import java.util.UUID;
import java.util.stream.Collectors;

public class RastreadorCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("rastreador")
            .requires(source -> source.getEntity() instanceof ServerPlayer && RastreadorProfession.isTracker((ServerPlayer) source.getEntity()))
            .then(Commands.literal("whitelist")
                .then(Commands.literal("add")
                    .then(Commands.argument("player", StringArgumentType.word())
                        .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(ctx.getSource().getOnlinePlayerNames(), builder))
                        .executes(RastreadorCommand::addPlayerToWhitelist)))
                .then(Commands.literal("remove")
                    .then(Commands.argument("player", StringArgumentType.word())
                        .suggests((ctx, builder) -> {
                            try {
                                ServerPlayer tracker = ctx.getSource().getPlayerOrException();
                                tracker.getCapability(TrackerCapabilityProvider.TRACKER_CAPABILITY).ifPresent(cap -> {
                                    cap.getWhitelist().forEach(uuid -> {
                                        ServerPlayer whitelistedPlayer = tracker.getServer().getPlayerList().getPlayer(uuid);
                                        if (whitelistedPlayer != null) {
                                            builder.suggest(whitelistedPlayer.getName().getString());
                                        }
                                    });
                                });
                            } catch (CommandSyntaxException e) {
                                // Ignore
                            }
                            return builder.buildFuture();
                        })
                        .executes(RastreadorCommand::removePlayerFromWhitelist)))
                .then(Commands.literal("list")
                    .executes(RastreadorCommand::listWhitelist))
            )
        );
    }

    private static int addPlayerToWhitelist(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer tracker = context.getSource().getPlayerOrException();
        String playerName = StringArgumentType.getString(context, "player");
        ServerPlayer playerToAdd = context.getSource().getServer().getPlayerList().getPlayerByName(playerName);

        if (playerToAdd == null) {
            context.getSource().sendFailure(Component.literal("Player not found: " + playerName));
            return 0;
        }

        tracker.getCapability(TrackerCapabilityProvider.TRACKER_CAPABILITY).ifPresent(cap -> {
            if (cap.isWhitelisted(playerToAdd.getUUID())) {
                context.getSource().sendFailure(Component.literal("Player " + playerName + " is already on your whitelist."));
            } else {
                cap.addToWhitelist(playerToAdd.getUUID());
                context.getSource().sendSuccess(() -> Component.literal("Player " + playerName + " has been added to your whitelist."), true);
            }
        });
        return 1;
    }
    
    private static int removePlayerFromWhitelist(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer tracker = context.getSource().getPlayerOrException();
        String playerName = StringArgumentType.getString(context, "player");
        ServerPlayer playerToRemove = context.getSource().getServer().getPlayerList().getPlayerByName(playerName);

        if (playerToRemove == null) {
            context.getSource().sendFailure(Component.literal("Player not found: " + playerName));
            return 0;
        }

        tracker.getCapability(TrackerCapabilityProvider.TRACKER_CAPABILITY).ifPresent(cap -> {
            if (!cap.isWhitelisted(playerToRemove.getUUID())) {
                context.getSource().sendFailure(Component.literal("Player " + playerName + " is not on your whitelist."));
            } else {
                cap.removeFromWhitelist(playerToRemove.getUUID());
                context.getSource().sendSuccess(() -> Component.literal("Player " + playerName + " has been removed from your whitelist."), true);
            }
        });
        return 1;
    }

    private static int listWhitelist(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer tracker = context.getSource().getPlayerOrException();
        tracker.getCapability(TrackerCapabilityProvider.TRACKER_CAPABILITY).ifPresent(cap -> {
            if (cap.getWhitelist().isEmpty()) {
                context.getSource().sendSuccess(() -> Component.literal("Your whitelist is empty."), false);
                return;
            }

            String list = cap.getWhitelist().stream()
                .map(uuid -> tracker.getServer().getPlayerList().getPlayer(uuid))
                .filter(java.util.Objects::nonNull)
                .map(p -> p.getName().getString())
                .collect(Collectors.joining(", "));

            context.getSource().sendSuccess(() -> Component.literal("Your whitelist: " + list), false);
        });
        return 1;
    }
} 