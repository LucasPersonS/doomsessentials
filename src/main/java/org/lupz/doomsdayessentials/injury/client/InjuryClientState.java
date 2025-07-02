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
} 