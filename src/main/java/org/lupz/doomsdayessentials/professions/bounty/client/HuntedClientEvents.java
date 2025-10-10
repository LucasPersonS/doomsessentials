package org.lupz.doomsdayessentials.professions.bounty.client;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lupz.doomsdayessentials.EssentialsMod;

@Mod.EventBusSubscriber(modid = EssentialsMod.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class HuntedClientEvents {
	private HuntedClientEvents() {}

	@SubscribeEvent
	public static void onRegisterGuiOverlays(RegisterGuiOverlaysEvent event) {
		event.registerAbove(VanillaGuiOverlay.PLAYER_HEALTH.id(), "hunted_state", HuntedHudOverlay.HUD);
	}
} 