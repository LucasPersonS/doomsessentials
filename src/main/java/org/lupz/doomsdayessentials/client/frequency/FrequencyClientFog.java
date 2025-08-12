package org.lupz.doomsdayessentials.client.frequency;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lupz.doomsdayessentials.EssentialsMod;
import net.minecraft.client.Minecraft;
import org.lupz.doomsdayessentials.frequency.capability.FrequencyCapabilityProvider;

@Mod.EventBusSubscriber(modid = EssentialsMod.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class FrequencyClientFog {
    private FrequencyClientFog() {}

    @SubscribeEvent
    public static void onComputeFogColor(ViewportEvent.ComputeFogColor event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        mc.player.getCapability(FrequencyCapabilityProvider.FREQUENCY_CAPABILITY).ifPresent(cap -> {
            int level = cap.getLevel();
            if (level <= 0) return;
            // Non-linear ramp so high levels are much stronger
            float t = Math.min(1.0f, level / 100.0f);
            float blend = 0.18f + (float)Math.pow(t, 1.6f) * 0.80f; // 0.18 .. ~0.98
            if (blend > 0.98f) blend = 0.98f; // avoid full clamp
            float inv = 1.0f - blend;
            float r = event.getRed() * inv + 1.0f * blend;
            float g = event.getGreen() * inv + 0.0f * blend;
            float b = event.getBlue() * inv + 0.0f * blend;
            event.setRed(r);
            event.setGreen(g);
            event.setBlue(b);
        });
    }

    @SubscribeEvent
    public static void onRenderFog(ViewportEvent.RenderFog event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        mc.player.getCapability(FrequencyCapabilityProvider.FREQUENCY_CAPABILITY).ifPresent(cap -> {
            int level = cap.getLevel();
            if (level <= 0) return;
            float t = Math.min(1.0f, level / 100.0f);
            // Pull far plane slightly closer with level to reduce horizon bleed
            float farFactor = 1.0f - 0.25f * t; // 1.0 .. 0.75
            float near = Math.max(0.15f, 0.35f + 1.2f * t); // 0.35 .. 1.55
            event.setNearPlaneDistance(near);
            event.setFarPlaneDistance(event.getFarPlaneDistance() * farFactor);
            event.setCanceled(false);
        });
    }
} 