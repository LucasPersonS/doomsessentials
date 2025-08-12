package org.lupz.doomsdayessentials.entity;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.particles.ParticleTypes;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.util.GeckoLibUtil;
import software.bernie.geckolib.core.animation.AnimatableManager.ControllerRegistrar;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import org.lupz.doomsdayessentials.sound.ModSounds;
import net.minecraft.sounds.SoundSource;

public class FacelessEntity extends Monster implements GeoEntity {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    /**
     * Counts how many consecutive server ticks the nearest player has been
     * looking directly at this Faceless.  Once it reaches the threshold,
     * the entity will vanish in a puff of smoke.  This prevents the mob from
     * disappearing instantly on spawn when the player may already be looking
     * in its general direction.
     */
    private int gazeTicks;
    private int lifeTicks;

    public FacelessEntity(EntityType<? extends Monster> type, Level level){
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes(){
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 20.0)
                .add(Attributes.MOVEMENT_SPEED, 0.25)
                .add(Attributes.FOLLOW_RANGE, 32.0);
    }

    @Override
    protected void registerGoals(){
        this.goalSelector.addGoal(0, new LookAtPlayerGoal(this, Player.class, 8f));
    }

    @Override
    public void aiStep(){
        super.aiStep();
        lifeTicks++;
        if(level().isClientSide) return;
        Player p = level().getNearestPlayer(this, 32);
        if (p != null && this.hasLineOfSight(p)) {
            Vec3 look = p.getLookAngle().normalize();
            Vec3 dir = this.getEyePosition().vectorTo(p.getEyePosition()).normalize();

            // dot == -1 when player is looking straight at the entity.
            // Use -0.9 so slight offset still counts.
            if (look.dot(dir) < -0.9) {
                gazeTicks++;
                if (gazeTicks >= 10 && lifeTicks > 40) { // wait at least 2s after spawn
                    ((ServerLevel) level()).sendParticles(ParticleTypes.SMOKE, getX(), getY() + 1, getZ(), 20, .2, .5, .2, .02);
                    // Play vanish sound at entity position
                    ((ServerLevel) level()).playSound(null, getX(), getY(), getZ(), ModSounds.VANISH.get(), SoundSource.HOSTILE, 1.0f, 1.0f);
                    this.discard();
                }
            } else {
                gazeTicks = 0; // player looked away; reset counter
            }
        } else {
            gazeTicks = 0;
        }
    }

    // ---------------- Geckolib ----------------
    @Override
    public void registerControllers(ControllerRegistrar registrar){
        // No animations defined yet.
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache(){
        return cache;
    }
} 