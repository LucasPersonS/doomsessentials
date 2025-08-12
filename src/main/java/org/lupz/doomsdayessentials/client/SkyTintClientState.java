package org.lupz.doomsdayessentials.client;

public class SkyTintClientState {
    private static int color = 0; // 0 means disabled
    private static float alpha = 1.0f;

    public static void set(int rgb, float a){
        color = rgb;
        alpha = a;
    }

    public static boolean isActive() {
        return color != 0;
    }

    public static float getRed() { return ((color >> 16) & 0xFF) / 255.0f; }
    public static float getGreen() { return ((color >> 8) & 0xFF) / 255.0f; }
    public static float getBlue() { return (color & 0xFF) / 255.0f; }

    public static float getAlpha(){ return alpha; }
} 