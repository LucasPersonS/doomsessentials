package org.lupz.doomsdayessentials.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

// THIS IS A DIRECT COPY OF FREQUENCYPARTICLE.JAVA FOR DIAGNOSTIC PURPOSES
public class BlackSmokeParticle extends TextureSheetParticle {
    private final SpriteSet sprites;

    private BlackSmokeParticle(ClientLevel level, double x, double y, double z, double xd, double yd, double zd, SpriteSet spriteSet) {
        super(level, x, y, z, xd, yd, zd);
        this.sprites = spriteSet;

        this.lifetime = 88;
        this.xd = xd * 1.0;
        this.yd = yd * 1.0;
        this.zd = zd * 1.0;
        this.friction = 1.0f - (4.0f / 20.0f);
        this.hasPhysics = true;
        this.gravity = 0.0f;
        this.quadSize = 0.0f;
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
            // Using Frequency's color/size for the test
            updateEffect();
            this.move(this.xd, this.yd, this.zd);
            this.xd *= (double)this.friction;
            this.yd *= (double)this.friction;
            this.zd *= (double)this.friction;
        }
    }

    private void updateEffect() {
        float ageRatio = (float)this.age / (float)this.lifetime;
        this.quadSize = 0.12f * (float)(Math.sin(ageRatio * Math.PI));
        
        float r1=0.843f, g1=0.109f, b1=0.109f;
        float r2=0.435f, g2=0.015f, b2=0.015f;
        if (ageRatio < 0.16f) {
            float segmentRatio = ageRatio / 0.16f;
            this.rCol = r1 + (r2 - r1) * segmentRatio;
            this.gCol = g1 + (g2 - g1) * segmentRatio;
            this.bCol = b1 + (b2 - b1) * segmentRatio;
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
            return new BlackSmokeParticle(level, x, y, z, dx, dy, dz, this.sprites);
        }
    }
} 