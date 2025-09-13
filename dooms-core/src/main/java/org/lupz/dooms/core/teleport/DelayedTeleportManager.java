package org.lupz.dooms.core.teleport;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraft.core.particles.ParticleTypes;
import org.lupz.dooms.core.CoreConstants;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Mod.EventBusSubscriber(modid = CoreConstants.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class DelayedTeleportManager {

	static {
		MinecraftForge.EVENT_BUS.addListener(DelayedTeleportManager::onServerTick);
	}

	private record Pending(UUID uuid, ServerLevel destLevel, Vec3 destPos, float yaw, float pitch, int ticks, boolean smoke) {}

	private static final Map<UUID, Pending> PENDING = new ConcurrentHashMap<>();
	private static final Map<UUID, Vec3> LAST_ORIGIN = new ConcurrentHashMap<>();

	public static void scheduleTeleport(ServerPlayer player, Vec3 origin, ServerLevel destLevel, Vec3 destPos, int delayTicks) {
		scheduleTeleport(player, origin, destLevel, destPos, delayTicks, false);
	}

	public static void scheduleTeleport(ServerPlayer player, Vec3 origin, ServerLevel destLevel, Vec3 destPos, int delayTicks, boolean smoke) {
		PENDING.put(player.getUUID(), new Pending(player.getUUID(), destLevel, destPos, player.getYRot(), player.getXRot(), delayTicks, smoke));
		LAST_ORIGIN.put(player.getUUID(), origin);
		player.addEffect(new MobEffectInstance(MobEffects.LEVITATION, delayTicks, 8, false, false));
		destLevel.sendParticles(ParticleTypes.LARGE_SMOKE, player.getX(), player.getY(), player.getZ(), 50, 0.5, 1, 0.5, 0.02);
	}

	public static boolean teleportBack(ServerPlayer player) {
		Vec3 origin = LAST_ORIGIN.remove(player.getUUID());
		if (origin != null) {
			player.teleportTo(player.serverLevel(), origin.x, origin.y, origin.z, player.getYRot(), player.getXRot());
			return true;
		}
		return false;
	}

	@SubscribeEvent
	public static void onServerTick(TickEvent.ServerTickEvent event) {
		if (event.phase != TickEvent.Phase.END) return;
		PENDING.values().removeIf(p -> {
			if (p.smoke) {
				ServerPlayer player = p.destLevel.getServer().getPlayerList().getPlayer(p.uuid);
				if (player != null) {
					((ServerLevel) player.level()).sendParticles(ParticleTypes.LARGE_SMOKE, player.getX(), player.getY() + 0.5, player.getZ(), 20, 0.3, 0.4, 0.3, 0.02);
				}
			}

			int remaining = p.ticks - 1;
			if (remaining <= 0) {
				ServerPlayer player = p.destLevel.getServer().getPlayerList().getPlayer(p.uuid);
				if (player != null) {
					player.teleportTo(p.destLevel, p.destPos.x, p.destPos.y, p.destPos.z, p.yaw, p.pitch);
					player.removeEffect(MobEffects.LEVITATION);
					p.destLevel.sendParticles(ParticleTypes.PORTAL, p.destPos.x, p.destPos.y, p.destPos.z, 80, 1, 1, 1, 0.05);

					PENDING.remove(p.uuid);
				}
				return true;
			} else {
				PENDING.put(p.uuid, new Pending(p.uuid, p.destLevel, p.destPos, p.yaw, p.pitch, remaining, p.smoke));
				return false;
			}
		});
	}
} 