package org.lupz.doomsdayessentials.professions.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkHooks;
import org.lupz.doomsdayessentials.professions.EngenheiroProfession;
import org.lupz.doomsdayessentials.professions.menu.EngineerCraftMenuProvider;
import org.lupz.doomsdayessentials.professions.shop.EngineerShopUtil;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistries;
import java.util.*;
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
                        })
                        .then(Commands.literal("add")
                            .requires(src -> src.hasPermission(2))
                            .then(Commands.argument("args", StringArgumentType.greedyString())
                                .suggests(EngenheiroCommand::suggestAllItems)
                                .executes(EngenheiroCommand::addItem)))
                        .then(Commands.literal("buy")
                            .then(Commands.argument("item", StringArgumentType.word())
                                .suggests(EngenheiroCommand::suggest)
                                .executes(EngenheiroCommand::buyItem))))
        );
    }

    // ---------- Helpers ----------
    private static CompletableFuture<Suggestions> suggest(CommandContext<CommandSourceStack> c, SuggestionsBuilder b){
        EngineerShopUtil.getEntries().keySet().forEach(b::suggest); return b.buildFuture(); }

    private static CompletableFuture<Suggestions> suggestAllItems(CommandContext<CommandSourceStack> c, SuggestionsBuilder b){
        // Only suggest if the builder is empty or has a partial match
        String remaining = b.getRemaining().toLowerCase();
        if (remaining.isEmpty() || remaining.length() < 3) {
            // Don't suggest if input is too short to avoid overwhelming autocomplete
            return b.buildFuture();
        }
        
        ForgeRegistries.ITEMS.getKeys().stream()
            .filter(rl -> rl.toString().toLowerCase().contains(remaining))
            .limit(20) // Limit suggestions to avoid overwhelming
            .forEach(rl -> b.suggest(rl.toString()));
        return b.buildFuture();
    }

    private static int buyItem(CommandContext<CommandSourceStack> ctx) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        if(!EngenheiroProfession.isEngineer(player)) { player.sendSystemMessage(Component.translatable("profession.engenheiro.not_engineer")); return 0; }
        String alias = StringArgumentType.getString(ctx, "item");
        var entry = EngineerShopUtil.getEntries().get(alias);
        if(entry==null) { player.sendSystemMessage(Component.literal("Item não encontrado.")); return 0; }
        var outItem  = ForgeRegistries.ITEMS.getValue(entry.outputId());
        if(outItem==null) return 0;

        // verify all costs
        for (var cost : entry.costs().entrySet()) {
            var item = ForgeRegistries.ITEMS.getValue(cost.getKey());
            if(item==null) { player.sendSystemMessage(Component.literal("Item de custo inválido.")); return 0; }
            int avail = player.getInventory().countItem(item);
            if(avail < cost.getValue()) {
                player.sendSystemMessage(Component.literal("§cVocê precisa de mais " + item.getDescription().getString()));
                return 0;
            }
        }
        // remove costs
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

    private static int addItem(CommandContext<CommandSourceStack> ctx) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        var held = player.getMainHandItem();
        if(held.isEmpty()) { player.sendSystemMessage(Component.literal("Segure o item de saída na mão.")); return 0; }

        String argStr = StringArgumentType.getString(ctx, "args");
        String[] tokens = argStr.split(" ");
        if(tokens.length < 2 || tokens.length % 2 != 0) {
            player.sendSystemMessage(Component.literal("Uso: /engenheiro craft add <count1> <item1> [<count2> <item2> ...]"));
            return 0;
        }

        Map<ResourceLocation,Integer> costs = new LinkedHashMap<>();
        for(int i=0;i<tokens.length;i+=2){
            int amt;
            try{ amt = Integer.parseInt(tokens[i]); }catch(NumberFormatException e){ player.sendSystemMessage(Component.literal("Quantidade inválida: "+tokens[i])); return 0; }
            ResourceLocation id = new ResourceLocation(tokens[i+1]);
            if(!ForgeRegistries.ITEMS.containsKey(id)) { player.sendSystemMessage(Component.literal("Item inválido: "+id)); return 0; }
            costs.put(id, amt);
        }

        ResourceLocation outId = ForgeRegistries.ITEMS.getKey(held.getItem());
        String alias = outId.getPath();
        EngineerShopUtil.addOrReplace(alias, new EngineerShopUtil.Entry(outId, held.getCount(), costs));
        player.sendSystemMessage(Component.literal("§aItem adicionado/atualizado na oficina."));
        return 1;
    }
} 