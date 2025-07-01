package org.lupz.doomsdayessentials.injury.client;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import org.lupz.doomsdayessentials.EssentialsMod;

@EventBusSubscriber(modid = EssentialsMod.MOD_ID, value = Dist.CLIENT, bus = Bus.MOD)
public class InjuryClientEvents {

   @SubscribeEvent
   public static void registerGuiOverlays(RegisterGuiOverlaysEvent event) {
      event.registerAbove(VanillaGuiOverlay.ARMOR_LEVEL.id(), "injury_level", InjuryHudOverlay.HUD_INJURY);
   }
} 