package org.lupz.doomsdayessentials.killfeed.client;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lupz.doomsdayessentials.EssentialsMod;

/**
 * Client-only listener to reset the local kill streak on death and on logout.
 */
@Mod.EventBusSubscriber(modid = EssentialsMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class KillStreakClientEvents {
	@SubscribeEvent
	public static void onLocalDeath(LivingDeathEvent event) {
		var mc = net.minecraft.client.Minecraft.getInstance();
		if (mc.player == null) return;
		if (event.getEntity() == mc.player) {
			KillFeedManager.resetLocalKillStreak();
		}
	}

	@SubscribeEvent
	public static void onClientLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
		KillFeedManager.resetLocalKillStreak();
	}
} 