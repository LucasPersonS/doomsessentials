package org.lupz.doomsdayessentials.killfeed.event;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import org.lupz.doomsdayessentials.EssentialsMod;
import org.lupz.doomsdayessentials.killfeed.network.packet.KillFeedPacket;
import org.lupz.doomsdayessentials.network.PacketHandler;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.eventbus.api.EventPriority;

import java.util.UUID;

@Mod.EventBusSubscriber(modid = EssentialsMod.MOD_ID)
public class KillEventHandler {

	@SubscribeEvent(priority = EventPriority.LOWEST)
	public static void onLivingDeath(LivingDeathEvent event) {
		if (event.isCanceled()) {
			return;
		}
		
		if (event.getEntity().level().isClientSide()) {
			return;
		}

		// Allow any living victim (players or mobs)
		LivingEntity victim = event.getEntity();

		DamageSource source = event.getSource();
		Entity killer = source.getEntity();
		Entity directKiller = source.getDirectEntity();

		// Only broadcast when a PLAYER is the killer (PvP or PvE)
		if (!(killer instanceof Player) || killer == victim) {
			return;
		}
		Player killerPlayer = (Player) killer;

		String victimName = victim.getDisplayName().getString();
		UUID victimUUID = victim.getUUID();
		String victimTypeId = ForgeRegistries.ENTITY_TYPES.getKey(victim.getType()).toString();

		UUID killerUUID = killerPlayer.getUUID();
		String killerName = killerPlayer.getDisplayName().getString();
		String killerTypeId = ForgeRegistries.ENTITY_TYPES.getKey(killerPlayer.getType()).toString();

		ItemStack weapon = ItemStack.EMPTY;
		String weaponName = "Fists";
		String weaponId = "minecraft:air";

		weapon = killerPlayer.getMainHandItem();
		if (!weapon.isEmpty()) {
			weaponName = weapon.getDisplayName().getString();
			ResourceLocation key = ForgeRegistries.ITEMS.getKey(weapon.getItem());
			if (key != null) {
				weaponId = key.toString();
			}
			// If TACZ style, prefer GunId NBT for both display and id mapping
			CompoundTag tag = weapon.getTag();
			if (tag != null && tag.contains("GunId", net.minecraft.nbt.Tag.TAG_STRING)) {
				String gunIdRaw = tag.getString("GunId");
				if (!gunIdRaw.isEmpty()) {
					String normalizedId = gunIdRaw.contains(":") ? gunIdRaw : ("tacz:" + gunIdRaw);
					weaponId = normalizedId;
					weaponName = formatGunId(gunIdRaw.contains(":") ? gunIdRaw.split(":",2)[1] : gunIdRaw);
					
					// Debug log
					System.out.println("KillEventHandler: GunId from NBT: " + gunIdRaw);
					System.out.println("KillEventHandler: Normalized weaponId: " + weaponId);
				}
			}
		}

		KillFeedPacket packet = new KillFeedPacket(killerUUID, killerName, victimUUID, victimName, weaponName, weaponId, victimTypeId);
		PacketHandler.CHANNEL.send(PacketDistributor.ALL.noArg(), packet);
	}

	private static String formatGunId(String gunId) {
		// Replace underscores with spaces and capitalize words
		String[] parts = gunId.replace('_', ' ').split(" ");
		StringBuilder sb = new StringBuilder();
		for (String part : parts) {
			if (part.isEmpty()) continue;
			sb.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1)).append(" ");
		}
		return sb.toString().trim();
	}
} 