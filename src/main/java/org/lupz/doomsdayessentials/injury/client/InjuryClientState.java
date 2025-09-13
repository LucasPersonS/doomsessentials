package org.lupz.doomsdayessentials.injury.client;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(value = Dist.CLIENT)
public class InjuryClientState {
    private static boolean isDowned = false;
    private static long downedUntil = 0L;

    public static boolean isDowned() {
        return isDowned;
    }

    public static long getDownedUntil() {
        return downedUntil;
    }

    public static void setDowned(boolean downed, long until) {
        isDowned = downed;
        downedUntil = until;
    }

    public static void updateDownedState(boolean downed, long until) {
        setDowned(downed, until);
        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
        if (downed) {
            try {
                net.minecraft.resources.ResourceLocation art = net.minecraft.resources.ResourceLocation.parse("minecraft:shaders/post/art.json");
                mc.gameRenderer.loadEffect(art);
            } catch (Exception e) {
                try {
                    net.minecraft.resources.ResourceLocation blur = net.minecraft.resources.ResourceLocation.parse("minecraft:shaders/post/blur.json");
                    mc.gameRenderer.loadEffect(blur);
                } catch (Exception ignored) {}
            }
            // Do not open any Screen; a HUD overlay will render the "Desistir" action
            if (mc.screen instanceof org.lupz.doomsdayessentials.injury.client.DownedScreen) {
                mc.setScreen(null);
            }
        } else {
            try {
                mc.gameRenderer.shutdownEffect();
            } catch (Exception ignored) {}
            if (mc.screen instanceof org.lupz.doomsdayessentials.injury.client.DownedScreen) {
                mc.setScreen(null);
            }
        }
    }
} 