package org.lupz.doomsdayessentials.kit;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lupz.doomsdayessentials.EssentialsMod;

import java.util.ArrayList;
import java.util.List;

@Mod.EventBusSubscriber(modid = EssentialsMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class KitCommand {

    private static final SuggestionProvider<CommandSourceStack> TIER_SUGGESTIONS = (ctx, builder) -> {
        for (String tier : KitManager.TIERS) {
            builder.suggest(tier);
        }
        return builder.buildFuture();
    };

    private static final SuggestionProvider<CommandSourceStack> COOLDOWN_SUGGESTIONS = (ctx, builder) -> {
        builder.suggest("daily");
        builder.suggest("weekly");
        builder.suggest("monthly");
        return builder.buildFuture();
    };

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent e) {
        register(e.getDispatcher());
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("dooms")
                        .then(Commands.literal("kit")
                                .executes(KitCommand::openMenu)
                                .then(Commands.literal("create")
                                        .requires(src -> src.hasPermission(2))
                                        .then(Commands.argument("tier", StringArgumentType.word())
                                                .suggests(TIER_SUGGESTIONS)
                                                .executes(ctx -> createKit(ctx, Kit.CooldownType.DAILY))
                                                .then(Commands.argument("cooldown", StringArgumentType.word())
                                                        .suggests(COOLDOWN_SUGGESTIONS)
                                                        .executes(KitCommand::createKitWithCooldown))))
                                .then(Commands.literal("reload")
                                        .requires(src -> src.hasPermission(3))
                                        .executes(KitCommand::reloadKits))
                                .then(Commands.literal("delete")
                                        .requires(src -> src.hasPermission(2))
                                        .then(Commands.argument("tier", StringArgumentType.word())
                                                .suggests(TIER_SUGGESTIONS)
                                                .then(Commands.argument("cooldown", StringArgumentType.word())
                                                        .suggests(COOLDOWN_SUGGESTIONS)
                                                        .executes(KitCommand::deleteKit))))
                                .then(Commands.literal("resetcooldown")
                                        .requires(src -> src.hasPermission(2))
                                        .then(Commands
                                                .argument("targets",
                                                        net.minecraft.commands.arguments.EntityArgument.players())
                                                .then(Commands.argument("tier", StringArgumentType.word())
                                                        .suggests(TIER_SUGGESTIONS)
                                                        .then(Commands.argument("cooldown", StringArgumentType.word())
                                                                .suggests(COOLDOWN_SUGGESTIONS)
                                                                .executes(KitCommand::resetCooldownForPlayers)))))
                                .then(Commands.literal("setcooldown")
                                        .requires(src -> src.hasPermission(2))
                                        .then(Commands
                                                .argument("targets",
                                                        net.minecraft.commands.arguments.EntityArgument.players())
                                                .then(Commands.argument("tier", StringArgumentType.word())
                                                        .suggests(TIER_SUGGESTIONS)
                                                        .then(Commands.argument("cooldown", StringArgumentType.word())
                                                                .suggests(COOLDOWN_SUGGESTIONS)
                                                                .executes(KitCommand::setCooldownForPlayers)))))));
    }

    private static int openMenu(CommandContext<CommandSourceStack> ctx) {
        if (!(ctx.getSource().getEntity() instanceof ServerPlayer sp)) {
            ctx.getSource().sendFailure(Component.literal("§cApenas jogadores podem usar este comando."));
            return 0;
        }

        org.lupz.doomsdayessentials.kit.menu.KitMenuProvider.open(sp, Kit.CooldownType.DAILY);
        return 1;
    }

    private static int createKit(CommandContext<CommandSourceStack> ctx, Kit.CooldownType defaultCooldown) {
        if (!(ctx.getSource().getEntity() instanceof ServerPlayer sp)) {
            ctx.getSource().sendFailure(Component.literal("§cApenas jogadores podem usar este comando."));
            return 0;
        }

        String tier = StringArgumentType.getString(ctx, "tier").toLowerCase();
        if (!KitManager.isValidTier(tier)) {
            ctx.getSource()
                    .sendFailure(Component.literal("§cTier inválido. Use: free, sobrevivente, infectado, dissoluto"));
            return 0;
        }

        List<ItemStack> items = new ArrayList<>();
        for (int i = 0; i < sp.getInventory().getContainerSize(); i++) {
            ItemStack stack = sp.getInventory().getItem(i);
            if (!stack.isEmpty()) {
                items.add(stack.copy());
            }
        }

        if (items.isEmpty()) {
            ctx.getSource().sendFailure(Component.literal("§cSeu inventário está vazio!"));
            return 0;
        }

        boolean success = KitManager.createKit(tier, defaultCooldown, items);
        if (success) {
            ctx.getSource().sendSuccess(() -> Component.literal(
                    "§aKit criado para tier §e" + tier + " §acom cooldown §e" + defaultCooldown.getId() + "§a!"), true);
            ctx.getSource().sendSuccess(() -> Component.literal("§7Total de itens: §f" + items.size()), false);
            return 1;
        } else {
            ctx.getSource().sendFailure(Component.literal("§cFalha ao criar kit."));
            return 0;
        }
    }

    private static int createKitWithCooldown(CommandContext<CommandSourceStack> ctx) {
        String cooldownStr = StringArgumentType.getString(ctx, "cooldown").toLowerCase();
        Kit.CooldownType cooldownType = Kit.CooldownType.fromId(cooldownStr);

        if (cooldownType == null) {
            ctx.getSource().sendFailure(Component.literal("§cCooldown inválido. Use: daily, weekly, monthly"));
            return 0;
        }

        return createKit(ctx, cooldownType);
    }

    private static int reloadKits(CommandContext<CommandSourceStack> ctx) {
        KitManager.load();
        ctx.getSource().sendSuccess(() -> Component.literal("§aKits recarregados do disco!"), true);
        return 1;
    }

    private static int deleteKit(CommandContext<CommandSourceStack> ctx) {
        String tier = StringArgumentType.getString(ctx, "tier").toLowerCase();
        String cooldownStr = StringArgumentType.getString(ctx, "cooldown").toLowerCase();

        if (!KitManager.isValidTier(tier)) {
            ctx.getSource().sendFailure(Component.literal("§cTier inválido."));
            return 0;
        }

        Kit.CooldownType cooldownType = Kit.CooldownType.fromId(cooldownStr);
        if (cooldownType == null) {
            ctx.getSource().sendFailure(Component.literal("§cCooldown inválido."));
            return 0;
        }

        boolean success = KitManager.deleteKit(tier, cooldownType);
        if (success) {
            ctx.getSource().sendSuccess(
                    () -> Component.literal("§aKit deletado: §e" + tier + " " + cooldownType.getId()), true);
            return 1;
        } else {
            ctx.getSource().sendFailure(Component.literal("§cKit não encontrado."));
            return 0;
        }
    }

    private static int resetCooldownForPlayers(CommandContext<CommandSourceStack> ctx) {
        try {
            java.util.Collection<ServerPlayer> targets = net.minecraft.commands.arguments.EntityArgument.getPlayers(ctx,
                    "targets");
            String tier = StringArgumentType.getString(ctx, "tier").toLowerCase();
            String cooldownStr = StringArgumentType.getString(ctx, "cooldown").toLowerCase();

            if (!KitManager.isValidTier(tier)) {
                ctx.getSource().sendFailure(Component.literal("§cTier inválido."));
                return 0;
            }

            Kit.CooldownType cooldownType = Kit.CooldownType.fromId(cooldownStr);
            if (cooldownType == null) {
                ctx.getSource().sendFailure(Component.literal("§cCooldown inválido."));
                return 0;
            }

            int count = 0;
            for (ServerPlayer player : targets) {
                KitCooldownManager.resetCooldown(player, tier, cooldownType);
                player.sendSystemMessage(Component
                        .literal("§aSeu cooldown do kit §e" + tier + " " + cooldownType.getId() + " §afoi resetado!"));
                count++;
            }

            final int finalCount = count;
            ctx.getSource().sendSuccess(() -> Component.literal("§aCooldown resetado para §e" + finalCount
                    + " §ajogador(es): §f" + tier + " " + cooldownType.getId()), true);
            return count;
        } catch (Exception e) {
            ctx.getSource().sendFailure(Component.literal("§cErro: " + e.getMessage()));
            return 0;
        }
    }

    private static int setCooldownForPlayers(CommandContext<CommandSourceStack> ctx) {
        try {
            java.util.Collection<ServerPlayer> targets = net.minecraft.commands.arguments.EntityArgument.getPlayers(ctx,
                    "targets");
            String tier = StringArgumentType.getString(ctx, "tier").toLowerCase();
            String cooldownStr = StringArgumentType.getString(ctx, "cooldown").toLowerCase();

            if (!KitManager.isValidTier(tier)) {
                ctx.getSource().sendFailure(Component.literal("§cTier inválido."));
                return 0;
            }

            Kit.CooldownType cooldownType = Kit.CooldownType.fromId(cooldownStr);
            if (cooldownType == null) {
                ctx.getSource().sendFailure(Component.literal("§cCooldown inválido."));
                return 0;
            }

            int count = 0;
            for (ServerPlayer player : targets) {
                KitCooldownManager.setClaimed(player, tier, cooldownType);
                player.sendSystemMessage(Component
                        .literal("§eCooldown do kit §e" + tier + " " + cooldownType.getId() + " §efoi ativado!"));
                count++;
            }

            final int finalCount = count;
            ctx.getSource().sendSuccess(() -> Component.literal("§aCooldown ativado para §e" + finalCount
                    + " §ajogador(es): §f" + tier + " " + cooldownType.getId()), true);
            return count;
        } catch (Exception e) {
            ctx.getSource().sendFailure(Component.literal("§cErro: " + e.getMessage()));
            return 0;
        }
    }
}
