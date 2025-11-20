package org.lupz.doomsdayessentials.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lupz.doomsdayessentials.EssentialsMod;
import org.lupz.doomsdayessentials.lootbox.LootboxManager;

@Mod.EventBusSubscriber(modid = EssentialsMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class LootboxCommand {

	private LootboxCommand() {}

	@SubscribeEvent
	public static void onRegister(RegisterCommandsEvent e) {
		register(e.getDispatcher());
	}

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		SuggestionProvider<CommandSourceStack> RARITY_SUGGEST = (ctx, builder) -> {
			for (String r : LootboxManager.RARITIES) builder.suggest(r);
			return builder.buildFuture();
		};

		SuggestionProvider<CommandSourceStack> RARITY_LIST_SUGGEST = (ctx, builder) -> {
			for (String r : LootboxManager.RARITIES) builder.suggest(r);
			builder.suggest("incomum,rara");
			builder.suggest("epica,lendaria");
			builder.suggest("incomum,rara,epica,lendaria");
			return builder.buildFuture();
		};

		LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("lootbox").requires(s -> s.hasPermission(2));

		// /lootbox add item <raridade> [chance]
		root.then(Commands.literal("add")
				.then(Commands.literal("item")
						.then(Commands.argument("raridade", StringArgumentType.word()).suggests(RARITY_SUGGEST)
								.executes(LootboxCommand::addHeldItem)
								.then(Commands.argument("chance", DoubleArgumentType.doubleArg(0.0))
										.executes(LootboxCommand::addHeldItemWithChance)))));

		// /lootbox open <raridade> (test GUI)
		root.then(Commands.literal("open")
				.then(Commands.argument("raridade", StringArgumentType.word()).suggests(RARITY_SUGGEST)
						.executes(LootboxCommand::openLootbox)));


		LiteralArgumentBuilder<CommandSourceStack> looted = Commands.literal("looted").requires(s -> s.hasPermission(2));

		SuggestionProvider<CommandSourceStack> ITEM_SUGGEST = (ctx, builder) -> {
			try {
				String list = com.mojang.brigadier.arguments.StringArgumentType.getString(ctx, "raridades");
				java.util.Set<String> rset = parseRarities(list);
				for (String r : rset) {
					java.util.List<String> names = org.lupz.doomsdayessentials.lootbox.LootboxManager.getItemNames(r);
					for (int i = 0; i < names.size(); i++) {
						String n = names.get(i);
						builder.suggest(r+":"+i+":"+n);
					}
				}
			} catch (Exception ignored) {}
			return builder.buildFuture();
		};

		looted.then(Commands.literal("removeitem")
			.then(Commands.argument("raridades", com.mojang.brigadier.arguments.StringArgumentType.greedyString()).suggests(RARITY_LIST_SUGGEST)
				.executes(ctx -> listItems(ctx))
				.then(Commands.argument("item", com.mojang.brigadier.arguments.StringArgumentType.greedyString()).suggests(ITEM_SUGGEST)
					.executes(ctx -> promptConfirm(ctx))
					.then(Commands.literal("confirm").executes(ctx -> doRemove(ctx))))));

		dispatcher.register(root);
		dispatcher.register(looted);
	}

	private static int addHeldItem(CommandContext<CommandSourceStack> ctx) {
		return add(ctx, 1.0);
	}

	private static int addHeldItemWithChance(CommandContext<CommandSourceStack> ctx) {
		double chance = DoubleArgumentType.getDouble(ctx, "chance");
		return add(ctx, chance);
	}

	private static int add(CommandContext<CommandSourceStack> ctx, double chance) {
		String rarity = StringArgumentType.getString(ctx, "raridade").toLowerCase();
		if (!(ctx.getSource().getEntity() instanceof ServerPlayer sp)) {
			ctx.getSource().sendFailure(Component.literal("Apenas jogadores.").withStyle(Style.EMPTY.withBold(true)));
			return 0;
		}
		ItemStack held = sp.getItemInHand(InteractionHand.MAIN_HAND);
		if (held.isEmpty()) {
			ctx.getSource().sendFailure(Component.literal("Coloque um item na mão principal.").withStyle(Style.EMPTY.withBold(true)));
			return 0;
		}
		boolean ok = LootboxManager.addItem(rarity, held, chance);
		if (!ok) {
			ctx.getSource().sendFailure(Component.literal("Raridade inválida. Use: incomum, rara, epica, lendaria").withStyle(Style.EMPTY.withBold(true)));
			return 0;
		}
		Component name = held.getHoverName();
		ctx.getSource().sendSuccess(() -> Component.literal("§6§l» §e§lLOOTBOX §6§l« §7Item adicionado em §f" + rarity + "§7 (chance §e" + chance + "§7): ").append(name.copy()), true);
		return 1;
	}

	private static int openLootbox(CommandContext<CommandSourceStack> ctx) {
		String rarity = StringArgumentType.getString(ctx, "raridade").toLowerCase();
		if (!(ctx.getSource().getEntity() instanceof ServerPlayer sp)) {
			ctx.getSource().sendFailure(Component.literal("Apenas jogadores.").withStyle(Style.EMPTY.withBold(true)));
			return 0;
		}
		if (!LootboxManager.RARITIES.contains(rarity)) {
			ctx.getSource().sendFailure(Component.literal("Raridade inválida. Use: incomum, rara, epica, lendaria").withStyle(Style.EMPTY.withBold(true)));
			return 0;
		}
		net.minecraftforge.network.NetworkHooks.openScreen(sp, new org.lupz.doomsdayessentials.lootbox.LootboxMenuProvider(rarity, org.lupz.doomsdayessentials.lootbox.LootboxManager.getAllAsStacks(rarity)), buf -> {
			buf.writeUtf(rarity);
			java.util.List<net.minecraft.world.item.ItemStack> pool = org.lupz.doomsdayessentials.lootbox.LootboxManager.getAllAsStacks(rarity);
			buf.writeVarInt(pool.size());
			for (net.minecraft.world.item.ItemStack s : pool) buf.writeItem(s);
		});
		return 1;
	}

	private static java.util.Set<String> parseRarities(String csv) {
		java.util.Set<String> out = new java.util.LinkedHashSet<>();
		if (csv == null) return out;
		for (String p : csv.toLowerCase().split(",")) {
			String t = p.trim();
			if (t.isEmpty()) continue;
			String canon = switch (t) {
				case "comum", "common", "normal" -> LootboxManager.R_INCOMUM;
				case "uncommon", "incomum" -> LootboxManager.R_INCOMUM;
				case "rara", "rare" -> LootboxManager.R_RARA;
				case "epica", "epic" -> LootboxManager.R_EPICA;
				case "lendaria", "legendary" -> LootboxManager.R_LENDARIA;
				default -> t;
			};
			if (LootboxManager.RARITIES.contains(canon)) out.add(canon);
		}
		return out;
	}

	private static int listItems(CommandContext<CommandSourceStack> ctx) {
		String list = com.mojang.brigadier.arguments.StringArgumentType.getString(ctx, "raridades");
		java.util.Set<String> rset = parseRarities(list);
		if (rset.isEmpty()) {
			ctx.getSource().sendFailure(Component.literal("Raridades inválidas. Use: incomum, rara, epica, lendaria"));
			return 0;
		}
		int total = 0;
		for (String r : rset) {
			java.util.List<String> names = org.lupz.doomsdayessentials.lootbox.LootboxManager.getItemNames(r);
			if (names.isEmpty()) {
				ctx.getSource().sendSuccess(() -> Component.literal("§7["+r+"] vazio"), false);
				continue;
			}
			ctx.getSource().sendSuccess(() -> Component.literal("§6§l» §e§lLOOTBOX §6§l« §7Itens em §f"+r+":"), false);
			for (int i = 0; i < names.size(); i++) {
				String n = names.get(i);
				net.minecraft.network.chat.MutableComponent line = Component.literal("- ["+r+"] #"+i+" ").append(Component.literal(n).withStyle(net.minecraft.ChatFormatting.WHITE));
				String cmd = "/looted removeitem " + r + " " + r + ":" + i + ":" + n + " confirm";
				line = line.append(Component.literal(" [Remover]").withStyle(style -> style.withColor(net.minecraft.ChatFormatting.RED).withUnderlined(true).withClickEvent(new net.minecraft.network.chat.ClickEvent(net.minecraft.network.chat.ClickEvent.Action.RUN_COMMAND, cmd))));
				final net.minecraft.network.chat.MutableComponent fline = line;
				ctx.getSource().sendSuccess(() -> fline, false);
				total++;
			}
		}
		return total;
	}

	private static int promptConfirm(CommandContext<CommandSourceStack> ctx) {
		String list = com.mojang.brigadier.arguments.StringArgumentType.getString(ctx, "raridades");
		String itemSel = com.mojang.brigadier.arguments.StringArgumentType.getString(ctx, "item");
		java.util.Set<String> rset = parseRarities(list);
		if (rset.isEmpty()) {
			ctx.getSource().sendFailure(Component.literal("Raridades inválidas."));
			return 0;
		}
		String[] parts = itemSel.split(":", 3);
		if (parts.length < 2) {
			ctx.getSource().sendFailure(Component.literal("Seleção inválida. Use sugestão epica:3:Nome"));
			return 0;
		}
		String rarity = parts[0];
		int idx;
		try { idx = Integer.parseInt(parts[1]); } catch (Exception e) { ctx.getSource().sendFailure(Component.literal("Índice inválido.")); return 0; }
		if (!rset.contains(rarity)) { ctx.getSource().sendFailure(Component.literal("Raridade fora da seleção.")); return 0; }
		java.util.List<String> names = org.lupz.doomsdayessentials.lootbox.LootboxManager.getItemNames(rarity);
		if (idx < 0 || idx >= names.size()) { ctx.getSource().sendFailure(Component.literal("Índice fora do intervalo.")); return 0; }
		String name = names.get(idx);
		net.minecraft.network.chat.MutableComponent msg = Component.literal("Confirmar remoção de ").append(Component.literal(name).withStyle(net.minecraft.ChatFormatting.YELLOW)).append(Component.literal(" de ["+rarity+"]?"));
		String cmd = "/looted removeitem " + list + " " + rarity + ":" + idx + ":" + name + " confirm";
		msg = msg.append(Component.literal(" [Confirmar]").withStyle(style -> style.withColor(net.minecraft.ChatFormatting.GREEN).withUnderlined(true).withClickEvent(new net.minecraft.network.chat.ClickEvent(net.minecraft.network.chat.ClickEvent.Action.RUN_COMMAND, cmd))));
		final net.minecraft.network.chat.MutableComponent fmsg = msg;
		ctx.getSource().sendSuccess(() -> fmsg, false);
		return 1;
	}

	private static int doRemove(CommandContext<CommandSourceStack> ctx) {
		String list = com.mojang.brigadier.arguments.StringArgumentType.getString(ctx, "raridades");
		String itemSel = com.mojang.brigadier.arguments.StringArgumentType.getString(ctx, "item");
		String[] parts = itemSel.split(":", 3);
		if (parts.length < 2) { ctx.getSource().sendFailure(Component.literal("Seleção inválida.")); return 0; }
		String rarity = parts[0];
		int idx; try { idx = Integer.parseInt(parts[1]); } catch (Exception e) { ctx.getSource().sendFailure(Component.literal("Índice inválido.")); return 0; }
		if (!LootboxManager.RARITIES.contains(rarity)) { ctx.getSource().sendFailure(Component.literal("Raridade inválida.")); return 0; }
		boolean ok = LootboxManager.removeItem(rarity, idx);
		if (ok) {
			ctx.getSource().sendSuccess(() -> Component.literal("§aItem removido de "+rarity+" (#"+idx+")"), true);
			return 1;
		}
		ctx.getSource().sendFailure(Component.literal("Falha ao remover item."));
		return 0;
	}
} 
