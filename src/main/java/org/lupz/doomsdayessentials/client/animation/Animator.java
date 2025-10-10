package org.lupz.doomsdayessentials.client.animation;

import net.minecraft.client.model.PlayerModel;
import net.minecraft.world.entity.player.Player;

public abstract class Animator {
	private int tick = 0;

	public void tick(Player player) {
		tick++;
	}

	protected int getTick() {
		return tick;
	}

	// New cancellable hooks (ParCool-style)
	public boolean animatePre(Player player, PlayerModelTransformer transformer) { return false; }
	public boolean rotatePre(Player player, PlayerModelRotator rotator) { return false; }

	// Legacy hooks (still used in some places)
	public void rotatePre(com.mojang.blaze3d.vertex.PoseStack stack, Player player, float partialTick) {}
	public void animatePost(PlayerModel<?> model, Player player, float partialTick) {}
} 