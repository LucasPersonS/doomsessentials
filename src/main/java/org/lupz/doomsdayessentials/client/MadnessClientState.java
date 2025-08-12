package org.lupz.doomsdayessentials.client;

public class MadnessClientState {

    private static int ticksRemaining = 0;
    private static float shakeIntensity = 0.0f;
    private static float overlayIntensity = 0.0f;

    private MadnessClientState() {}

    public static void activate(int durationTicks, float shake, float overlay) {
        ticksRemaining = durationTicks;
        shakeIntensity = shake;
        overlayIntensity = overlay;
    }

    public static void tick() {
        if (ticksRemaining > 0) {
            ticksRemaining--;
            if (ticksRemaining == 0) {
                shakeIntensity = 0.0f;
                overlayIntensity = 0.0f;
            }
        }
    }

    public static boolean isActive() {
        return ticksRemaining > 0;
    }

    public static float getShakeIntensity() {
        return shakeIntensity;
    }

    public static float getOverlayIntensity() {
        return overlayIntensity;
    }
} 