package org.lupz.doomsdayessentials.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

public class FrequencyParticle extends TextureSheetParticle {
    private final SpriteSet sprites;

    private FrequencyParticle(ClientLevel level, double x, double y, double z, double xd, double yd, double zd, SpriteSet spriteSet) {
        super(level, x, y, z, xd, yd, zd);
        this.sprites = spriteSet;

        // --- From JSON Description ---
        // "particle_lifetime_expression": { "max_lifetime": 4.4 } -> 4.4s * 20 ticks/s = 88 ticks
        this.lifetime = 88;
        // "minecraft:particle_initial_speed": 1
        this.xd = xd * 1.0;
        this.yd = yd * 1.0;
        this.zd = zd * 1.0;
        // "minecraft:particle_motion_dynamic": { "linear_drag_coefficient": 4 }
        this.friction = 1.0f - (4.0f / 20.0f); // Approximate conversion
        // "minecraft:particle_motion_collision": { "collision_drag": 0.4, "coefficient_of_restitution": 1, "collision_radius": 0.4 }
        this.hasPhysics = true;
        this.gravity = 0.0f; // No gravity by default in the json
        // --- End of JSON Description ---

        this.quadSize = 0.0f; // Initialize size
        this.setSpriteFromAge(spriteSet);
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;

        if (this.age++ >= this.lifetime) {
            this.remove();
        } else {
            this.setSpriteFromAge(this.sprites);
            this.updateColor();
            this.updateSize();

            this.move(this.xd, this.yd, this.zd);
            this.xd *= (double)this.friction;
            this.yd *= (double)this.friction;
            this.zd *= (double)this.friction;
        }
    }

    private void updateSize() {
        float ageRatio = (float)this.age / (float)this.lifetime;
        // "variable.psize" catmull_rom curve [0, 0, 1, 0, 0]
        // This creates a curve that goes from 0 up to a peak at ~25% and then back down to 0.
        this.quadSize = 0.12f * (float)(Math.sin(ageRatio * Math.PI));
    }

    private void updateColor() {
        float ageRatio = (float)this.age / (float)this.lifetime;

        // Color gradient from JSON, converted from HEX to RGB floats (0.0-1.0)
        float r1=0.843f, g1=0.109f, b1=0.109f; // #ffd71c1c -> D71C1C
        float r2=0.435f, g2=0.015f, b2=0.015f; // #FF6F0404 -> 6F0404
        float r3=1.000f, g3=0.000f, b3=0.454f; // #FFFF0074 -> FF0074
        float r4=0.376f, g4=0.011f, b4=0.011f; // #FF600303 -> 600303
        float r5=0.352f, g5=0.019f, b5=0.027f; // #FF5A0507 -> 5A0507
        float r6=0.423f, g6=0.003f, b6=0.035f; // #FF6C0109 -> 6C0109
        float r7=0.886f, g7=0.066f, b7=0.066f; // #ffe21111 -> E21111

        if (ageRatio < 0.16f) { // 0.0 to 0.16
            float segmentRatio = ageRatio / 0.16f;
            this.rCol = r1 + (r2 - r1) * segmentRatio;
            this.gCol = g1 + (g2 - g1) * segmentRatio;
            this.bCol = b1 + (b2 - b1) * segmentRatio;
        } else if (ageRatio < 0.33f) { // 0.16 to 0.33
            float segmentRatio = (ageRatio - 0.16f) / (0.33f - 0.16f);
            this.rCol = r2 + (r3 - r2) * segmentRatio;
            this.gCol = g2 + (g3 - g2) * segmentRatio;
            this.bCol = b2 + (b3 - b2) * segmentRatio;
        } else if (ageRatio < 0.5f) { // 0.33 to 0.5
            float segmentRatio = (ageRatio - 0.33f) / (0.5f - 0.33f);
            this.rCol = r3 + (r4 - r3) * segmentRatio;
            this.gCol = g3 + (g4 - g3) * segmentRatio;
            this.bCol = b3 + (b4 - b3) * segmentRatio;
        } else if (ageRatio < 0.67f) { // 0.5 to 0.67
            float segmentRatio = (ageRatio - 0.5f) / (0.67f - 0.5f);
            this.rCol = r4 + (r5 - r4) * segmentRatio;
            this.gCol = g4 + (g5 - g4) * segmentRatio;
            this.bCol = b4 + (b5 - b4) * segmentRatio;
        } else if (ageRatio < 0.83f) { // 0.67 to 0.83
            float segmentRatio = (ageRatio - 0.67f) / (0.83f - 0.67f);
            this.rCol = r5 + (r6 - r5) * segmentRatio;
            this.gCol = g5 + (g6 - g5) * segmentRatio;
            this.bCol = b5 + (b6 - b5) * segmentRatio;
        } else { // 0.83 to 1.0
            float segmentRatio = (ageRatio - 0.83f) / (1.0f - 0.83f);
            this.rCol = r6 + (r7 - r6) * segmentRatio;
            this.gCol = g6 + (g7 - g6) * segmentRatio;
            this.bCol = b6 + (b7 - b6) * segmentRatio;
        }
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    @OnlyIn(Dist.CLIENT)
    public static class Provider implements ParticleProvider<net.minecraft.core.particles.SimpleParticleType> {
        private final SpriteSet sprites;

        public Provider(SpriteSet spriteSet) {
            this.sprites = spriteSet;
        }

        public Particle createParticle(net.minecraft.core.particles.SimpleParticleType particleType, ClientLevel level,
                                       double x, double y, double z,
                                       double dx, double dy, double dz) {
            return new FrequencyParticle(level, x, y, z, dx, dy, dz, this.sprites);
        }
    }
} 