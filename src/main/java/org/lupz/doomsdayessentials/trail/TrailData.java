package org.lupz.doomsdayessentials.trail;

import net.minecraft.core.particles.ParticleOptions;

public class TrailData {
    private final ParticleOptions particle;
    private final float dx;
    private final float dy;
    private final float dz;
    private final float speed;
    private final int count;

    public TrailData(ParticleOptions particle, float dx, float dy, float dz, float speed, int count) {
        this.particle = particle;
        this.dx = dx;
        this.dy = dy;
        this.dz = dz;
        this.speed = speed;
        this.count = count;
    }

    // Getters for all fields
    public ParticleOptions getParticle() { return particle; }
    public float getDx() { return dx; }
    public float getDy() { return dy; }
    public float getDz() { return dz; }
    public float getSpeed() { return speed; }
    public int getCount() { return count; }
} 