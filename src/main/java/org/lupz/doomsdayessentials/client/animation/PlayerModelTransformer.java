package org.lupz.doomsdayessentials.client.animation;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.entity.player.Player;

public class PlayerModelTransformer {
	private final Player player;
	private final PlayerModel<?> model;
	private final float partial;

	public PlayerModelTransformer(Player player, PlayerModel<?> model) {
		this.player = player;
		this.model = model;
		this.partial = Minecraft.getInstance().getFrameTime();
	}

	public float getPartialTick() { return partial; }
	public PlayerModel<?> getRawModel() { return model; }

	public void resetToVanillaDefaults() {
		reset(model.head);
		reset(model.hat);
		reset(model.jacket);
		reset(model.body);
		reset(model.rightArm); model.rightArm.x = -5.0F; model.rightArm.y = 2.0F; model.rightArm.z = 0.0F; model.rightSleeve.copyFrom(model.rightArm);
		reset(model.leftArm);  model.leftArm.x = 5.0F;  model.leftArm.y = 2.0F; model.leftArm.z = 0.0F; model.leftSleeve.copyFrom(model.leftArm);
		reset(model.leftLeg);  model.leftLeg.x = 1.9F; model.leftLeg.y = 12.0F; model.leftLeg.z = 0.0F; model.leftPants.copyFrom(model.leftLeg);
		reset(model.rightLeg); model.rightLeg.x = -1.9F;model.rightLeg.y = 12.0F; model.rightLeg.z = 0.0F; model.rightPants.copyFrom(model.rightLeg);
	}

	private void reset(ModelPart p) { p.xRot = 0; p.yRot = 0; p.zRot = 0; p.x = 0; p.y = 0; p.z = 0; }

	// Head rotations (degrees absolute)
	public PlayerModelTransformer rotateHeadPitch(float deg) { model.head.xRot = (float)Math.toRadians(deg); return this; }
	public PlayerModelTransformer rotateHeadYaw(float deg) { model.head.yRot = (float)Math.toRadians(deg); return this; }
	public PlayerModelTransformer rotateHeadRoll(float deg) { model.head.zRot = (float)Math.toRadians(deg); return this; }
	// Additive head
	public PlayerModelTransformer addHeadYaw(float deg) { model.head.yRot += (float)Math.toRadians(deg); return this; }
	public PlayerModelTransformer addHeadRoll(float deg) { model.head.zRot += (float)Math.toRadians(deg); return this; }

	// Translate parts (model space units)
	public PlayerModelTransformer translateHead(float x, float y, float z) { model.head.x += x; model.head.y += y; model.head.z += z; return this; }
	public PlayerModelTransformer translateRightArm(float x, float y, float z) { model.rightArm.x += x; model.rightArm.y += y; model.rightArm.z += z; return this; }
	public PlayerModelTransformer translateLeftArm(float x, float y, float z) { model.leftArm.x += x; model.leftArm.y += y; model.leftArm.z += z; return this; }
	public PlayerModelTransformer translateRightLeg(float x, float y, float z) { model.rightLeg.x += x; model.rightLeg.y += y; model.rightLeg.z += z; return this; }
	public PlayerModelTransformer translateLeftLeg(float x, float y, float z) { model.leftLeg.x += x; model.leftLeg.y += y; model.leftLeg.z += z; return this; }

	// Lerp rotations in radians (ParCool style)
	public PlayerModelTransformer rotateRightArm(float xRad, float yRad, float zRad, float factor) {
		lerpRot(model.rightArm, xRad, yRad, zRad, factor); return this;
	}
	public PlayerModelTransformer rotateLeftArm(float xRad, float yRad, float zRad, float factor) {
		lerpRot(model.leftArm, xRad, yRad, zRad, factor); return this;
	}
	public PlayerModelTransformer rotateRightLeg(float xRad, float yRad, float zRad, float factor) {
		lerpRot(model.rightLeg, xRad, yRad, zRad, factor); return this;
	}
	public PlayerModelTransformer rotateLeftLeg(float xRad, float yRad, float zRad, float factor) {
		lerpRot(model.leftLeg, xRad, yRad, zRad, factor); return this;
	}

	private static void lerpRot(ModelPart part, float xRad, float yRad, float zRad, float f) {
		part.xRot = lerp(part.xRot, xRad, f);
		part.yRot = lerp(part.yRot, yRad, f);
		part.zRot = lerp(part.zRot, zRad, f);
	}
	private static float lerp(float a, float b, float f) { return a + (b - a) * f; }
} 