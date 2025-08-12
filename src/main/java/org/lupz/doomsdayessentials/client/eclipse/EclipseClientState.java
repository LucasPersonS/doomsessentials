package org.lupz.doomsdayessentials.client.eclipse;

public final class EclipseClientState {
    private static boolean active;
    private static float fogNear;
    private static float fogFar;
    private static float overlayAlpha;

    private EclipseClientState() {}

    public static void activate(float near, float far, float alpha){
        active = true;
        fogNear = Math.max(0.0f, near);
        fogFar = Math.max(0.1f, far);
        overlayAlpha = Math.max(0.0f, Math.min(1.0f, alpha));
    }

    public static void deactivate(){
        active = false;
    }

    public static boolean isActive(){ return active; }
    public static float getFogNear(){ return fogNear; }
    public static float getFogFar(){ return fogFar; }
    public static float getOverlayAlpha(){ return overlayAlpha; }
} 