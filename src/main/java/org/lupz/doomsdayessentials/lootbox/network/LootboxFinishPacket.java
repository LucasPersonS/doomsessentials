package org.lupz.doomsdayessentials.lootbox.network;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;
import org.lupz.doomsdayessentials.lootbox.LootboxManager;
import org.lupz.doomsdayessentials.sound.ModSounds;

import java.util.List;
import java.util.function.Supplier;

public class LootboxFinishPacket {
	private final String rarity;
	private final int windowId;

	public LootboxFinishPacket(String rarity, int windowId) {
		this.rarity = rarity;
		this.windowId = windowId;
	}

	public static void encode(LootboxFinishPacket pkt, FriendlyByteBuf buf) {
		buf.writeUtf(pkt.rarity);
		buf.writeVarInt(pkt.windowId);
	}

	public static LootboxFinishPacket decode(FriendlyByteBuf buf) {
		String r = buf.readUtf();
		int w = buf.readVarInt();
		return new LootboxFinishPacket(r, w);
	}

	public static void handle(LootboxFinishPacket pkt, Supplier<NetworkEvent.Context> ctx) {
		ctx.get().enqueueWork(() -> {
			ServerPlayer sp = ctx.get().getSender();
			if (sp == null) return;
			if (sp.containerMenu == null || sp.containerMenu.containerId != pkt.windowId) {
				// proceed anyway – safety net to avoid desync
			}
			List<LootboxManager.LootEntry> entries = LootboxManager.getEntries(pkt.rarity);
			LootboxManager.LootEntry picked = pickWeighted(entries, sp);
			ItemStack reward = picked != null ? picked.toStack() : ItemStack.EMPTY;
			double chancePercent = picked != null ? picked.chance() : 0.0;
			if (!reward.isEmpty()) {
				if (!sp.getInventory().add(reward.copy())) {
					sp.drop(reward.copy(), false);
				}
			}
			sp.level().playSound(null, sp.getX(), sp.getY(), sp.getZ(), ModSounds.LOOTBOX_COMPLETE.get(), SoundSource.PLAYERS, 1.0f, 1.0f);
			if (sp.level() instanceof ServerLevel sl) spawnParticles(sl, sp);
			if (!reward.isEmpty()) announce(sp, reward, pkt.rarity, chancePercent);
			sp.closeContainer();
		});
		ctx.get().setPacketHandled(true);
	}

	public static void finalizeServer(ServerPlayer sp, String rarity) {
		List<LootboxManager.LootEntry> entries = LootboxManager.getEntries(rarity);
		LootboxManager.LootEntry picked = pickWeighted(entries, sp);
		ItemStack reward = picked != null ? picked.toStack() : ItemStack.EMPTY;
		double chancePercent = picked != null ? picked.chance() : 0.0;
		if (!reward.isEmpty()) {
			if (!sp.getInventory().add(reward.copy())) {
				sp.drop(reward.copy(), false);
			}
		}
		sp.level().playSound(null, sp.getX(), sp.getY(), sp.getZ(), ModSounds.LOOTBOX_COMPLETE.get(), SoundSource.PLAYERS, 1.0f, 1.0f);
		if (sp.level() instanceof ServerLevel sl) spawnParticles(sl, sp);
		if (!reward.isEmpty()) announce(sp, reward, rarity, chancePercent);
	}

	private static LootboxManager.LootEntry pickWeighted(List<LootboxManager.LootEntry> entries, ServerPlayer sp) {
		double total = 0;
		for (LootboxManager.LootEntry e : entries) total += Math.max(0.0, e.chance());
		if (total <= 0) return null;
		double r = sp.getRandom().nextDouble() * total;
		double acc = 0;
		for (LootboxManager.LootEntry e : entries) {
			acc += Math.max(0.0, e.chance());
			if (r <= acc) return e;
		}
		return entries.get(entries.size() - 1);
	}

	private static void announce(ServerPlayer sp, ItemStack reward, String rarity, double chancePercent) {
		String playerName = sp.getName().getString();
		String itemName = reward.getHoverName().getString();
		String boxName = rarity.toUpperCase();
		String pctStr = formatPercent(chancePercent);
		net.minecraft.ChatFormatting color = colorForPercent(chancePercent);
		net.minecraft.network.chat.MutableComponent msg = Component.literal("§6§l» §e§lLOOTBOX §6§l« §7O jogador §f" + playerName + " §7conseguiu o item ")
				.append(Component.literal(itemName).withStyle(net.minecraft.ChatFormatting.GOLD))
				.append(Component.literal(" §7na Caixa §e" + boxName + " "))
				.append(Component.literal("[" + pctStr + "]").withStyle(color));
		msg.withStyle(s -> s.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_ITEM, new HoverEvent.ItemStackInfo(reward))));
		var server = sp.level().getServer();
		if (server != null) server.getPlayerList().broadcastSystemMessage(msg, false);
	}

	private static String formatPercent(double p) {
		if (p < 0.1) return String.format(java.util.Locale.ROOT, "%.3f%%", p);
		if (p < 1.0) return String.format(java.util.Locale.ROOT, "%.2f%%", p);
		if (p < 10.0) return String.format(java.util.Locale.ROOT, "%.1f%%", p);
		return String.format(java.util.Locale.ROOT, "%.0f%%", p);
	}

	private static net.minecraft.ChatFormatting colorForPercent(double p) {
		if (p < 0.1) return net.minecraft.ChatFormatting.GOLD; // ultra raro
		if (p <= 1.0) return net.minecraft.ChatFormatting.YELLOW; // raro
		if (p <= 5.0) return net.minecraft.ChatFormatting.GREEN; // incomum
		return net.minecraft.ChatFormatting.AQUA; // comum
	}

	private static void spawnParticles(ServerLevel level, ServerPlayer sp) {
		double x = sp.getX(); double y = sp.getY() + 1.0; double z = sp.getZ();
		for (int i = 0; i < 20; i++) {
			double angle = (i / 20.0) * 2 * Math.PI;
			double ox = Math.cos(angle) * 2.0;
			double oz = Math.sin(angle) * 2.0;
			level.sendParticles(ParticleTypes.HAPPY_VILLAGER, x + ox, y, z + oz, 1, 0.1, 0.1, 0.1, 0.05);
		}
		for (int i = 0; i < 15; i++) {
			double ox = (sp.getRandom().nextDouble() - 0.5) * 4.0;
			double oy = sp.getRandom().nextDouble() * 2.0;
			double oz = (sp.getRandom().nextDouble() - 0.5) * 4.0;
			level.sendParticles(ParticleTypes.FIREWORK, x + ox, y + oy, z + oz, 1, 0, 0.1, 0, 0.1);
		}
		for (int i = 0; i < 30; i++) {
			double ox = (sp.getRandom().nextDouble() - 0.5) * 3.0;
			double oy = sp.getRandom().nextDouble() * 3.0;
			double oz = (sp.getRandom().nextDouble() - 0.5) * 3.0;
			level.sendParticles(ParticleTypes.END_ROD, x + ox, y + oy, z + oz, 1, 0, 0, 0, 0.02);
		}
	}
} 