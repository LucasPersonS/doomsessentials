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
        // Safely update the client screen depending on the new state
        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
        if (downed) {
            // Open the Downed screen
            mc.setScreen(new org.lupz.doomsdayessentials.injury.client.DownedScreen());
        } else if (mc.screen instanceof org.lupz.doomsdayessentials.injury.client.DownedScreen) {
            // Close the Downed screen if it was open
            mc.setScreen(null);
        }
    }
} 