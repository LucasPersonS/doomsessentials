package org.lupz.doomsdayessentials.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.model.PlayerModel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.lupz.doomsdayessentials.client.animation.AnimationManager;
import org.lupz.doomsdayessentials.client.animation.Animator;
import org.lupz.doomsdayessentials.client.animation.PlayerModelRotator;
import org.lupz.doomsdayessentials.EssentialsMod;

@Mixin(PlayerRenderer.class)
public abstract class PlayerRendererMixin extends LivingEntityRenderer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
	public PlayerRendererMixin(EntityRendererProvider.Context ctx, PlayerModel<AbstractClientPlayer> model, float shadow) { super(ctx, model, shadow); }

	@Inject(method = "setupRotations(Lnet/minecraft/client/player/AbstractClientPlayer;Lcom/mojang/blaze3d/vertex/PoseStack;FFF)V", at = @At("HEAD"), cancellable = true)
	private void de$setupRotationsHead(AbstractClientPlayer player, PoseStack stack, float xRot, float yRot, float zRot, CallbackInfo ci) {
		Animator animator = AnimationManager.getAnimator(player);
		if (animator == null) return;
		PlayerModelRotator rotator = new PlayerModelRotator(stack, player, Minecraft.getInstance().getFrameTime());
		boolean cancel = animator.rotatePre(player, rotator);
		if (cancel) {
			EssentialsMod.LOGGER.debug("[Mixin] Canceling vanilla rotations for {}", player.getGameProfile().getName());
			ci.cancel();
		}
	}
} 