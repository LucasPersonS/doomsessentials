package org.lupz.doomsdayessentials.mixin.client;

import net.minecraft.client.model.PlayerModel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.lupz.doomsdayessentials.client.animation.AnimationManager;
import org.lupz.doomsdayessentials.client.animation.Animator;
import org.lupz.doomsdayessentials.client.animation.PlayerModelTransformer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.lupz.doomsdayessentials.EssentialsMod;

@Mixin(PlayerModel.class)
public abstract class PlayerModelMixin<T extends LivingEntity> {
	@Inject(method = "setupAnim(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V", at = @At("HEAD"), cancellable = true)
	private void de$setupAnimHead(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, CallbackInfo ci) {
		if (!(entity instanceof Player player)) return;
		Animator animator = AnimationManager.getAnimator(player);
		if (animator == null) return;
		PlayerModelTransformer transformer = new PlayerModelTransformer(player, (PlayerModel<?>) (Object) this);
		// Ensure pure baseline (no vanilla) when we choose to cancel
		transformer.resetToVanillaDefaults();
		boolean cancel = animator.animatePre(player, transformer);
		if (cancel) {
			EssentialsMod.LOGGER.debug("[Mixin] Canceling vanilla setupAnim for {}", player.getGameProfile().getName());
			ci.cancel();
		}
	}

	@Inject(method = "setupAnim(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V", at = @At("TAIL"))
	private void de$setupAnimTail(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, CallbackInfo ci) {
		if (!(entity instanceof Player player)) return;
		Animator animator = AnimationManager.getAnimator(player);
		if (animator == null) return;
		// Reset baseline then add post adjustments (optional)
		AnimationManager.applyBaselineIfPresent((PlayerModel<?>) (Object) this, player);
		EssentialsMod.LOGGER.debug("[Mixin] PlayerModel.setupAnim -> animatePost for {}", player.getGameProfile().getName());
		animator.animatePost((PlayerModel<?>) (Object) this, player, net.minecraft.client.Minecraft.getInstance().getFrameTime());
	}
} 