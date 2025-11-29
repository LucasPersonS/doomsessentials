package org.lupz.doomsdayessentials.lootbox.farming;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lupz.doomsdayessentials.EssentialsMod;
import org.lupz.doomsdayessentials.lootbox.LootboxManager;

/**
 * Comando /lootboxfarm para gerenciar o sistema de farming de lootboxes
 */
@Mod.EventBusSubscriber(modid = EssentialsMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class LootboxFarmCommand {

    private static final SuggestionProvider<CommandSourceStack> RARITY_SUGGESTIONS = (ctx, builder) -> {
        for (String rarity : LootboxManager.RARITIES) {
            builder.suggest(rarity);
        }
        return builder.buildFuture();
    };

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("lootboxfarm")
                        // /lootboxfarm stats - Ver progresso semanal
                        .then(Commands.literal("stats")
                                .executes(LootboxFarmCommand::showStats))
                        // /lootboxfarm give <player> <rarity> <amount> - Dar fragmentos (admin)
                        .then(Commands.literal("give")
                                .requires(src -> src.hasPermission(2))
                                .then(Commands.argument("player", EntityArgument.player())
                                        .then(Commands.argument("rarity", StringArgumentType.word())
                                                .suggests(RARITY_SUGGESTIONS)
                                                .then(Commands.argument("amount", IntegerArgumentType.integer(1, 1000))
                                                        .executes(LootboxFarmCommand::giveFragments)))))
                        // /lootboxfarm reset <player> - Resetar progresso (admin)
                        .then(Commands.literal("reset")
                                .requires(src -> src.hasPermission(2))
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(LootboxFarmCommand::resetPlayer)))
                        // /lootboxfarm resetall - Resetar todos (admin)
                        .then(Commands.literal("resetall")
                                .requires(src -> src.hasPermission(3))
                                .executes(LootboxFarmCommand::resetAll)));
    }

    private static int showStats(CommandContext<CommandSourceStack> ctx) {
        if (!(ctx.getSource().getEntity() instanceof ServerPlayer player)) {
            ctx.getSource().sendFailure(Component.literal("§cApenas jogadores podem usar este comando."));
            return 0;
        }

        ctx.getSource().sendSuccess(() -> Component.literal("§6§l═══ PROGRESSO DE FARMING ═══"), false);
        ctx.getSource().sendSuccess(() -> Component.literal(""), false);

        for (String rarity : LootboxManager.RARITIES) {
            int current = LootboxFarmingManager.getFragmentsThisWeek(player.getUUID(), rarity);
            int limit = LootboxFarmingManager.getWeeklyLimit(rarity);
            double percentage = (current / (double) limit) * 100.0;

            String color = switch (rarity) {
                case "incomum" -> "§a";
                case "rara" -> "§9";
                case "epica" -> "§5";
                case "lendaria" -> "§6";
                default -> "§7";
            };

            String bar = createProgressBar(current, limit, 20);
            ctx.getSource().sendSuccess(() -> Component.literal(
                    color + "§l" + rarity.toUpperCase() + ": §f" + current + "§7/§f" + limit +
                            " §7(" + String.format("%.1f", percentage) + "%)"),
                    false);
            ctx.getSource().sendSuccess(() -> Component.literal("  " + bar), false);
        }

        ctx.getSource().sendSuccess(() -> Component.literal(""), false);
        ctx.getSource().sendSuccess(() -> Component.literal("§7Reset semanal: §fSegunda-feira 00:00"), false);

        return 1;
    }

    private static String createProgressBar(int current, int max, int length) {
        int filled = (int) ((current / (double) max) * length);
        StringBuilder bar = new StringBuilder("§8[");
        for (int i = 0; i < length; i++) {
            if (i < filled) {
                bar.append("§a█");
            } else {
                bar.append("§7█");
            }
        }
        bar.append("§8]");
        return bar.toString();
    }

    private static int giveFragments(CommandContext<CommandSourceStack> ctx) {
        try {
            ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
            String rarity = StringArgumentType.getString(ctx, "rarity").toLowerCase();
            int amount = IntegerArgumentType.getInteger(ctx, "amount");

            if (!LootboxManager.RARITIES.contains(rarity)) {
                ctx.getSource()
                        .sendFailure(Component.literal("§cRaridade inválida! Use: incomum, rara, epica, lendaria"));
                return 0;
            }

            int added = LootboxFarmingManager.addFragments(target, rarity, amount);

            if (added > 0) {
                ctx.getSource().sendSuccess(() -> Component.literal(
                        "§aDeu §e" + added + " §afragmentos §f" + rarity + " §apara §e" + target.getName().getString()),
                        true);
                return added;
            } else {
                ctx.getSource().sendFailure(Component.literal("§cJogador atingiu o limite semanal!"));
                return 0;
            }
        } catch (Exception e) {
            ctx.getSource().sendFailure(Component.literal("§cErro: " + e.getMessage()));
            return 0;
        }
    }

    private static int resetPlayer(CommandContext<CommandSourceStack> ctx) {
        try {
            ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
            LootboxFarmingManager.resetPlayer(target.getUUID());

            ctx.getSource().sendSuccess(() -> Component.literal(
                    "§aProgresso de farming resetado para §e" + target.getName().getString()), true);
            target.sendSystemMessage(
                    Component.literal("§eSeu progresso de farming de lootboxes foi resetado por um administrador."));

            return 1;
        } catch (Exception e) {
            ctx.getSource().sendFailure(Component.literal("§cErro: " + e.getMessage()));
            return 0;
        }
    }

    private static int resetAll(CommandContext<CommandSourceStack> ctx) {
        LootboxFarmingManager.resetAll();
        ctx.getSource().sendSuccess(() -> Component.literal("§aTodos os progressos de farming foram resetados!"), true);
        return 1;
    }
}
