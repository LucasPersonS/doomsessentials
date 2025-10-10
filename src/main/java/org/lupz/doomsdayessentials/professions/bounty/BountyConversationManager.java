package org.lupz.doomsdayessentials.professions.bounty;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lupz.doomsdayessentials.EssentialsMod;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Mod.EventBusSubscriber(modid = EssentialsMod.MOD_ID)
public final class BountyConversationManager {
	private BountyConversationManager() {}

	private enum Stage { NONE, ASK_NAME, ASK_REWARD_ITEM, ASK_AMOUNT }
	private static class State {
		Stage stage = Stage.NONE;
		long expiresAt;
		String pendingName;
		net.minecraft.world.item.Item pendingItem;
	}

	private static final Map<UUID, State> STATES = new ConcurrentHashMap<>();
	private static final long TIMEOUT_TICKS = 20L * 30L;

	public static void start(ServerPlayer player) {
		State st = new State();
		st.stage = Stage.ASK_NAME;
		st.expiresAt = player.level().getGameTime() + TIMEOUT_TICKS;
		STATES.put(player.getUUID(), st);
		player.sendSystemMessage(Component.literal("§6§l» §e§lMURAL §6§l« §7Digite o §f§lNICK §7da vítima (TAB completa)."));
		pushPlayerTabCompletions(player);
	}

	@SubscribeEvent
	public static void onChat(ServerChatEvent event) {
		ServerPlayer sp = event.getPlayer();
		State st = STATES.get(sp.getUUID());
		if (st == null || st.stage == Stage.NONE) return;
		// intercept and not broadcast
		event.setCanceled(true);
		String msg = event.getRawText().trim();
		long now = sp.level().getGameTime();
		if (now > st.expiresAt) { STATES.remove(sp.getUUID()); clearTabCompletions(sp); sp.sendSystemMessage(Component.literal("§cOperação cancelada por tempo excedido.")); return; }
		if (st.stage == Stage.ASK_NAME) {
			ServerPlayer target = sp.server.getPlayerList().getPlayerByName(msg);
			if (target == null) {
				sp.sendSystemMessage(Component.literal("§cJogador não encontrado. §7Digite novamente (TAB ajuda)."));
				st.expiresAt = now + TIMEOUT_TICKS;
				pushPlayerTabCompletions(sp);
				return;
			}
			st.pendingName = target.getName().getString();
			st.stage = Stage.ASK_REWARD_ITEM;
			st.expiresAt = now + TIMEOUT_TICKS;
			sp.sendSystemMessage(Component.literal("§6§l» §e§lRECOMPENSA §6§l« §7Digite o item (§fSucata§7/§fPlaca de Metal§7/§fFragmentos de Metal§7/§fLâmina de Metal§7/§fEngrenagens§7)."));
			pushItemTabCompletions(sp);
			return;
		}
		if (st.stage == Stage.ASK_REWARD_ITEM) {
			net.minecraft.world.item.Item chosen = parseRewardItem(msg);
			if (chosen == null) {
				sp.sendSystemMessage(Component.literal("§cItem não encontrado. §7Tente novamente (TAB ajuda)."));
				st.expiresAt = now + TIMEOUT_TICKS;
				pushItemTabCompletions(sp);
				return;
			}
			st.pendingItem = chosen;
			st.stage = Stage.ASK_AMOUNT;
			st.expiresAt = now + TIMEOUT_TICKS;
			sp.sendSystemMessage(Component.literal("§6§l» §e§lQUANTIA §6§l« §7Digite a quantidade."));
			clearTabCompletions(sp);
			return;
		}
		if (st.stage == Stage.ASK_AMOUNT) {
			int amount;
			try { amount = Integer.parseInt(msg); } catch (Exception ex) { sp.sendSystemMessage(Component.literal("§cQuantidade inválida. Digite um número.")); return; }
			ServerPlayer target = sp.server.getPlayerList().getPlayerByName(st.pendingName);
			if (target == null) { sp.sendSystemMessage(Component.literal("§cJogador offline. Operação cancelada.")); STATES.remove(sp.getUUID()); return; }
			boolean ok = BountyManager.placeBounty(sp, target.getUUID(), st.pendingItem, amount);
			if (ok) sp.sendSystemMessage(Component.literal("§aRecompensa colocada com sucesso"));
			else sp.sendSystemMessage(Component.literal("§cItens insuficientes."));
			STATES.remove(sp.getUUID());
		}
	}

