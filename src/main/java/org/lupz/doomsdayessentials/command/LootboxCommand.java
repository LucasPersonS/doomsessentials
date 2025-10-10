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

		dispatcher.register(root);
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
} 