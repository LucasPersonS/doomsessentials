package org.lupz.doomsdayessentials.client.animation;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lupz.doomsdayessentials.EssentialsMod;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = EssentialsMod.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class AnimationManager {
	private static final Map<UUID, Animator> CURRENT = new HashMap<>();

	// Baseline cache to avoid cumulative additive transforms within the same frame
	private static final Map<UUID, Baseline> BASELINES = new HashMap<>();

	private static class Baseline {
		float headX, headY, headZ, headOffZ;
		float rArmX, rArmY, rArmZ, rArmOffY, rArmOffZ;
		float lArmX, lArmY, lArmZ;
		float rLegX, rLegY, rLegZ;
		float lLegX, lLegY, lLegZ, lLegOffY, lLegOffZ;

		static Baseline capture(PlayerModel<?> model) {
			Baseline b = new Baseline();
			b.headX = model.head.xRot; b.headY = model.head.yRot; b.headZ = model.head.zRot; b.headOffZ = model.head.z;
			b.rArmX = model.rightArm.xRot; b.rArmY = model.rightArm.yRot; b.rArmZ = model.rightArm.zRot; b.rArmOffY = model.rightArm.y; b.rArmOffZ = model.rightArm.z;
			b.lArmX = model.leftArm.xRot; b.lArmY = model.leftArm.yRot; b.lArmZ = model.leftArm.zRot;
			b.rLegX = model.rightLeg.xRot; b.rLegY = model.rightLeg.yRot; b.rLegZ = model.rightLeg.zRot;
			b.lLegX = model.leftLeg.xRot; b.lLegY = model.leftLeg.yRot; b.lLegZ = model.leftLeg.zRot; b.lLegOffY = model.leftLeg.y; b.lLegOffZ = model.leftLeg.z;
			return b;
		}

		void applyTo(PlayerModel<?> model) {
			model.head.xRot = headX; model.head.yRot = headY; model.head.zRot = headZ; model.head.z = headOffZ;
			model.rightArm.xRot = rArmX; model.rightArm.yRot = rArmY; model.rightArm.zRot = rArmZ; model.rightArm.y = rArmOffY; model.rightArm.z = rArmOffZ;
			model.leftArm.xRot = lArmX; model.leftArm.yRot = lArmY; model.leftArm.zRot = lArmZ;
			model.rightLeg.xRot = rLegX; model.rightLeg.yRot = rLegY; model.rightLeg.zRot = rLegZ;
			model.leftLeg.xRot = lLegX; model.leftLeg.yRot = lLegY; model.leftLeg.zRot = lLegZ; model.leftLeg.y = lLegOffY; model.leftLeg.z = lLegOffZ;
		}
	}

	public static void setAnimator(Player player, Animator animator) {
		if (player == null) return;
		CURRENT.put(player.getUUID(), animator);
		EssentialsMod.LOGGER.debug("[AnimMgr] setAnimator {} -> {}", player.getGameProfile().getName(), animator.getClass().getSimpleName());
	}

	public static void clearAnimator(Player player) {
		if (player == null) return;
		CURRENT.remove(player.getUUID());
		EssentialsMod.LOGGER.debug("[AnimMgr] clearAnimator {}", player.getGameProfile().getName());
	}

	public static Animator getAnimator(Player player) {
		if (player == null) return null;
		Animator a = CURRENT.get(player.getUUID());
		return a;
	}

	// Called by mixin before applying custom transforms to ensure a stable baseline per frame
	public static void applyBaselineIfPresent(PlayerModel<?> model, Player player) {
		UUID id = player.getUUID();
		Baseline b = BASELINES.get(id);
		if (b == null) {
			BASELINES.put(id, Baseline.capture(model));
		} else {
			b.applyTo(model);
		}
	}

	// Called when server signals slide end/cancel: drop any cached baseline so next frame is fresh
	public static void onSlideEnd(Player player) {
		if (player == null) return;
		BASELINES.remove(player.getUUID());
	}

	@SubscribeEvent
	public static void onClientTick(TickEvent.ClientTickEvent event) {
		Minecraft mc = Minecraft.getInstance();
		if (event.phase == TickEvent.Phase.START) {
			// New frame: clear baselines so next render pass captures fresh vanilla state
			BASELINES.clear();
			return;
		}
		if (event.phase != TickEvent.Phase.END) return;
		if (mc.level == null) return;
		CURRENT.entrySet().removeIf(e -> e.getValue() == null);
		for (Map.Entry<UUID, Animator> e : CURRENT.entrySet()) {
			Player p = mc.level.getPlayerByUUID(e.getKey());
			if (p == null) continue;
			Animator a = e.getValue();
			a.tick(p);
		}
	}
} 