	@SubscribeEvent
	public static void onServerTick(TickEvent.ServerTickEvent e) {
		if (e.phase != TickEvent.Phase.END) return;
		long now = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer().overworld().getGameTime();
		STATES.entrySet().removeIf(en -> {
			boolean expired = now > en.getValue().expiresAt;
			if (expired) {
				ServerPlayer sp = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayer(en.getKey());
				if (sp != null) { clearTabCompletions(sp); sp.sendSystemMessage(Component.literal("§cOperação cancelada por tempo excedido.")); }
			}
			return expired;
		});
	}

	public static boolean isInConversation(ServerPlayer sp) { State st = STATES.get(sp.getUUID()); return st != null && st.stage != Stage.NONE; }

	private static void pushPlayerTabCompletions(ServerPlayer player) {
		try {
			java.util.List<String> names = new java.util.ArrayList<>();
			for (ServerPlayer p : player.getServer().getPlayerList().getPlayers()) names.add(p.getGameProfile().getName());
			player.connection.send(new net.minecraft.network.protocol.game.ClientboundCustomChatCompletionsPacket(net.minecraft.network.protocol.game.ClientboundCustomChatCompletionsPacket.Action.ADD, names));
		} catch (Throwable ignored) {}
	}

	private static void pushItemTabCompletions(ServerPlayer player) {
		try {
			java.util.List<String> names = new java.util.ArrayList<>();
			names.add("Sucata");
			names.add("Placa de Metal");
			names.add("Fragmentos de Metal");
			names.add("Lâmina de Metal");
			names.add("Engrenagens");
			player.connection.send(new net.minecraft.network.protocol.game.ClientboundCustomChatCompletionsPacket(net.minecraft.network.protocol.game.ClientboundCustomChatCompletionsPacket.Action.SET, names));
		} catch (Throwable ignored) {}
	}

	private static void clearTabCompletions(ServerPlayer player) {
		try {
			player.connection.send(new net.minecraft.network.protocol.game.ClientboundCustomChatCompletionsPacket(net.minecraft.network.protocol.game.ClientboundCustomChatCompletionsPacket.Action.SET, java.util.List.of()));
		} catch (Throwable ignored) {}
	}

	private static net.minecraft.world.item.Item parseRewardItem(String name) {
		String n = normalize(name);
		// Synonyms and direct matches
		if (n.startsWith("sucata") || n.contains("scrap")) return org.lupz.doomsdayessentials.item.ModItems.SCRAPMETAL.get();
		if (n.startsWith("placa de metal") || n.startsWith("placa") || n.contains("sheetmetal") || n.contains("metal plate")) return org.lupz.doomsdayessentials.item.ModItems.SHEETMETAL.get();
		if (n.startsWith("fragmento") || n.startsWith("fragmentos") || n.contains("metal fragment")) return org.lupz.doomsdayessentials.item.ModItems.METAL_FRAGMENTS.get();
		if (n.startsWith("lamina") || n.startsWith("lamina de metal") || n.contains("metalblade") || n.equals("blade")) return org.lupz.doomsdayessentials.item.ModItems.METALBLADE.get();
		if (n.startsWith("engrenagem") || n.startsWith("engrenagens") || n.contains("gear") || n.equals("doomsday:gears") || n.equals("doomsdayessentials:gears")) return org.lupz.doomsdayessentials.item.ModItems.GEARS.get();
		return null;
	}

	private static String normalize(String s) {
		String lower = s.trim().toLowerCase(java.util.Locale.ROOT);
		try {
			java.text.Normalizer.Form form = java.text.Normalizer.Form.NFD;
			String norm = java.text.Normalizer.normalize(lower, form);
			return norm.replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
		} catch (Throwable t) {
			return lower;
		}
	}
} 