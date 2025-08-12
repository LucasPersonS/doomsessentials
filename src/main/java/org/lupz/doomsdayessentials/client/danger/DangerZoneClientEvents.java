package org.lupz.doomsdayessentials.client.danger;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lupz.doomsdayessentials.EssentialsMod;

@Mod.EventBusSubscriber(modid = EssentialsMod.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class DangerZoneClientEvents {
    @SubscribeEvent
    public static void registerOverlays(RegisterGuiOverlaysEvent event){
        event.registerAbove(VanillaGuiOverlay.PLAYER_HEALTH.id(), "danger_zone", DangerZoneOverlay.OVERLAY);
    }
} 