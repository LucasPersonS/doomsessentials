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

	private enum Stage { NONE, ASK_NAME, ASK_AMOUNT }
	private static class State {
		Stage stage = Stage.NONE;
		long expiresAt;
		String pendingName;
	}

	private static final Map<UUID, State> STATES = new ConcurrentHashMap<>();
	private static final long TIMEOUT_TICKS = 20L * 30L;

	public static void start(ServerPlayer player) {
		State st = new State();
		st.stage = Stage.ASK_NAME;
		st.expiresAt = player.level().getGameTime() + TIMEOUT_TICKS;
		STATES.put(player.getUUID(), st);
		player.sendSystemMessage(Component.literal("§eEscreva o nickname da vítima"));
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
		if (now > st.expiresAt) { STATES.remove(sp.getUUID()); sp.sendSystemMessage(Component.literal("§cOperação cancelada por tempo excedido.")); return; }
		if (st.stage == Stage.ASK_NAME) {
			ServerPlayer target = sp.server.getPlayerList().getPlayerByName(msg);
			if (target == null) {
				sp.sendSystemMessage(Component.literal("§cJogador não encontrado. Tente novamente."));
				st.expiresAt = now + TIMEOUT_TICKS;
				return;
			}
			st.pendingName = target.getName().getString();
			st.stage = Stage.ASK_AMOUNT;
			st.expiresAt = now + TIMEOUT_TICKS;
			sp.sendSystemMessage(Component.literal("§eDigite a quantia de gears"));
			return;
		}
		if (st.stage == Stage.ASK_AMOUNT) {
			int gears;
			try { gears = Integer.parseInt(msg); } catch (Exception ex) { sp.sendSystemMessage(Component.literal("§cQuantidade inválida. Digite um número.")); return; }
			ServerPlayer target = sp.server.getPlayerList().getPlayerByName(st.pendingName);
			if (target == null) { sp.sendSystemMessage(Component.literal("§cJogador offline. Operação cancelada.")); STATES.remove(sp.getUUID()); return; }
			boolean ok = BountyManager.placeBounty(sp, target.getUUID(), gears);
			if (ok) sp.sendSystemMessage(Component.literal("§aRecompensa colocada com sucesso"));
			else sp.sendSystemMessage(Component.literal("§cGears insuficientes."));
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
				if (sp != null) sp.sendSystemMessage(Component.literal("§cOperação cancelada por tempo excedido."));
			}
			return expired;
		});
	}

	public static boolean isInConversation(ServerPlayer sp) { State st = STATES.get(sp.getUUID()); return st != null && st.stage != Stage.NONE; }
} 