package org.lupz.doomsdayessentials.event;

import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lupz.doomsdayessentials.EssentialsMod;

@Mod.EventBusSubscriber(modid = EssentialsMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class FacelessSpawner {
    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent e){
        // Natural Faceless spawning disabled – entity can only be spawned via commands/summon.
        return;
    }
} 