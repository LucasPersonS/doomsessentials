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
                .then(Commands.literal("collect").executes(TerritoryCommand::collect));

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

    private static int collect(CommandContext<CommandSourceStack> ctx) {
        if (!(ctx.getSource().getEntity() instanceof net.minecraft.server.level.ServerPlayer player)) {
            ctx.getSource().sendFailure(Component.literal("Somente jogadores podem coletar."));
            return 0;
        }
        var gman = org.lupz.doomsdayessentials.guild.GuildsManager.get(player.serverLevel());
        var guild = gman.getGuildByMember(player.getUUID());
        if (guild == null) {
            player.sendSystemMessage(Component.literal("Você não pertence a uma guilda."));
            return 0;
        }
        int total = org.lupz.doomsdayessentials.territory.ResourceGeneratorManager.get().collectForGuild(player, guild.getName());
        if (total == 0) {
            player.sendSystemMessage(Component.literal("Nenhum recurso para coletar."));
        } else {
            player.sendSystemMessage(Component.literal("Coletado " + total + " itens dos seus geradores."));
        }
        return 1;
    }

    private static int generatorInfo(CommandContext<CommandSourceStack> ctx) {
        String areaName = StringArgumentType.getString(ctx, "area");
        var data = org.lupz.doomsdayessentials.territory.ResourceGeneratorManager.get().get(areaName);
        if (data == null) {
            ctx.getSource().sendFailure(Component.literal("Gerador não encontrado."));
            return 0;
        }
        ctx.getSource().sendSuccess(() -> Component.literal(
                "Loot: " + data.lootId + ", PorHora: " + data.itemsPerHour + ", Cap: " + data.storageCap + ", Dono: " + data.ownerGuild + ", Armazenado: " + data.storedItems), false);
        return 1;
    }

    private static int generatorSet(CommandContext<CommandSourceStack> ctx) {
        String areaName = StringArgumentType.getString(ctx, "area");
        String field = StringArgumentType.getString(ctx, "field");
        String value = StringArgumentType.getString(ctx, "value");
        var mgr = org.lupz.doomsdayessentials.territory.ResourceGeneratorManager.get();
        var data = mgr.get(areaName);
        if (data == null) {
            ctx.getSource().sendFailure(Component.literal("Gerador não encontrado."));
            return 0;
        }
        try {
            switch (field) {
                case "loot" -> data.lootId = value;
                case "perhour", "itemsPerHour" -> data.itemsPerHour = Integer.parseInt(value);
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
        ctx.getSource().sendSuccess(() -> Component.literal("Geradores recarregados do disco."), true);
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
} 