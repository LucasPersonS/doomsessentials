package org.lupz.doomsdayessentials.professions.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkHooks;
import org.lupz.doomsdayessentials.professions.EngenheiroProfession;
import org.lupz.doomsdayessentials.professions.menu.EngineerCraftMenuProvider;
import org.lupz.doomsdayessentials.professions.shop.EngineerShopUtil;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraft.world.item.Item;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public final class EngenheiroCommand {
    private EngenheiroCommand() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher){
        dispatcher.register(Commands.literal("engenheiro")
            .then(Commands.literal("craft").requires(src -> src.getEntity() instanceof ServerPlayer)
                .executes(ctx -> {
                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                    if(!EngenheiroProfession.isEngineer(player)){
                        player.sendSystemMessage(Component.translatable("profession.engenheiro.not_engineer"));
                        return 0;
                    }
                    NetworkHooks.openScreen(player, new EngineerCraftMenuProvider());
                    return 1;
                }))
            .then(Commands.literal("add")
                .requires(src -> src.hasPermission(2))
                .then(Commands.argument("args", StringArgumentType.greedyString())
                    .suggests(EngenheiroCommand::suggestAllItems)
                    .executes(EngenheiroCommand::addRecipe)))
            .then(Commands.literal("reload")
                .requires(src -> src.hasPermission(2))
                .executes(ctx -> {
                    EngineerShopUtil.loadConfig();
                    ctx.getSource().sendSuccess(() -> Component.translatable("command.engineer.reloaded"), true);
                    return 1;
                }))
            .then(Commands.literal("list")
                .executes(EngenheiroCommand::listRecipes))
            .then(Commands.literal("buy")
                .then(Commands.argument("item", StringArgumentType.word())
                    .suggests(EngenheiroCommand::suggest)
                    .executes(EngenheiroCommand::buyItem)))
        );
    }

    private static CompletableFuture<Suggestions> suggest(CommandContext<CommandSourceStack> c, SuggestionsBuilder b){
        EngineerShopUtil.getEntries().keySet().forEach(b::suggest); return b.buildFuture();
    }

    private static CompletableFuture<Suggestions> suggestAllItems(CommandContext<CommandSourceStack> c, SuggestionsBuilder b) {
        String remaining = b.getRemaining();
        String[] tokens = remaining.split(" ", -1);

        if (tokens.length == 0) {
            return Suggestions.empty();
        }

        int currentTokenIndex = tokens.length - 1;
        
        // Suggest items for odd-indexed tokens (0-indexed)
        // e.g., <count> <item> <count> <item>
        // indices:   0      1      2      3
        if (currentTokenIndex % 2 == 1) {
            int startOfToken = remaining.lastIndexOf(" ") + 1;
            SuggestionsBuilder itemSuggestionBuilder = b.createOffset(b.getStart() + startOfToken);
            return SharedSuggestionProvider.suggestResource(ForgeRegistries.ITEMS.getKeys().stream(), itemSuggestionBuilder);
        }

        // It's a count token, so don't suggest anything.
        return Suggestions.empty();
    }

    private static int listRecipes(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        Map<String, EngineerShopUtil.Entry> recipes = EngineerShopUtil.getEntries();

        if (recipes.isEmpty()) {
            source.sendSuccess(() -> Component.translatable("command.generic.no_recipes"), false);
            return 1;
        }

        source.sendSuccess(() -> Component.translatable("command.engineer.list.header"), false);
        for (Map.Entry<String, EngineerShopUtil.Entry> recipeEntry : recipes.entrySet()) {
            String alias = recipeEntry.getKey();
            EngineerShopUtil.Entry details = recipeEntry.getValue();
            Item outputItem = ForgeRegistries.ITEMS.getValue(details.outputId());
            String outputName = outputItem != null ? outputItem.getDescription().getString() : details.outputId().toString();

            MutableComponent recipeComp = Component.literal("§a" + alias + " -> " + details.outputCount() + "x " + outputName);
            source.sendSuccess(() -> recipeComp, false);

            MutableComponent costsComp = Component.literal("  §fCosts: ");
            for (Map.Entry<ResourceLocation, Integer> costEntry : details.costs().entrySet()) {
                Item costItem = ForgeRegistries.ITEMS.getValue(costEntry.getKey());
                String costName = costItem != null ? costItem.getDescription().getString() : costEntry.getKey().toString();
                costsComp.append(Component.literal(costEntry.getValue() + "x " + costName + " "));
            }
            source.sendSuccess(() -> costsComp, false);
        }

        return 1;
    }

    private static int addRecipe(CommandContext<CommandSourceStack> ctx) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        var held = player.getMainHandItem();
        if(held.isEmpty()) {
            player.sendSystemMessage(Component.literal("§cYou must hold the output item in your main hand."));
            return 0;
        }

        String argStr = StringArgumentType.getString(ctx, "args");
        String[] tokens = argStr.split(" ");
        if(tokens.length < 2 || tokens.length % 2 != 0) {
            player.sendSystemMessage(Component.literal("§cUsage: /engenheiro add <count1> <item1> [<count2> <item2> ...]"));
            return 0;
        }

        Map<ResourceLocation,Integer> costs = new LinkedHashMap<>();
        for(int i=0;i<tokens.length;i+=2){
            int amt;
            try {
                amt = Integer.parseInt(tokens[i]);
            } catch(NumberFormatException e) {
                player.sendSystemMessage(Component.literal("§cInvalid amount: " + tokens[i]));
                return 0;
            }
            ResourceLocation id = ResourceLocation.tryParse(tokens[i+1]);
            if(id == null || !ForgeRegistries.ITEMS.containsKey(id)) {
                player.sendSystemMessage(Component.literal("§cInvalid item: " + tokens[i+1]));
                return 0;
            }
            costs.put(id, amt);
        }

        ResourceLocation outId = ForgeRegistries.ITEMS.getKey(held.getItem());
        String alias = outId.getPath();
        EngineerShopUtil.addRecipe(alias, new EngineerShopUtil.Entry(outId, held.getCount(), costs));
        EngineerShopUtil.loadConfig(); // Reload recipes
        player.sendSystemMessage(Component.literal("§aRecipe for " + alias + " has been added/updated."));
        return 1;
    }

    private static int buyItem(CommandContext<CommandSourceStack> ctx) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        if(!EngenheiroProfession.isEngineer(player)) { player.sendSystemMessage(Component.translatable("profession.engenheiro.not_engineer")); return 0; }
        String alias = StringArgumentType.getString(ctx, "item");
        var entry = EngineerShopUtil.getEntries().get(alias);
        if(entry==null) { player.sendSystemMessage(Component.literal("Item não encontrado.")); return 0; }
        var outItem  = ForgeRegistries.ITEMS.getValue(entry.outputId());
        if(outItem==null) return 0;

        for (var cost : entry.costs().entrySet()) {
            var item = ForgeRegistries.ITEMS.getValue(cost.getKey());
            if(item==null) { player.sendSystemMessage(Component.literal("Item de custo inválido.")); return 0; }
            int avail = player.getInventory().countItem(item);
            if(avail < cost.getValue()) {
                player.sendSystemMessage(Component.literal("§cVocê precisa de mais " + item.getDescription().getString()));
                return 0;
            }
        }

        for (var cost : entry.costs().entrySet()) {
            var item = ForgeRegistries.ITEMS.getValue(cost.getKey());
            int toRemove = cost.getValue();
            for(int i=0;i<player.getInventory().getContainerSize();i++){
                var st=player.getInventory().getItem(i);
                if(st.is(item)){
                    int rem=Math.min(toRemove, st.getCount()); st.shrink(rem); toRemove-=rem; if(toRemove<=0) break;
                }
            }
        }

        player.getInventory().add(new net.minecraft.world.item.ItemStack(outItem, entry.outputCount()));
        player.sendSystemMessage(Component.literal("§aItem fabricado!"));
        return 1;
    }
} 