package org.lupz.doomsdayessentials.client.animation;

import net.minecraft.client.model.PlayerModel;
import net.minecraft.world.entity.player.Player;

public class SlidingAnimator extends Animator {
	private static final int MAX_TRANSITION_TICK = 5;

	private static float easeSinInOut(float x) {
		if (x < 0) x = 0; if (x > 1) x = 1;
		// ParCool uses Easing.sinInOut(0,1,0,1). We approximate with standard sin-in-out curve.
		return (float)(0.5 - 0.5 * Math.cos(Math.PI * x));
	}

	private static float yawFromVector(double x, double z) { if (x == 0 && z == 0) return 0f; return (float) Math.toDegrees(Math.atan2(-x, z)); }

	@Override
	public boolean rotatePre(Player player, PlayerModelRotator rotator) {
		// Use velocity direction like ParCool’s slide vector fallback
		net.minecraft.world.phys.Vec3 vec = player.getDeltaMovement();
		if (vec.horizontalDistanceSqr() < 1.0E-4) vec = player.getLookAngle();
		float animFactor = (getTick() + rotator.getPartialTick()) / MAX_TRANSITION_TICK;
		if (animFactor > 1) animFactor = 1;
		float eased = easeSinInOut(animFactor);
		float yRot = yawFromVector(vec.x, vec.z);
		rotator
			.rotateYawRightward(180f + yRot)
			.rotatePitchFrontward(-55f * eased)
			.translate(0.35f * eased, 0, 0)
			.rotateYawRightward(-55f * eased)
			.translate(0, -0.7f * eased, -0.3f * eased);
		return true; // cancel vanilla rotations
	}

	@Override
	public boolean animatePre(Player player, PlayerModelTransformer transformer) {
		float animFactor = (getTick() + transformer.getPartialTick()) / MAX_TRANSITION_TICK;
		if (animFactor > 1) animFactor = 1;
		float eased = easeSinInOut(animFactor);
		// Apply ParCool-equivalent transforms
		transformer
			.translateLeftLeg(0, -1.2f * eased, -2f * eased)
			.translateRightArm(0, 1.2f * eased, 1.2f * eased)
			.translateHead(0, 0, -eased)
			.rotateHeadPitch(50 * eased)
			.addHeadYaw(50 * eased)
			.addHeadRoll(-10 * eased)
			.rotateRightArm((float) Math.toRadians(50), (float) Math.toRadians(-40), 0, eased)
			.rotateLeftArm((float) Math.toRadians(20), 0, (float) Math.toRadians(-100), eased)
			.rotateRightLeg((float) Math.toRadians(-30), (float) Math.toRadians(40), 0, eased)
			.rotateLeftLeg((float) Math.toRadians(40), (float) Math.toRadians(-30), (float) Math.toRadians(15), eased);
		return true; // cancel vanilla limb animation fully
	}

	@Override
	public void animatePost(PlayerModel<?> model, Player player, float partialTick) {
		// No-op; ParCool uses animatePost but we’re handling in animatePre since we cancel vanilla.
	}
} 