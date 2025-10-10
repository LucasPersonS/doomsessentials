package org.lupz.doomsdayessentials.client.animation;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.world.entity.player.Player;

public class PlayerModelRotator {
	private final PoseStack stack;
	private final Player player;
	private final float partial;

	public PlayerModelRotator(PoseStack stack, Player player, float partial) {
		this.stack = stack;
		this.player = player;
		this.partial = partial;
	}

	public float getPartialTick() { return partial; }

	public PlayerModelRotator rotateYawRightward(float deg) { stack.mulPose(Axis.YN.rotationDegrees(deg)); return this; }
	public PlayerModelRotator rotatePitchFrontward(float deg) { stack.mulPose(Axis.XN.rotationDegrees(deg)); return this; }
	public PlayerModelRotator rotateRollRightward(float deg) { stack.mulPose(Axis.ZN.rotationDegrees(deg)); return this; }
	public PlayerModelRotator translate(float x, float y, float z) { stack.translate(x, y, z); return this; }
} 