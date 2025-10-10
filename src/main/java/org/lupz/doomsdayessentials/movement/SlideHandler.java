package org.lupz.doomsdayessentials.movement;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.phys.Vec3;

public final class SlideHandler {
	private SlideHandler() {}

	// NBT keys in persistent data
	private static final String KEY_SLIDE_TICKS = "slideTicks";
	private static final String KEY_IS_SLIDING = "isSliding";
	private static final String KEY_SLIDE_END_CROUCH_TICKS = "slideEndCrouchTicks";
	private static final String KEY_SLIDE_AIR_GRACE = "slideAirGrace";

	// Tuning (Warzone-like feel)
	private static final int MAX_SLIDE_TICKS = 10; // ~3.0s
	private static final double BASE_SLIDE_IMPULSE = 2.15; // base push
	private static final double FRICTION = 1.12; // very gentle decay for long glide
	private static final double DOWNHILL_ACCEL = 0.04; // stronger accel downhill
	private static final double UPHILL_DAMP = 0.94; // slight damp uphill
	private static final int END_CROUCH_TICKS = 16; // slightly longer crouch at end
	private static final int AIR_GRACE_TICKS = 2; // small bumps won't cancel

	public static void tryStartSlide(ServerPlayer player) {
		if (player.level().isClientSide()) return;
		if (!(player.level() instanceof ServerLevel)) return;
		if (player.isPassenger() || player.isSwimming() || player.isInWaterOrBubble() || player.isFallFlying()) return;
		if (!player.onGround()) return;
		
		var held = player.getMainHandItem();
		var key = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(held.getItem());
		if (key == null || key.getNamespace() == null || !"tacz".equals(key.getNamespace())) return;
		java.util.concurrent.atomic.AtomicBoolean blocked = new java.util.concurrent.atomic.AtomicBoolean(false);
		player.getCapability(org.lupz.doomsdayessentials.injury.capability.InjuryCapabilityProvider.INJURY_CAPABILITY).ifPresent(cap -> {
			if (cap.isDowned()) blocked.set(true);
		});
		if (blocked.get()) return;
		if (player.getPersistentData().getBoolean(KEY_IS_SLIDING)) return;
		int cooldown = player.getPersistentData().getInt("slideCooldown");
		if (cooldown > 0) return;

		Vec3 vel = player.getDeltaMovement();
		double currentSpeed = Math.sqrt(vel.x * vel.x + vel.z * vel.z);
		double impulse = BASE_SLIDE_IMPULSE + currentSpeed * 0.9;
		Vec3 look = player.getLookAngle().normalize();
		Vec3 motion = new Vec3(look.x * impulse, 0.0, look.z * impulse);
		player.setDeltaMovement(motion);
		player.hurtMarked = true;
		player.setPose(Pose.SWIMMING);
		player.setSwimming(true);
		player.setSprinting(false);

		player.getPersistentData().putBoolean(KEY_IS_SLIDING, true);
		player.getPersistentData().putInt(KEY_SLIDE_TICKS, MAX_SLIDE_TICKS);
		player.getPersistentData().putInt(KEY_SLIDE_END_CROUCH_TICKS, 0);
		player.getPersistentData().putInt(KEY_SLIDE_AIR_GRACE, AIR_GRACE_TICKS);
		player.getPersistentData().putInt("slideCooldown", 20); // 1s cooldown

		org.lupz.doomsdayessentials.EssentialsMod.LOGGER.debug("[SlideServer] Slide START for {} at {} {} {}", player.getGameProfile().getName(), String.format("%.2f", player.getX()), String.format("%.2f", player.getY()), String.format("%.2f", player.getZ()));
		org.lupz.doomsdayessentials.network.PacketHandler.CHANNEL.send(
			net.minecraftforge.network.PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player),
			new org.lupz.doomsdayessentials.network.packet.s2c.StartSlideS2CPacket(player.getUUID())
		);
	}

	public static void tick(ServerPlayer player) {
		if (player.level().isClientSide()) return;
		int endCrouch = player.getPersistentData().getInt(KEY_SLIDE_END_CROUCH_TICKS);
		if (endCrouch > 0) {
			player.setPose(Pose.CROUCHING);
			player.getPersistentData().putInt(KEY_SLIDE_END_CROUCH_TICKS, endCrouch - 1);
			return;
		}

		if (!player.getPersistentData().getBoolean(KEY_IS_SLIDING)) return;

		int ticks = player.getPersistentData().getInt(KEY_SLIDE_TICKS);
		if (ticks <= 0) {
			endSlide(player);
			return;
		}

		int grace = player.getPersistentData().getInt(KEY_SLIDE_AIR_GRACE);
		if (!player.onGround()) {
			if (grace > 0) {
				player.getPersistentData().putInt(KEY_SLIDE_AIR_GRACE, grace - 1);
			} else {
				endSlide(player);
				return;
			}
		} else {
			player.getPersistentData().putInt(KEY_SLIDE_AIR_GRACE, AIR_GRACE_TICKS);
		}

		if (player.isInWaterOrBubble() || player.isFallFlying() || player.isPassenger()) {
			endSlide(player);
			return;
		}

		Vec3 d = player.getDeltaMovement();
		d = new Vec3(d.x * FRICTION, Math.min(d.y, 0.0), d.z * FRICTION);

		double pitch = Math.toRadians(player.getXRot());
		if (pitch < -0.1) {
			d = new Vec3(d.x * UPHILL_DAMP, d.y, d.z * UPHILL_DAMP);
		} else if (pitch > 0.1) {
			Vec3 fwd = player.getLookAngle().normalize();
			d = d.add(fwd.x * DOWNHILL_ACCEL, 0.0, fwd.z * DOWNHILL_ACCEL);
		}

		player.setDeltaMovement(d);
		player.hurtMarked = true;
		player.setPose(Pose.SWIMMING);
		player.setSwimming(true);

		player.getPersistentData().putInt(KEY_SLIDE_TICKS, ticks - 1);
	}

	public static void cancelSlide(ServerPlayer player) {
		if (!player.getPersistentData().getBoolean(KEY_IS_SLIDING)) return;
		Vec3 d = player.getDeltaMovement();
		player.setDeltaMovement(d.x, 0.42, d.z);
		endSlide(player);
	}

	public static void endSlide(ServerPlayer player) {
		player.getPersistentData().putBoolean(KEY_IS_SLIDING, false);
		player.getPersistentData().putInt(KEY_SLIDE_TICKS, 0);
		player.getPersistentData().putInt(KEY_SLIDE_END_CROUCH_TICKS, END_CROUCH_TICKS);
		player.setPose(Pose.CROUCHING);
		player.setSwimming(false);
		org.lupz.doomsdayessentials.EssentialsMod.LOGGER.debug("[SlideServer] Slide END for {}", player.getGameProfile().getName());
		org.lupz.doomsdayessentials.network.PacketHandler.CHANNEL.send(
			net.minecraftforge.network.PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player),
			new org.lupz.doomsdayessentials.network.packet.s2c.EndSlideS2CPacket(player.getUUID())
		);
	}

	public static boolean isSliding(ServerPlayer player) {
		return player.getPersistentData().getBoolean(KEY_IS_SLIDING);
	}
} 