package org.lupz.doomsdayessentials.killlog;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import org.lupz.doomsdayessentials.EssentialsMod;
import org.lupz.doomsdayessentials.combat.AreaManager;
import org.lupz.doomsdayessentials.combat.ManagedArea;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Mod.EventBusSubscriber(modid = EssentialsMod.MOD_ID)
public final class AdminKillLogger {

	private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();
	private static final Path LOG_DIR = Path.of("config", "doomsdayessentials", "killlogs");

	// Track last hits per player (ring buffer of 5)
	private static final Map<UUID, ArrayDeque<HitInfo>> LAST_HITS = new ConcurrentHashMap<>();
	private record HitInfo(String name, String type, String source, long tsMs) {}

	@SubscribeEvent(priority = EventPriority.LOW)
	public static void onPlayerHurt(LivingHurtEvent event) {
		if (!(event.getEntity() instanceof ServerPlayer player)) return;
		if (player.level().isClientSide()) return;
		DamageSource src = event.getSource();
		Entity attacker = src.getEntity();
		String name = attacker instanceof Player p ? p.getGameProfile().getName() : (attacker instanceof LivingEntity le ? le.getName().getString() : "");
		String type = attacker != null ? attacker.getType().toShortString() : "env";
		String source = src.getMsgId();
		ArrayDeque<HitInfo> q = LAST_HITS.computeIfAbsent(player.getUUID(), k -> new ArrayDeque<>(5));
		if (q.size() >= 5) q.removeFirst();
		q.addLast(new HitInfo(name, type, source, System.currentTimeMillis()));
	}

	@SubscribeEvent(priority = EventPriority.LOWEST)
	public static void onPlayerDeath(LivingDeathEvent event) {
		if (event.isCanceled()) {
			return;
		}
		if (!(event.getEntity() instanceof ServerPlayer player)) {
			return;
		}
		if (player.level().isClientSide()) {
			return;
		}

		try {
			Files.createDirectories(LOG_DIR);
		} catch (IOException e) {
			EssentialsMod.LOGGER.error("Failed to create killlogs directory", e);
			return;
		}

		DamageSource source = event.getSource();
		Entity killer = source.getEntity();
		Entity direct = source.getDirectEntity();

		String causeId = source.getMsgId();
		String killerType = killer != null ? killer.getType().toShortString() : "none";
		String killerName = killer instanceof Player kp ? kp.getDisplayName().getString() : (killer != null ? killer.getName().getString() : "-");
		UUID killerUUID = killer instanceof Player kp2 ? kp2.getUUID() : null;

		Map<String, Object> weaponInfo = new HashMap<>();
		weaponInfo.put("id", "minecraft:air");
		weaponInfo.put("display", "Fists");
		if (killer instanceof Player kp3) {
			ItemStack w = kp3.getMainHandItem();
			if (!w.isEmpty()) {
				String wid = "minecraft:air";
				ResourceLocation key = ForgeRegistries.ITEMS.getKey(w.getItem());
				if (key != null) wid = key.toString();
				String wname = w.getDisplayName().getString();
				CompoundTag tag = w.getTag();
				if (tag != null && tag.contains("GunId", net.minecraft.nbt.Tag.TAG_STRING)) {
					String gunIdRaw = tag.getString("GunId");
					if (!gunIdRaw.isEmpty()) {
						String normalizedId = gunIdRaw.contains(":") ? gunIdRaw : ("tacz:" + gunIdRaw);
						wid = normalizedId;
						wname = formatGunId(gunIdRaw.contains(":") ? gunIdRaw.split(":",2)[1] : gunIdRaw);
					}
				}
				weaponInfo.put("id", wid);
				weaponInfo.put("display", wname);
			}
		}

		ServerLevel level = player.serverLevel();
		var area = AreaManager.get().getAreaAt(level, player.blockPosition());

		Map<String, Object> record = new HashMap<>();
		ZonedDateTime now = ZonedDateTime.ofInstant(Instant.now(), ZoneId.of("America/Sao_Paulo"));
		record.put("timestamp", now.toString());
		record.put("playerName", player.getGameProfile().getName());
		record.put("playerUUID", player.getUUID().toString());
		record.put("dimension", level.dimension().location().toString());
		record.put("x", Math.floor(player.getX()*100.0)/100.0);
		record.put("y", Math.floor(player.getY()*100.0)/100.0);
		record.put("z", Math.floor(player.getZ()*100.0)/100.0);
		record.put("areaName", area != null ? area.getName() : "none");
		record.put("areaType", area != null ? area.getType().name() : "none");
		record.put("causeId", causeId);
		record.put("killerType", killerType);
		record.put("killerName", killerName);
		record.put("killerUUID", killerUUID != null ? killerUUID.toString() : "");
		record.put("weapon", weaponInfo);
		record.put("inventory", snapshotInventory(player));

		// last hits snapshot
		ArrayDeque<HitInfo> q = LAST_HITS.getOrDefault(player.getUUID(), new ArrayDeque<>());
		List<Map<String,Object>> lastHits = new ArrayList<>();
		for (HitInfo h : q) {
			Map<String,Object> m = new HashMap<>();
			m.put("name", h.name());
			m.put("type", h.type());
			m.put("source", h.source());
			m.put("time", h.tsMs());
			lastHits.add(m);
		}
		record.put("last_hits", lastHits);
		q.clear();

		Path file = LOG_DIR.resolve(player.getUUID().toString() + ".jsonl");
		String json = GSON.toJson(record) + "\n";
		try {
			Files.writeString(file, json, StandardCharsets.UTF_8, java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.APPEND);
		} catch (IOException e) {
			EssentialsMod.LOGGER.error("Failed to append killlog for {}", player.getGameProfile().getName(), e);
		}
	}

	private static List<Map<String, Object>> snapshotInventory(ServerPlayer player) {
		List<Map<String, Object>> out = new ArrayList<>();
		for (int i = 0; i < player.getInventory().items.size(); i++) {
			ItemStack s = player.getInventory().items.get(i);
			if (s.isEmpty()) continue;
			out.add(itemMap("main", i, s));
		}
		for (int i = 0; i < player.getInventory().armor.size(); i++) {
			ItemStack s = player.getInventory().armor.get(i);
			if (s.isEmpty()) continue;
			out.add(itemMap("armor", i, s));
		}
		for (int i = 0; i < player.getInventory().offhand.size(); i++) {
			ItemStack s = player.getInventory().offhand.get(i);
			if (s.isEmpty()) continue;
			out.add(itemMap("offhand", i, s));
		}
		return out;
	}

	private static Map<String, Object> itemMap(String slotType, int slot, ItemStack stack) {
		Map<String, Object> m = new HashMap<>();
		m.put("slotType", slotType);
		m.put("slot", slot);
		ResourceLocation key = ForgeRegistries.ITEMS.getKey(stack.getItem());
		m.put("id", key != null ? key.toString() : "minecraft:air");
		m.put("count", stack.getCount());
		m.put("display", stack.getHoverName().getString());
		if (stack.hasTag()) {
			m.put("nbt", stack.getTag().toString());
		}
		return m;
	}

	private static String formatGunId(String gunId) {
		String[] parts = gunId.replace('_', ' ').split(" ");
		StringBuilder sb = new StringBuilder();
		for (String part : parts) {
			if (part.isEmpty()) continue;
			sb.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1)).append(" ");
		}
		return sb.toString().trim();
	}
} 