package org.lupz.doomsdayessentials.professions.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.lupz.doomsdayessentials.professions.menu.ProfissoesMenuProvider;
import org.lupz.doomsdayessentials.config.EssentialsConfig;

import java.util.concurrent.CompletableFuture;

public final class ProfissoesCommand {
    private ProfissoesCommand() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("profissoes")
                        .requires(src -> src.hasPermission(0))
                        .executes(ctx -> {
                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                            player.openMenu(new ProfissoesMenuProvider());
                            return 1;
                        })
                        .then(Commands.literal("loja")
                            .executes(ctx -> {
                                var player = ctx.getSource().getPlayerOrException();
                                net.minecraftforge.network.NetworkHooks.openScreen(player, new org.lupz.doomsdayessentials.professions.menu.ShopMenuProvider());
                                return 1;
                            })
                            .then(Commands.argument("item", StringArgumentType.word())
                                .suggests(ProfissoesCommand::suggestShopItems)
                                .executes(ProfissoesCommand::buyShopItem)))
        );
    }

    // ---------------- Shop helpers ----------------
    private record ShopEntry(net.minecraft.resources.ResourceLocation outputId, int outputCount, net.minecraft.resources.ResourceLocation costId, int costCount) {}

    private static java.util.Map<String, ShopEntry> getShopEntries() {
        java.util.Map<String, ShopEntry> map = new java.util.HashMap<>();
        for (String line : EssentialsConfig.SHOP_ITEMS.get()) {
            String[] parts = line.split(",");
            if (parts.length != 3) continue;
            try {
                net.minecraft.resources.ResourceLocation output = new net.minecraft.resources.ResourceLocation(parts[0]);
                net.minecraft.resources.ResourceLocation cost = new net.minecraft.resources.ResourceLocation(parts[1]);
                int costCount = Integer.parseInt(parts[2]);
                String alias = output.getPath();
                map.put(alias, new ShopEntry(output, 1, cost, costCount));
            } catch (Exception ignored) {}
        }
        return map;
    }

    private static CompletableFuture<com.mojang.brigadier.suggestion.Suggestions> suggestShopItems(CommandContext<CommandSourceStack> c, SuggestionsBuilder b) {
        getShopEntries().keySet().forEach(b::suggest);
        return b.buildFuture();
    }

    private static int listShop(CommandContext<CommandSourceStack> ctx) {
        var entries = getShopEntries();
        if (entries.isEmpty()) {
            ctx.getSource().sendFailure(Component.literal("Nenhum item configurado na loja."));
            return 0;
        }
        ctx.getSource().sendSuccess(() -> Component.literal("§6=== Loja de Profissões ==="), false);
        entries.forEach((key, entry) -> {
            var outItem = net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(entry.outputId);
            var costItem = net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(entry.costId);
            if (outItem == null || costItem == null) return;
            Component line = Component.literal(" - ")
                    .append(outItem.getDescription())
                    .append(Component.literal(" por "))
                    .append(Component.literal(entry.costCount + " " + costItem.getDescription().getString()))
                    .withStyle(style -> style.withClickEvent(new net.minecraft.network.chat.ClickEvent(net.minecraft.network.chat.ClickEvent.Action.RUN_COMMAND, "/profissoes loja " + key))
                            .withHoverEvent(new net.minecraft.network.chat.HoverEvent(net.minecraft.network.chat.HoverEvent.Action.SHOW_TEXT, Component.literal("Clique para comprar"))))
                    .withStyle(net.minecraft.ChatFormatting.AQUA);
            ctx.getSource().sendSuccess(() -> line, false);
        });
        return 1;
    }

    private static int buyShopItem(CommandContext<CommandSourceStack> ctx) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        String idStr = StringArgumentType.getString(ctx, "item");
        var entries = getShopEntries();
        ShopEntry entry = entries.get(idStr);
        if (entry == null) {
            ctx.getSource().sendFailure(Component.literal("Item não encontrado na loja."));
            return 0;
        }
        var player = ctx.getSource().getPlayerOrException();
        var costItem = net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(entry.costId);
        var outItem = net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(entry.outputId);
        if (costItem == null || outItem == null) {
            ctx.getSource().sendFailure(Component.literal("Item inválido na configuração."));
            return 0;
        }
        int available = player.getInventory().countItem(costItem);
        if (available < entry.costCount) {
            ctx.getSource().sendFailure(Component.literal("Você precisa de " + entry.costCount + " " + costItem.getDescription().getString() + " para comprar."));
            return 0;
        }
        // Remove cost
        int toRemove = entry.costCount;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            net.minecraft.world.item.ItemStack stack = player.getInventory().getItem(i);
            if (stack.is(costItem)) {
                int remove = Math.min(toRemove, stack.getCount());
                stack.shrink(remove);
                toRemove -= remove;
                if (toRemove <= 0) break;
            }
        }
        // Give output
        player.getInventory().add(new net.minecraft.world.item.ItemStack(outItem, entry.outputCount));
        player.sendSystemMessage(Component.literal("Compra realizada: " + outItem.getDescription().getString()));

        // Reopen professions screen to keep the shop GUI without forcing player to reopen manually
        net.minecraftforge.network.NetworkHooks.openScreen(player, new org.lupz.doomsdayessentials.professions.menu.ProfissoesMenuProvider());
        return 1;
    }
} 