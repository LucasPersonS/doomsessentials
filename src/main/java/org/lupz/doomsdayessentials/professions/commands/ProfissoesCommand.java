package org.lupz.doomsdayessentials.professions.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.lupz.doomsdayessentials.professions.menu.ProfissoesMenuProvider;
import org.lupz.doomsdayessentials.config.EssentialsConfig;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.*;
import java.util.concurrent.CompletableFuture;

public final class ProfissoesCommand {
    private ProfissoesCommand() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("profissoes")
                        .requires(src -> src.hasPermission(0))
                        .executes(ctx -> {
                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                            org.lupz.doomsdayessentials.network.PacketHandler.CHANNEL.send(net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> player), new org.lupz.doomsdayessentials.professions.menu.ProfessionSelectionOpener());
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
                        .then(Commands.literal("add")
                            .requires(src -> src.hasPermission(2))
                            .then(Commands.argument("args", StringArgumentType.greedyString())
                                .suggests(ProfissoesCommand::suggestAllItems)
                                .executes(ProfissoesCommand::addShopItem)))
        );
    }

    // ---------------- Shop helpers ----------------
    private record ShopEntry(net.minecraft.resources.ResourceLocation outputId, int outputCount, java.util.Map<net.minecraft.resources.ResourceLocation,Integer> costs) {
        net.minecraft.resources.ResourceLocation costId(){ return costs.keySet().stream().findFirst().orElse(null);} 
        int costCount(){return costs.values().stream().findFirst().orElse(0);} }

    private static java.util.Map<String, ShopEntry> getShopEntries() {
        java.util.Map<String, ShopEntry> map = new java.util.HashMap<>();
        for(String line: EssentialsConfig.SHOP_ITEMS.get()){
            String[] parts = line.split(",");
            try{
                if(parts.length>=4 && parts.length%2==0){
                    var out = net.minecraft.resources.ResourceLocation.tryParse(parts[0]);
                    int outCount = Integer.parseInt(parts[1]);
                    java.util.Map<net.minecraft.resources.ResourceLocation,Integer> costs = new java.util.LinkedHashMap<>();
                    for(int i=2;i<parts.length;i+=2){
                        var cid = net.minecraft.resources.ResourceLocation.tryParse(parts[i]);
                        int ccount = Integer.parseInt(parts[i+1]);
                        costs.put(cid, ccount);
                    }
                    map.put(out.getPath(), new ShopEntry(out, outCount, costs));
                }
            }catch(Exception ignored){}
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
            var costItem = net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(entry.costId());
            if (outItem == null || costItem == null) return;
            Component line = Component.literal(" - ")
                    .append(outItem.getDescription())
                    .append(Component.literal(" por "))
                    .append(Component.literal(entry.costCount() + " " + costItem.getDescription().getString()))
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
        var outItem = net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(entry.outputId);
        if (outItem == null) {
            ctx.getSource().sendFailure(Component.literal("Item inválido na configuração."));
            return 0;
        }
        // verify all costs
        for(var cost: entry.costs().entrySet()){
            var item = net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(cost.getKey());
            if(item==null){ctx.getSource().sendFailure(Component.literal("Custo inválido na configuração.")); return 0;}
            int available = player.getInventory().countItem(item);
            if(available < cost.getValue()){
                ctx.getSource().sendFailure(Component.literal("Você precisa de mais " + item.getDescription().getString()));
                return 0;
            }
        }
        // remove costs
        for(var cost: entry.costs().entrySet()){
            var item = net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(cost.getKey());
            int toRemove = cost.getValue();
            for(int i=0;i<player.getInventory().getContainerSize();i++){
                var stack=player.getInventory().getItem(i);
                if(stack.is(item)){
                    int rem=Math.min(toRemove, stack.getCount()); stack.shrink(rem); toRemove-=rem; if(toRemove<=0) break;
                }
            }
        }
        // Give output
        player.getInventory().add(new net.minecraft.world.item.ItemStack(outItem, entry.outputCount));
        player.sendSystemMessage(Component.literal("Compra realizada: " + outItem.getDescription().getString()));

        // Reopen professions screen to keep the shop GUI without forcing player to reopen manually
        net.minecraftforge.network.NetworkHooks.openScreen(player, new org.lupz.doomsdayessentials.professions.menu.ProfissoesMenuProvider());
        return 1;
    }

    private static int addShopItem(CommandContext<CommandSourceStack> ctx) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        var player = ctx.getSource().getPlayerOrException();
        var held = player.getMainHandItem();
        if (held.isEmpty()) {
            ctx.getSource().sendFailure(net.minecraft.network.chat.Component.literal("Segure um item na mão principal para adicionar."));
            return 0;
        }
        String argStr = StringArgumentType.getString(ctx, "args");
        String[] tokens = argStr.split(" ");
        if(tokens.length<2 || tokens.length%2!=0){ctx.getSource().sendFailure(Component.literal("Uso: /profissoes add <count1> <item1> [<count2> <item2> ...]")); return 0;}

        // Validate items
        Map<net.minecraft.resources.ResourceLocation,Integer> costs = new LinkedHashMap<>();
        for(int i=0;i<tokens.length;i+=2){
            int amt;
            try{amt=Integer.parseInt(tokens[i]);}catch(NumberFormatException e){ctx.getSource().sendFailure(Component.literal("Número inválido: "+tokens[i]));return 0;}
            net.minecraft.resources.ResourceLocation cid = net.minecraft.resources.ResourceLocation.tryParse(tokens[i+1]);
            if(!net.minecraftforge.registries.ForgeRegistries.ITEMS.containsKey(cid)){ctx.getSource().sendFailure(Component.literal("Item não existe: "+cid));return 0;}
            costs.put(cid, amt);
        }

        var outItem = net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(held.getItem());
        if(outItem==null){ctx.getSource().sendFailure(Component.literal("Item na mão não registrado."));return 0;}
        int outputCount = held.getCount();

        StringBuilder sb = new StringBuilder();
        sb.append(outItem).append(",").append(outputCount);
        costs.forEach((id,amt)-> sb.append(",").append(id).append(",").append(amt));
        String newLine = sb.toString();

        java.util.List<String> list = new java.util.ArrayList<>(EssentialsConfig.SHOP_ITEMS.get().stream().map(Object::toString).collect(java.util.stream.Collectors.toList()));
        // Remove any existing entry for same output item
        list.removeIf(s -> s.startsWith(outItem.toString() + ","));
        list.add(newLine);
        EssentialsConfig.SHOP_ITEMS.set(list);

        ctx.getSource().sendSuccess(() -> net.minecraft.network.chat.Component.literal("Item adicionado/atualizado na loja: " + held.getHoverName().getString()), false);
        return 1;
    }

    private static CompletableFuture<com.mojang.brigadier.suggestion.Suggestions> suggestAllItems(CommandContext<CommandSourceStack> c, SuggestionsBuilder b) {
        String remaining = b.getRemaining();
        
        // Parse the arguments to find what we're currently typing
        String[] tokens = remaining.split(" ");
        String currentToken = tokens.length > 0 ? tokens[tokens.length - 1] : "";
        
        // If we're typing a number (first token of a pair), don't suggest items
        if (tokens.length % 2 == 1 && currentToken.matches("\\d+")) {
            return b.buildFuture();
        }
        
        // If we're typing an item ID, suggest items
        if (tokens.length % 2 == 0 || currentToken.contains(":")) {
            String searchTerm = currentToken.toLowerCase();
            
            // Only suggest if we have at least 2 characters
            if (searchTerm.length() < 2) {
                return b.buildFuture();
            }
            
            ForgeRegistries.ITEMS.getKeys().stream()
                .filter(rl -> rl.toString().toLowerCase().contains(searchTerm))
                .limit(20) // Limit suggestions to avoid overwhelming
                .forEach(rl -> b.suggest(rl.toString()));
        }
        
        return b.buildFuture();
    }
} 