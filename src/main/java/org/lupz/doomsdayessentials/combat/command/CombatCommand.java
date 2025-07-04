package org.lupz.doomsdayessentials.combat.command;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lupz.doomsdayessentials.combat.CombatManager;
import net.minecraft.ChatFormatting;

@Mod.EventBusSubscriber(modid = org.lupz.doomsdayessentials.EssentialsMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class CombatCommand {

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent e) {
        register(e.getDispatcher());
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("combat")
                .requires(src -> src.hasPermission(2))
                .then(Commands.literal("clear")
                        .then(Commands.argument("targets", EntityArgument.entities())
                                .executes(ctx -> {
                                    var targets = EntityArgument.getEntities(ctx, "targets");
                                    targets.forEach(ent -> CombatManager.get().clearCombat(ent.getUUID()));
                                    ctx.getSource().sendSuccess(() -> Component.literal("Cleared combat for ")
                                            .withStyle(ChatFormatting.GREEN)
                                            .append(Component.literal(String.valueOf(targets.size())).withStyle(ChatFormatting.WHITE))
                                            .append(Component.literal(" entities").withStyle(ChatFormatting.GREEN)), true);
                                    return targets.size();
                                })))
                .then(Commands.literal("tag")
                        .then(Commands.argument("targets", EntityArgument.players())
                                .executes(ctx -> {
                                    var players = EntityArgument.getPlayers(ctx, "targets");
                                    players.forEach(p -> CombatManager.get().tagPlayer(p));
                                    ctx.getSource().sendSuccess(() -> Component.literal("Tagged ")
                                            .withStyle(ChatFormatting.GREEN)
                                            .append(Component.literal(String.valueOf(players.size())).withStyle(ChatFormatting.WHITE))
                                            .append(Component.literal(" players").withStyle(ChatFormatting.GREEN)), true);
                                    return players.size();
                                })))
                .then(Commands.literal("activate")
                        .executes(ctx -> {
                            // If the command source is NOT a player (e.g., server console or command block), apply to all players
                            if (ctx.getSource().getEntity() == null) {
                                var server = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();
                                if (server == null) {
                                    return 0;
                                }
                                var players = server.getPlayerList().getPlayers();
                                players.forEach(p -> CombatManager.get().setAlwaysActive(p.getUUID(), true));
                                int count = players.size();
                                ctx.getSource().sendSuccess(() -> Component.literal("Modo de combate permanente ativado para ")
                                        .append(Component.literal(String.valueOf(count)).withStyle(ChatFormatting.WHITE))
                                        .append(Component.literal(" jogadores.").withStyle(ChatFormatting.GREEN)), true);
                                return count;
                            }

                            // Existing behaviour: toggle permanent combat mode for the executing player
                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                            boolean currently = CombatManager.get().isAlwaysActive(player.getUUID());

                            // If the player is trying to *deactivate* permanent combat mode, require SAFE zone.
                            if (currently) {
                                var area = org.lupz.doomsdayessentials.combat.AreaManager.get()
                                        .getAreaAt(player.serverLevel(), player.blockPosition());
                                if (area == null || area.getType() != org.lupz.doomsdayessentials.combat.AreaType.SAFE) {
                                    player.sendSystemMessage(Component.literal("§cVocê precisa estar em uma zona segura para desativar o modo de combate permanente."));
                                    return 0;
                                }
                            }

                            CombatManager.get().setAlwaysActive(player.getUUID(), !currently);
                            Component msg = Component.literal("Modo de combate permanente ")
                                    .append(Component.literal(!currently ? "ativado" : "desativado").withStyle(!currently ? ChatFormatting.GREEN : ChatFormatting.RED));
                            ctx.getSource().sendSuccess(() -> msg, false);
                            return 1;
                        })
                        // Admin shortcut: /combat activate all – enable permanent combat for every online player
                        .then(Commands.literal("all")
                                .requires(src -> src.hasPermission(2))
                                .executes(ctx -> {
                                    var server = ctx.getSource().getServer();
                                    if (server == null) return 0;
                                    var cm = CombatManager.get();
                                    long count = server.getPlayerList().getPlayers().stream()
                                            .filter(p -> !cm.isAlwaysActive(p.getUUID()))
                                            .peek(p -> cm.setAlwaysActive(p.getUUID(), true))
                                            .count();
                                    ctx.getSource().sendSuccess(() -> Component.literal("Modo de combate permanente ativado para ")
                                            .append(Component.literal(String.valueOf(count)).withStyle(ChatFormatting.WHITE))
                                            .append(Component.literal(" jogadores.").withStyle(ChatFormatting.GREEN)), true);
                                    return (int) count;
                                }))
                )
                .then(Commands.literal("check")
                        .then(Commands.argument("target", EntityArgument.player())
                                .executes(ctx -> {
                                    var player = EntityArgument.getPlayer(ctx, "target");
                                    boolean inCombat = CombatManager.get().isInCombat(player.getUUID());
                                    if (!inCombat) {
                                        ctx.getSource().sendSuccess(() -> Component.literal(player.getName().getString())
                                                .withStyle(ChatFormatting.WHITE)
                                                .append(Component.literal(" is not in combat.").withStyle(ChatFormatting.GREEN)), false);
                                    } else {
                                        int seconds = CombatManager.get().getRemainingTicks(player.getUUID()) / 20;
                                        ctx.getSource().sendSuccess(() -> Component.literal(player.getName().getString())
                                                .withStyle(ChatFormatting.WHITE)
                                                .append(Component.literal(" in combat for ").withStyle(ChatFormatting.RED))
                                                .append(Component.literal(String.valueOf(seconds)).withStyle(ChatFormatting.WHITE))
                                                .append(Component.literal(" more seconds.").withStyle(ChatFormatting.RED)), false);
                                    }
                                    return 1;
                                })))
                .executes(ctx -> {
                    if (ctx.getSource().getEntity() == null) {
                        ctx.getSource().sendFailure(Component.literal("Only players can use this command.").withStyle(ChatFormatting.RED));
                        return 0;
                    }
                    var player = ctx.getSource().getPlayerOrException();
                    boolean inCombat = CombatManager.get().isInCombat(player.getUUID());
                    if (!inCombat) {
                        ctx.getSource().sendSuccess(() -> Component.literal("You are not in combat.").withStyle(ChatFormatting.GREEN), false);
                    } else {
                        int ticks = CombatManager.get().getRemainingTicks(player.getUUID());
                        int seconds = ticks / 20;
                        ctx.getSource().sendSuccess(() -> Component.literal("In combat for ")
                                .withStyle(ChatFormatting.RED)
                                .append(Component.literal(String.valueOf(seconds)).withStyle(ChatFormatting.WHITE))
                                .append(Component.literal(" more seconds.").withStyle(ChatFormatting.RED)), false);
                    }
                    return 1;
                }));
    }
} 