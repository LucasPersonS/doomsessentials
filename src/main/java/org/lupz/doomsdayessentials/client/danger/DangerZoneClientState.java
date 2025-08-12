package org.lupz.doomsdayessentials.client.danger;

public class DangerZoneClientState {
    private static boolean active = false;
    private static String timeText = "";
    private static long endTimeMs = 0;

    public static void show(String time, int durationTicks){
        active = true;
        timeText = time;
        endTimeMs = System.currentTimeMillis() + durationTicks*50L;
    }
    public static void hide(){ active=false; }
    public static boolean isActive(){
        if(active && System.currentTimeMillis()>endTimeMs) active=false;
        return active;
    }
    public static String getTimeText(){ return timeText; }
} 