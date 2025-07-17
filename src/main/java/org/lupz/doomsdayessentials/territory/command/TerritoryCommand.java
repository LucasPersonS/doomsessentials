package org.lupz.doomsdayessentials.territory.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lupz.doomsdayessentials.EssentialsMod;
import org.lupz.doomsdayessentials.combat.AreaManager;
import org.lupz.doomsdayessentials.combat.ManagedArea;
import org.lupz.doomsdayessentials.territory.TerritoryEventManager;
import net.minecraft.ChatFormatting;
import net.minecraft.resources.ResourceLocation;

@Mod.EventBusSubscriber(modid = EssentialsMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class TerritoryCommand {

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent e) {
        register(e.getDispatcher());
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        // Root command /territory (admin only)
        var territoryRoot = Commands.literal("territory")
                .requires(src -> src.hasPermission(2));

        // -----------------------------
        // /territory event ...
        var eventCmd = Commands.literal("event")
                .then(Commands.literal("start")
                        .then(Commands.argument("area", StringArgumentType.word()).suggests(TerritoryCommand::suggestAreaNames)
                                .then(Commands.argument("durationMinutes", IntegerArgumentType.integer(1))
                                        .then(Commands.argument("minPlayers", IntegerArgumentType.integer(1))
                                                .executes(TerritoryCommand::startEvent)))))
                .then(Commands.literal("stop").executes(TerritoryCommand::stopEvent))
                .then(Commands.literal("status").executes(TerritoryCommand::status))
                .then(Commands.literal("collect"));

        // -----------------------------
        // /territory generator ...
        var generatorCmd = Commands.literal("generator")
                .then(Commands.literal("info")
                        .then(Commands.argument("area", StringArgumentType.word()).suggests(TerritoryCommand::suggestAreaNames)
                                .executes(TerritoryCommand::generatorInfo)))
                .then(Commands.literal("set")
                        .then(Commands.argument("area", StringArgumentType.word()).suggests(TerritoryCommand::suggestAreaNames)
                                .then(Commands.argument("field", StringArgumentType.word()).suggests(TerritoryCommand::suggestFieldNames)
                                        .then(Commands.argument("value", StringArgumentType.greedyString())
                                                .executes(TerritoryCommand::generatorSet)))))
                .then(Commands.literal("additem")
                        .then(Commands.argument("area", StringArgumentType.word()).suggests(TerritoryCommand::suggestAreaNames)
                                .then(Commands.argument("item", StringArgumentType.string())
                                        .then(Commands.argument("perHour", IntegerArgumentType.integer(1))
                                                .executes(TerritoryCommand::generatorAddItem)))))
                .then(Commands.literal("delitem")
                        .then(Commands.argument("area", StringArgumentType.word()).suggests(TerritoryCommand::suggestAreaNames)
                                .then(Commands.argument("item", StringArgumentType.string())
                                        .executes(TerritoryCommand::generatorDelItem))))
                .then(Commands.literal("reload").executes(TerritoryCommand::reloadGenerators));

        territoryRoot.then(eventCmd).then(generatorCmd);
        dispatcher.register(territoryRoot);
    }

    private static int startEvent(CommandContext<CommandSourceStack> ctx) {
        String areaName = StringArgumentType.getString(ctx, "area");
        int durationMinutes = IntegerArgumentType.getInteger(ctx, "durationMinutes");
        int minPlayers = IntegerArgumentType.getInteger(ctx, "minPlayers");

        ManagedArea area = AreaManager.get().getArea(areaName);
        if (area == null) {
            ctx.getSource().sendFailure(Component.literal("Área não encontrada."));
            return 0;
        }
        if (TerritoryEventManager.get().isEventRunning()) {
            ctx.getSource().sendFailure(Component.literal("Já existe um evento ativo."));
            return 0;
        }
        boolean ok = TerritoryEventManager.get().startEvent(area, durationMinutes * 60, minPlayers);
        if (ok) {
            ctx.getSource().sendSuccess(() -> Component.literal("Evento iniciado com sucesso."), true);
            return 1;
        } else {
            ctx.getSource().sendFailure(Component.literal("Falha ao iniciar evento."));
            return 0;
        }
    }

    private static int stopEvent(CommandContext<CommandSourceStack> ctx) {
        if (!TerritoryEventManager.get().isEventRunning()) {
            ctx.getSource().sendFailure(Component.literal("Nenhum evento em andamento."));
            return 0;
        }
        TerritoryEventManager.get().stopEvent();
        ctx.getSource().sendSuccess(() -> Component.literal("Evento interrompido."), true);
        return 1;
    }

    private static int status(CommandContext<CommandSourceStack> ctx) {
        if (!TerritoryEventManager.get().isEventRunning()) {
            ctx.getSource().sendSuccess(() -> Component.literal("Nenhum evento em andamento."), false);
            return 0;
        }
        ctx.getSource().sendSuccess(() -> Component.literal("Um evento de território está em andamento."), false);
        return 1;
    }

    /**
     * @deprecated A partir de 0.9.0 use /organizacao recompensas
     */
    @Deprecated
    private static int collect(CommandContext<CommandSourceStack> ctx) {
        ctx.getSource().sendFailure(Component.literal("Comando obsoleto. Use /organizacao recompensas."));
        return 0;
    }

    private static int generatorInfo(CommandContext<CommandSourceStack> ctx) {
        String areaName = StringArgumentType.getString(ctx, "area");
        var data = org.lupz.doomsdayessentials.territory.ResourceGeneratorManager.get().get(areaName);
        if (data == null) {
            ctx.getSource().sendFailure(Component.literal("Gerador não encontrado."));
            return 0;
        }
        net.minecraft.network.chat.MutableComponent msg = Component.literal("§6[enerator Info] §e" + areaName + "\n");
        for (var entry : data.lootEntries) {
            Component line = Component.literal("  §a• §f" + entry.id + " §7(")
                    .append(Component.literal(entry.perHour + "/h").withStyle(ChatFormatting.AQUA))
                    .append(Component.literal(", armazenado: "))
                    .append(Component.literal(String.valueOf(entry.stored)).withStyle(ChatFormatting.YELLOW))
                    .append(Component.literal(")\n"));
            msg = msg.append(line);
        }
        msg.append(Component.literal("§bCapacidade: §f" + data.storageCap + "\n"));
        msg.append(Component.literal("§dDono: §f" + (data.ownerGuild==null?"§7-":data.ownerGuild)));
        net.minecraft.network.chat.Component finalMsg = msg;
        ctx.getSource().sendSuccess(() -> finalMsg, false);
        return 1;
    }

    private static int generatorSet(CommandContext<CommandSourceStack> ctx) {
        String areaName = StringArgumentType.getString(ctx, "area");
        String field = StringArgumentType.getString(ctx, "field");
        String value = StringArgumentType.getString(ctx, "value");
        var mgr = org.lupz.doomsdayessentials.territory.ResourceGeneratorManager.get();
        var data = mgr.createIfAbsent(areaName);
        try {
            switch (field) {
                case "loot" -> {
                    if (data.lootEntries.isEmpty()) data.lootEntries.add(new org.lupz.doomsdayessentials.territory.ResourceAreaData.LootEntry(value, 1));
                    else data.lootEntries.get(0).id = value;
                }
                case "perhour", "itemsPerHour" -> {
                    int v = Integer.parseInt(value);
                    if (data.lootEntries.isEmpty()) data.lootEntries.add(new org.lupz.doomsdayessentials.territory.ResourceAreaData.LootEntry("minecraft:stone", v));
                    else data.lootEntries.get(0).perHour = v;
                }
                case "cap", "storageCap" -> data.storageCap = Integer.parseInt(value);
                case "owner" -> data.ownerGuild = value.equalsIgnoreCase("null") ? null : value;
                default -> {
                    ctx.getSource().sendFailure(Component.literal("Campo desconhecido."));
                    return 0;
                }
            }
        } catch (NumberFormatException ex) {
            ctx.getSource().sendFailure(Component.literal("Valor inválido."));
            return 0;
        }
        mgr.save();
        ctx.getSource().sendSuccess(() -> Component.literal("Gerador atualizado."), true);
        return 1;
    }

    private static int reloadGenerators(CommandContext<CommandSourceStack> ctx) {
        org.lupz.doomsdayessentials.territory.ResourceGeneratorManager.get().reload();
        ctx.getSource().sendSuccess(() -> Component.literal("Geradores recarregados do Gdisco."), true);
        return 1;
    }

    private static java.util.concurrent.CompletableFuture<com.mojang.brigadier.suggestion.Suggestions> suggestAreaNames(CommandContext<CommandSourceStack> c, SuggestionsBuilder b) {
        // Suggest all defined areas
        org.lupz.doomsdayessentials.combat.AreaManager.get().getAreas().forEach(a -> b.suggest(a.getName()));
        return b.buildFuture();
    }

    private static java.util.concurrent.CompletableFuture<com.mojang.brigadier.suggestion.Suggestions> suggestFieldNames(CommandContext<CommandSourceStack> c, SuggestionsBuilder b) {
        b.suggest("loot");
        b.suggest("itemsPerHour");
        b.suggest("storageCap");
        b.suggest("owner");
        return b.buildFuture();
    }

    // ------------------------------------------------------------------
    // additem / delitem impl
    // ------------------------------------------------------------------

    private static int generatorAddItem(CommandContext<CommandSourceStack> ctx) {
        String area = StringArgumentType.getString(ctx, "area");
        String itemId = StringArgumentType.getString(ctx, "item");
        int perHour = IntegerArgumentType.getInteger(ctx, "perHour");

        var mgr = org.lupz.doomsdayessentials.territory.ResourceGeneratorManager.get();
        var data = mgr.createIfAbsent(area);

        ResourceLocation rl = ResourceLocation.tryParse(itemId);
        if (!net.minecraftforge.registries.ForgeRegistries.ITEMS.containsKey(rl)) {
            ctx.getSource().sendFailure(Component.literal("Item inválido."));
            return 0;
        }

        var existing = data.lootEntries.stream().filter(e -> e.id.equals(itemId)).findFirst().orElse(null);
        if (existing != null) {
            existing.perHour = perHour;
            ctx.getSource().sendSuccess(() -> Component.literal("Item atualizado."), true);
        } else {
            data.lootEntries.add(new org.lupz.doomsdayessentials.territory.ResourceAreaData.LootEntry(itemId, perHour));
            ctx.getSource().sendSuccess(() -> Component.literal("Item adicionado."), true);
        }
        mgr.save();
        return 1;
    }

    private static int generatorDelItem(CommandContext<CommandSourceStack> ctx) {
        String area = StringArgumentType.getString(ctx, "area");
        String itemId = StringArgumentType.getString(ctx, "item");
        var mgr = org.lupz.doomsdayessentials.territory.ResourceGeneratorManager.get();
        var data = mgr.get(area);
        if (data == null) {
            ctx.getSource().sendFailure(Component.literal("Gerador não encontrado."));
            return 0;
        }
        boolean removed = data.lootEntries.removeIf(e -> e.id.equals(itemId));
        if (removed) {
            mgr.save();
            ctx.getSource().sendSuccess(() -> Component.literal("Item removido."), true);
            return 1;
        }
        ctx.getSource().sendFailure(Component.literal("Item não encontrado."));
        return 0;
    }
} 