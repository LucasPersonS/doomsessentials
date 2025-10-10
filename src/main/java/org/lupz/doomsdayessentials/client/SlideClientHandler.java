package org.lupz.doomsdayessentials.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lupz.doomsdayessentials.EssentialsMod;
import org.lupz.doomsdayessentials.network.PacketHandler;
import org.lupz.doomsdayessentials.network.StartSlidePacket;
import org.lupz.doomsdayessentials.network.CancelSlidePacket;
import org.lupz.doomsdayessentials.client.animation.AnimationManager;
import org.lupz.doomsdayessentials.client.animation.SlidingAnimator;

@Mod.EventBusSubscriber(modid = EssentialsMod.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class SlideClientHandler {
	private static boolean slideKeyConsumed = false;
	private static long lastSendMs = 0L;
	private static final long COOLDOWN_MS = 500L; // avoid spam
	private static boolean jumpCancelConsumed = false;

	@SubscribeEvent
	public static void onClientTick(TickEvent.ClientTickEvent event) {
		if (event.phase != TickEvent.Phase.END) return;
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null || mc.level == null) return;

		boolean maybeSliding = mc.player.getPose() == net.minecraft.world.entity.Pose.SWIMMING && mc.player.onGround() && !mc.player.isInWaterOrBubble();
		boolean animatorActive = AnimationManager.getAnimator(mc.player) instanceof SlidingAnimator;
		boolean jumpDown = mc.options != null && mc.options.keyJump.isDown();
		// Cancel slide on jump if either heuristic or animator is active
		if ((maybeSliding || animatorActive)) {
			if (jumpDown && !jumpCancelConsumed) {
				EssentialsMod.LOGGER.debug("[SlideClient] Jump pressed -> cancel slide (client+server) for {}", mc.player.getGameProfile().getName());
				PacketHandler.CHANNEL.sendToServer(new CancelSlidePacket());
				AnimationManager.clearAnimator(mc.player);
				jumpCancelConsumed = true;
			}
			if (!jumpDown) jumpCancelConsumed = false;
		}

		// Require sprint + sneak key held and TACZ item in hand to start
		boolean sprinting = mc.player.isSprinting() || (mc.options != null && mc.options.keySprint.isDown());
		boolean sneaking = mc.player.isShiftKeyDown() || (mc.options != null && mc.options.keyShift.isDown());
		if (!sprinting || !sneaking) {
			slideKeyConsumed = false;
			return;
		}

		// only trigger once per hold
		if (slideKeyConsumed) return;

		ItemStack held = mc.player.getMainHandItem();
		var key = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(held.getItem());
		if (key == null || key.getNamespace() == null || !"tacz".equals(key.getNamespace())) return;

		long now = System.currentTimeMillis();
		if (now - lastSendMs < COOLDOWN_MS) return;

		EssentialsMod.LOGGER.debug("[SlideClient] Sending StartSlidePacket from client for {}", mc.player.getGameProfile().getName());
		PacketHandler.CHANNEL.sendToServer(new StartSlidePacket());
		AnimationManager.setAnimator(mc.player, new SlidingAnimator());
		EssentialsMod.LOGGER.debug("[SlideClient] Local animator set to SlidingAnimator for {}", mc.player.getGameProfile().getName());
		lastSendMs = now;
		slideKeyConsumed = true;
	}
} 