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