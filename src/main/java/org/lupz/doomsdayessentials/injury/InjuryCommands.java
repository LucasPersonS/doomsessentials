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

@net.minecraftforge.fml.common.Mod.EventBusSubscriber(modid = org.lupz.doomsdayessentials.EssentialsMod.MOD_ID, bus = net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus.FORGE)
public class InjuryCommands {

    // ---------------------------------------------------------------------
    // Command registration lifecycle
    // ---------------------------------------------------------------------

    @net.minecraftforge.eventbus.api.SubscribeEvent
    public static void onRegisterCommands(net.minecraftforge.event.RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("machucado")
                .requires(src -> src.hasPermission(2))
                .then(Commands.literal("set")
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("level", IntegerArgumentType.integer(0))
                                        .executes(ctx -> {
                                            ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
                                            int level = IntegerArgumentType.getInteger(ctx, "level");
                                            int maxLevel = EssentialsConfig.MAX_INJURY_LEVEL.get();

                                            if (level > maxLevel) {
                                                ctx.getSource().sendFailure(Component.literal("§cO nível de ferimento não pode ser maior que " + maxLevel));
                                                return 0;
                                            }

                                            InjuryHelper.setInjuryLevel(target, level);
                                            ctx.getSource().sendSuccess(() -> Component.literal("§aNível de ferimento de " + target.getName().getString() + " definido para " + level), true);
                                            return 1;
                                        }))))
                .then(Commands.literal("forceheal")
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(ctx -> {
                                    ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
                                    InjuryHelper.setInjuryLevel(target, 0);
                                    InjuryHelper.revivePlayer(target);
                                    forceUnbedPlayer(target);
                                    ctx.getSource().sendSuccess(() -> Component.literal("§a" + target.getName().getString() + " foi totalmente curado e reanimado."), true);
                                    return 1;
                                })))
                .then(Commands.literal("forceunbed")
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(ctx -> {
                                    ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
                                    forceUnbedPlayer(target);
                                    ctx.getSource().sendSuccess(() -> Component.literal("§a" + target.getName().getString() + " foi forçadamente removido da cama médica."), true);
                                    return 1;
                                })))
                .then(Commands.literal("down")
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(ctx -> {
                                    ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
                                    InjuryHelper.downPlayer(target, null);
                                    ctx.getSource().sendSuccess(() -> Component.literal("§a" + target.getName().getString() + " foi derrubado."), true);
                                    return 1;
                                })))
                .then(Commands.literal("revive")
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(ctx -> {
                                    ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
                                    InjuryHelper.revivePlayer(target);
                                    ctx.getSource().sendSuccess(() -> Component.literal("§a" + target.getName().getString() + " foi revivido."), true);
                                    return 1;
                                }))));
    }

    private static void forceUnbedPlayer(ServerPlayer player) {
        if (player.getPersistentData().contains("healingBedLock")) {
            player.getPersistentData().remove("healingBedLock");
            player.getPersistentData().remove("healingBedX");
            player.getPersistentData().remove("healingBedY");
            player.getPersistentData().remove("healingBedZ");
            player.sendSystemMessage(Component.literal("§aVocê foi curado por um administrador."));
        }
    }
} 