package org.lupz.doomsdayessentials.client;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lupz.doomsdayessentials.EssentialsMod;

@Mod.EventBusSubscriber(modid = EssentialsMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class SkyTintRender {

    @SubscribeEvent
    public static void onFogColor(ViewportEvent.ComputeFogColor event){
        // If frequency is active, skip dooms sky tint so frequency red fully controls the sky
        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
        if (mc.player != null && mc.player.getCapability(org.lupz.doomsdayessentials.frequency.capability.FrequencyCapabilityProvider.FREQUENCY_CAPABILITY).map(cap -> cap.getLevel() > 0).orElse(false)) {
            return;
        }
        if(!SkyTintClientState.isActive()) return;

        float a = SkyTintClientState.getAlpha();
        float r = event.getRed()   * (1.0f - a) + SkyTintClientState.getRed()   * a;
        float g = event.getGreen() * (1.0f - a) + SkyTintClientState.getGreen() * a;
        float b = event.getBlue()  * (1.0f - a) + SkyTintClientState.getBlue()  * a;

        event.setRed(r);
        event.setGreen(g);
        event.setBlue(b);
    }
} 