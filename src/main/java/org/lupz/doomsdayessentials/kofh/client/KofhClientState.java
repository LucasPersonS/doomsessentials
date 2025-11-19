package org.lupz.doomsdayessentials.kofh.client;

import org.lupz.doomsdayessentials.network.packet.s2c.KofhScorePacket;

import java.util.ArrayList;
import java.util.List;

/**
 * Client-side state holder for the KOFH HUD.
 */
public final class KofhClientState {
    private static String areaName = "";
    private static int remainingSeconds = 0;
    private static String controllingGuild = null;
    private static boolean contested = false;
    private static double multiplier = 1.0;
    private static final List<KofhScorePacket.Entry> entries = new ArrayList<>();
    private static String dimensionId = "";
    private static double centerX = 0, centerY = 0, centerZ = 0;

    private static long lastUpdateMs = 0L;

    private KofhClientState() {}

    public static void update(KofhScorePacket pkt) {
        areaName = pkt.areaName;
        remainingSeconds = pkt.remainingSeconds;
        controllingGuild = pkt.controllingGuild;
        contested = pkt.contested;
        multiplier = pkt.multiplier;
        entries.clear();
        entries.addAll(pkt.entries);
        dimensionId = pkt.dimensionId;
        centerX = pkt.centerX; centerY = pkt.centerY; centerZ = pkt.centerZ;
        lastUpdateMs = System.currentTimeMillis();
    }

    public static String getAreaName(){return areaName;}
    public static int getRemainingSeconds(){return remainingSeconds;}
    public static String getControllingGuild(){return controllingGuild;}
    public static boolean isContested(){return contested;}
    public static double getMultiplier(){return multiplier;}
    public static List<KofhScorePacket.Entry> getEntries(){return java.util.Collections.unmodifiableList(entries);}    
    public static long getLastUpdateMs(){return lastUpdateMs;}
    public static String getDimensionId(){return dimensionId;}
    public static double getCenterX(){return centerX;}
    public static double getCenterY(){return centerY;}
    public static double getCenterZ(){return centerZ;}
}
