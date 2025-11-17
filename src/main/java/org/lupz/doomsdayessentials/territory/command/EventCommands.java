package org.lupz.doomsdayessentials.territory.command;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import org.lupz.doomsdayessentials.combat.AreaManager;
import org.lupz.doomsdayessentials.combat.ManagedArea;
import org.lupz.doomsdayessentials.territory.TerritoryEventManager;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;

/**
 * Shared implementations for territory event admin commands used by both /territory event and /event.
 */
public class EventCommands {

    public static int start(CommandContext<CommandSourceStack> ctx) {
        String areaName = StringArgumentType.getString(ctx, "area");
        int durationMinutes = IntegerArgumentType.getInteger(ctx, "durationMinutes");
        int minPlayers = IntegerArgumentType.getInteger(ctx, "minPlayers");

        ManagedArea area = AreaManager.get().getArea(areaName);
        if (area == null) {
            ctx.getSource().sendFailure(Component.literal("Área não encontrada."));
            return 0;
        }
        var mgr = TerritoryEventManager.get();
        if (mgr.isEventRunning(areaName)) {
            ctx.getSource().sendFailure(Component.literal("Já existe um evento ativo nesta área."));
            return 0;
        }
        if (mgr.getActiveAreas().size() >= 2) {
            ctx.getSource().sendFailure(Component.literal("Limite de 2 eventos simultâneos atingido."));
            return 0;
        }
        boolean ok = mgr.startEvent(area, durationMinutes * 60, minPlayers);
        if (ok) {
            ctx.getSource().sendSuccess(() -> Component.literal("Evento iniciado com sucesso."), true);
            return 1;
        } else {
            ctx.getSource().sendFailure(Component.literal("Falha ao iniciar evento."));
            return 0;
        }
    }

    public static int stop(CommandContext<CommandSourceStack> ctx) {
        String areaName = StringArgumentType.getString(ctx, "area");
        if (!TerritoryEventManager.get().isEventRunning(areaName)) {
            ctx.getSource().sendFailure(Component.literal("Nenhum evento em andamento para esta área."));
            return 0;
        }
        TerritoryEventManager.get().stopEvent(areaName);
        ctx.getSource().sendSuccess(() -> Component.literal("Evento em " + areaName + " interrompido."), true);
        return 1;
    }

    public static int stopAll(CommandContext<CommandSourceStack> ctx) {
        if (!TerritoryEventManager.get().isEventRunning()) {
            ctx.getSource().sendFailure(Component.literal("Nenhum evento em andamento."));
            return 0;
        }
        TerritoryEventManager.get().stopAll();
        ctx.getSource().sendSuccess(() -> Component.literal("Todos os eventos interrompidos."), true);
        return 1;
    }

    public static int status(CommandContext<CommandSourceStack> ctx) {
        boolean hasArea = ctx.getNodes().stream().anyMatch(n -> n.getNode().getName().equals("area"));
        if (hasArea) {
            String areaName = StringArgumentType.getString(ctx, "area");
            var mgr = TerritoryEventManager.get();
            if (!mgr.isEventRunning(areaName)) {
                ctx.getSource().sendSuccess(() -> Component.literal("Nenhum evento em " + areaName + "."), false);
                return 0;
            }
            ctx.getSource().sendSuccess(() -> Component.literal("Evento em " + areaName + ": " + mgr.getEventProgress(areaName) + "/" + mgr.getEventDuration(areaName) + "s, req " + mgr.getEventRequiredPlayers(areaName) + " jogadores."), false);
            return 1;
        } else {
            var mgr = TerritoryEventManager.get();
            if (!mgr.isEventRunning()) {
                ctx.getSource().sendSuccess(() -> Component.literal("Nenhum evento em andamento."), false);
                return 0;
            }
            ctx.getSource().sendSuccess(() -> Component.literal("Eventos ativos: " + String.join(", ", mgr.getActiveAreas())), false);
            return 1;
        }
    }

    public static int setPlayers(CommandContext<CommandSourceStack> ctx) {
        String areaName = StringArgumentType.getString(ctx, "area");
        int minPlayers = IntegerArgumentType.getInteger(ctx, "minPlayers");
        var mgr = TerritoryEventManager.get();
        if (!mgr.isEventRunning(areaName)) {
            ctx.getSource().sendFailure(Component.literal("Nenhum evento em andamento nesta área."));
            return 0;
        }
        boolean updated = mgr.updateRequiredPlayers(areaName, minPlayers);
        if (updated) {
            ctx.getSource().sendSuccess(() -> Component.literal("Mínimo de jogadores em " + areaName + " atualizado para " + minPlayers + "."), true);
            return 1;
        }
        ctx.getSource().sendFailure(Component.literal("Falha ao atualizar requisito de jogadores."));
        return 0;
    }

    public static java.util.concurrent.CompletableFuture<com.mojang.brigadier.suggestion.Suggestions> suggestAreaNames(CommandContext<CommandSourceStack> c, SuggestionsBuilder b) {
        AreaManager.get().getAreas().forEach(a -> b.suggest(a.getName()));
        return b.buildFuture();
    }
}
