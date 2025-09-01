package org.lupz.doomsdayessentials.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lupz.doomsdayessentials.EssentialsMod;
import org.lupz.doomsdayessentials.client.key.KeyBindings;
import org.lupz.doomsdayessentials.entity.SentryEntity;
import org.lupz.doomsdayessentials.network.MountSentryWeaponPacket;
import org.lupz.doomsdayessentials.network.PacketHandler;

@Mod.EventBusSubscriber(modid = EssentialsMod.MOD_ID, value = Dist.CLIENT)
public class ProfessionClientEvents {

	@Mod.EventBusSubscriber(modid = EssentialsMod.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
	public static class ModBus {
		@SubscribeEvent
		public static void onRegisterKeys(RegisterKeyMappingsEvent e) {
			e.register(KeyBindings.USE_SKILL);
		}
	}

	private static SentryEntity getLookedSentry() {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null || mc.level == null || mc.hitResult == null) return null;
		if (!(mc.hitResult instanceof net.minecraft.world.phys.BlockHitResult bhr)) return null;
		// Expand small AABB around crosshair block to find nearby sentry; simple approach
		var box = new net.minecraft.world.phys.AABB(bhr.getBlockPos()).inflate(1.5);
		for (var e : mc.level.getEntities(null, box)) {
			if (e instanceof SentryEntity s) return s;
		}
		return null;
	}

	@SubscribeEvent
	public static void onClientTick(TickEvent.ClientTickEvent event) {
		if (event.phase != TickEvent.Phase.END) return;
		Minecraft mc = Minecraft.getInstance();
		while (KeyBindings.USE_SKILL.consumeClick()) {
			SentryEntity s = getLookedSentry();
			if (s != null && mc.player != null) {
				ItemStack inHand = mc.player.getMainHandItem();
				var key = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(inHand.getItem());
				if (key != null && "tacz".equals(key.getNamespace())) {
					PacketHandler.CHANNEL.sendToServer(new MountSentryWeaponPacket(s.getId()));
					continue;
				}
			}
			PacketHandler.CHANNEL.sendToServer(new org.lupz.doomsdayessentials.professions.network.UseProfessionSkillPacket());
		}
	}

	@SubscribeEvent
	public static void onRenderOverlay(RenderGuiOverlayEvent.Post event) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.options.hideGui) return;
		SentryEntity s = getLookedSentry();
		if (s == null) return;
		GuiGraphics g = event.getGuiGraphics();
		String keyName = KeyBindings.USE_SKILL.getTranslatedKeyMessage().getString();
		String msg = "Press " + keyName + " with a Gun";
		int w = mc.getWindow().getGuiScaledWidth();
		int h = mc.getWindow().getGuiScaledHeight();
		g.drawString(mc.font, msg, (w - mc.font.width(msg)) / 2, h / 2 + 20, 0xFFFFFF, true);
	}
} 