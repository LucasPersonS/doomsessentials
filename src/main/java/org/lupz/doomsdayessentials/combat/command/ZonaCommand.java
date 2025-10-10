package org.lupz.doomsdayessentials.combat.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lupz.doomsdayessentials.EssentialsMod;
import org.lupz.doomsdayessentials.combat.AreaManager;
import org.lupz.doomsdayessentials.combat.ManagedArea;

import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Mod.EventBusSubscriber(modid = EssentialsMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ZonaCommand {

	private ZonaCommand() {}

	@SubscribeEvent
	public static void onRegisterCommands(RegisterCommandsEvent e) {
		register(e.getDispatcher());
	}

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		var root = Commands.literal("zona");

		root.then(
			Commands.literal("horario")
				.then(Commands.argument("nome", StringArgumentType.word())
					.suggests(ZonaCommand::suggestAreaNames)
					.executes(ZonaCommand::showHorarioByName))
				.executes(ZonaCommand::showHorarioAtPlayer)
		);

		root.then(
			Commands.literal("verhorario")
				.requires(src -> src.hasPermission(2))
				.then(Commands.argument("nome", StringArgumentType.word())
					.suggests(ZonaCommand::suggestAreaNames)
					.executes(ZonaCommand::verHorario))
		);

		root.then(
			Commands.literal("sethorario")
				.requires(src -> src.hasPermission(2))
				.then(Commands.argument("nome", StringArgumentType.word())
					.suggests(ZonaCommand::suggestAreaNames)
					.then(Commands.argument("janelas", StringArgumentType.greedyString())
						.suggests((c,b) -> { b.suggest("09:00-12:00 18:00-20:00"); b.suggest("none"); return b.buildFuture(); })
						.executes(ZonaCommand::setHorario)))
		);

		root.then(
			Commands.literal("limparhorario")
				.requires(src -> src.hasPermission(2))
				.then(Commands.argument("nome", StringArgumentType.word())
					.suggests(ZonaCommand::suggestAreaNames)
					.executes(ctx -> setHorarioToNone(ctx, StringArgumentType.getString(ctx, "nome"))))
		);

		dispatcher.register(root);
	}

	private static CompletableFuture<com.mojang.brigadier.suggestion.Suggestions> suggestAreaNames(CommandContext<CommandSourceStack> c, SuggestionsBuilder b) {
		AreaManager.get().getAreas().forEach(a -> b.suggest(a.getName()));
		return b.buildFuture();
	}

	private static int showHorarioAtPlayer(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
		ServerPlayer player = ctx.getSource().getPlayerOrException();
		ServerLevel level = ctx.getSource().getLevel();
		var area = AreaManager.get().getAreaAt(level, player.blockPosition());
		if (area == null) {
			ctx.getSource().sendSuccess(() -> Component.literal("Você não está em nenhuma zona.").withStyle(ChatFormatting.YELLOW), false);
			return 1;
		}
		return sendHorario(ctx, area);
	}

	private static int showHorarioByName(CommandContext<CommandSourceStack> ctx) {
		String nome = StringArgumentType.getString(ctx, "nome");
		var area = AreaManager.get().getArea(nome);
		if (area == null) {
			ctx.getSource().sendFailure(Component.literal("Zona '" + nome + "' não encontrada.").withStyle(ChatFormatting.RED));
			return 0;
		}
		return sendHorario(ctx, area);
	}

	private static int verHorario(CommandContext<CommandSourceStack> ctx) {
		String nome = StringArgumentType.getString(ctx, "nome");
		var area = AreaManager.get().getArea(nome);
		if (area == null) {
			ctx.getSource().sendFailure(Component.literal("Zona '" + nome + "' não encontrada.").withStyle(ChatFormatting.RED));
			return 0;
		}
		return sendHorario(ctx, area);
	}

	private static int sendHorario(CommandContext<CommandSourceStack> ctx, ManagedArea area) {
		List<ManagedArea.TimeWindow> wins = area.getOpenWindows();
		if (wins == null || wins.isEmpty()) {
			ctx.getSource().sendSuccess(() -> Component.literal("§a§l[ZONA] §e'" + area.getName() + "' está §aABERTA 24h§e."), false);
			return 1;
		}

		LocalTime now = LocalTime.now(ZoneId.of("America/Sao_Paulo"));
		boolean aberta = area.isCurrentlyOpen();
		StringBuilder sb = new StringBuilder();
		sb.append("§a§l[ZONA] §eHorários de '" + area.getName() + "': §b");
		for (int i = 0; i < wins.size(); i++) {
			ManagedArea.TimeWindow tw = wins.get(i);
			sb.append(String.format("%02d:%02d-%02d:%02d", tw.start().getHour(), tw.start().getMinute(), tw.end().getHour(), tw.end().getMinute()));
			if (i < wins.size() - 1) sb.append(", ");
		}
		ctx.getSource().sendSuccess(() -> Component.literal(sb.toString()), false);

		if (!aberta) {
			LocalTime next = null;
			for (ManagedArea.TimeWindow tw : wins) {
				LocalTime start = tw.start();
				if (start.isAfter(now)) {
					if (next == null || start.isBefore(next)) next = start;
				}
			}
			if (next == null) {
				next = wins.stream().map(ManagedArea.TimeWindow::start).min(LocalTime::compareTo).orElse(null);
			}
			String timeStr = next != null ? String.format("%02d:%02d", next.getHour(), next.getMinute()) : "--:--";
			ctx.getSource().sendSuccess(() -> Component.literal("§c§l[ZONA FECHADA] §eAbrirá às §a" + timeStr + "§e."), false);
		} else {
			ctx.getSource().sendSuccess(() -> Component.literal("§a§l[ZONA] §eStatus: §aABERTA agora."), false);
		}
		return 1;
	}

	private static int setHorario(CommandContext<CommandSourceStack> ctx) {
		String nome = StringArgumentType.getString(ctx, "nome");
		String valueStr = StringArgumentType.getString(ctx, "janelas");
		var area = AreaManager.get().getArea(nome);
		if (area == null) {
			ctx.getSource().sendFailure(Component.literal("Zona '" + nome + "' não encontrada.").withStyle(ChatFormatting.RED));
			return 0;
		}

		try {
			List<ManagedArea.TimeWindow> wins = new ArrayList<>();
			if (!valueStr.equalsIgnoreCase("none")) {
				String[] tokens = valueStr.trim().split("\\s+");
				for (String token : tokens) {
					String[] parts = token.split("-");
					if (parts.length != 2) throw new java.time.format.DateTimeParseException("Bad window", token, 0);
					LocalTime start = LocalTime.parse(parts[0]);
					LocalTime end = LocalTime.parse(parts[1]);
					wins.add(new ManagedArea.TimeWindow(start, end));
				}
			}
			area.setOpenWindows(wins);
			AreaManager.get().saveAreas();
			AreaManager.get().broadcastAreaUpdate();
			ctx.getSource().sendSuccess(() -> Component.literal("Horários de '" + nome + "' atualizados (" + wins.size() + ")").withStyle(ChatFormatting.GREEN), true);
			return 1;
		} catch (java.time.format.DateTimeParseException ex) {
			ctx.getSource().sendFailure(Component.literal("Formato inválido. Use HH:mm-HH:mm (espaços para múltiplas janelas) ou 'none' para limpar.").withStyle(ChatFormatting.RED));
			return 0;
		}
	}

	private static int setHorarioToNone(CommandContext<CommandSourceStack> ctx, String nome) {
		var area = AreaManager.get().getArea(nome);
		if (area == null) {
			ctx.getSource().sendFailure(Component.literal("Zona '" + nome + "' não encontrada.").withStyle(ChatFormatting.RED));
			return 0;
		}
		area.setOpenWindows(java.util.Collections.emptyList());
		AreaManager.get().saveAreas();
		AreaManager.get().broadcastAreaUpdate();
		ctx.getSource().sendSuccess(() -> Component.literal("Horários de '" + nome + "' limpos (aberta 24h)."), true);
		return 1;
	}
} 