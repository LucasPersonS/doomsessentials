package org.lupz.doomsdayessentials.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.network.NetworkHooks;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.List;
import java.util.UUID;

/**
 * Rastreador's Hunt Trap entity - places a bear trap on the ground
 */
public class TrapEntity extends Entity implements GeoEntity {
    
    private static final EntityDataAccessor<Integer> LIFETIME = SynchedEntityData.defineId(TrapEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<String> OWNER_UUID = SynchedEntityData.defineId(TrapEntity.class, EntityDataSerializers.STRING);
    
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private static final RawAnimation IDLE_ANIM = RawAnimation.begin().thenLoop("animation.trap.idle");
    private static final RawAnimation TRIGGER_ANIM = RawAnimation.begin().thenPlay("animation.trap.trigger");
    
    private boolean triggered = false;
    
    public TrapEntity(EntityType<?> type, Level level) {
        super(type, level);
        this.setNoGravity(false);
        this.noPhysics = false;
    }
    
    @Override
    protected void defineSynchedData() {
        this.entityData.define(LIFETIME, 600); // 30 seconds = 600 ticks
        this.entityData.define(OWNER_UUID, "");
    }
    
    public void setOwnerUUID(UUID uuid) {
        this.entityData.set(OWNER_UUID, uuid.toString());
    }
    
    public UUID getOwnerUUID() {
        String uuidStr = this.entityData.get(OWNER_UUID);
        if (uuidStr.isEmpty()) return null;
        try {
            return UUID.fromString(uuidStr);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
    
    public void setLifetime(int ticks) {
        this.entityData.set(LIFETIME, ticks);
    }
    
    public int getLifetime() {
        return this.entityData.get(LIFETIME);
    }
    
    @Override
    public void tick() {
        super.tick();
        
        if (this.level().isClientSide) return;
        
        // Count down lifetime
        int life = getLifetime();
        if (life <= 0) {
            this.discard();
            return;
        }
        setLifetime(life - 1);
        
        // Check for players standing on trap
        if (!triggered) {
            checkTrapTrigger();
        }
    }
    
    private void checkTrapTrigger() {
        // Check 1 block around the trap
        AABB trapArea = this.getBoundingBox().inflate(1.0, 0.5, 1.0);
        List<LivingEntity> nearbyEntities = this.level().getEntitiesOfClass(LivingEntity.class, trapArea);
        
        UUID ownerUUID = getOwnerUUID();
        
        for (LivingEntity target : nearbyEntities) {
            // Don't trap the owner
            if (ownerUUID != null && target.getUUID().equals(ownerUUID)) continue;
            
            // Trigger trap!
            triggered = true;
            
            // Play trap closing sound at trap location
            this.level().playSound(
                null, 
                this.getX(), this.getY(), this.getZ(),
                net.minecraft.sounds.SoundEvents.IRON_TRAPDOOR_CLOSE,
                net.minecraft.sounds.SoundSource.HOSTILE,
                1.5F, 
                0.8F
            );
            
            // Deal 4 hearts (8.0) of TRUE DAMAGE (bypasses armor)
            target.hurt(this.damageSources().magic(), 8.0F);
            
            // Apply Slowness V for 5 seconds
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 4, false, true, true)); // 5s Slowness V
            
            // Notify if victim is a player
            if (target instanceof ServerPlayer player) {
                player.sendSystemMessage(net.minecraft.network.chat.Component.literal("§c§l⚠ Você caiu em uma Rede de Caça!"));
            }
            
            // Notify owner (Rastreador)
            if (ownerUUID != null) {
                ServerPlayer owner = (ServerPlayer) this.level().getPlayerByUUID(ownerUUID);
                if (owner != null) {
                    owner.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                        "§a§l✓ Sua armadilha capturou §e" + target.getName().getString() + "§a!"
                    ));
                }
            }
            
            // Remove trap after 2 seconds (40 ticks) to show trigger animation
            setLifetime(40);
            break;
        }
    }
    
    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.contains("Lifetime")) {
            setLifetime(tag.getInt("Lifetime"));
        }
        if (tag.contains("OwnerUUID")) {
            this.entityData.set(OWNER_UUID, tag.getString("OwnerUUID"));
        }
        if (tag.contains("Triggered")) {
            triggered = tag.getBoolean("Triggered");
        }
    }
    
    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("Lifetime", getLifetime());
        String ownerUUID = this.entityData.get(OWNER_UUID);
        if (!ownerUUID.isEmpty()) {
            tag.putString("OwnerUUID", ownerUUID);
        }
        tag.putBoolean("Triggered", triggered);
    }
    
    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
    
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 0, this::predicate));
    }
    
    private PlayState predicate(AnimationState<TrapEntity> state) {
        if (triggered) {
            state.getController().setAnimation(TRIGGER_ANIM);
        } else {
            state.getController().setAnimation(IDLE_ANIM);
        }
        return PlayState.CONTINUE;
    }
    
    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
    
    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (this.isInvulnerableTo(source)) {
            return false;
        }
        
        if (!this.level().isClientSide && !this.isRemoved()) {
            // Play break sound
            this.level().playSound(
                null,
                this.getX(), this.getY(), this.getZ(),
                net.minecraft.sounds.SoundEvents.METAL_BREAK,
                net.minecraft.sounds.SoundSource.BLOCKS,
                1.0F,
                1.0F
            );
            
            // Destroy the trap
            this.discard();
            return true;
        }
        
        return false;
    }
    
    @Override
    public void push(double x, double y, double z) {
        // No knockback - trap stays in place
    }
    
    @Override
    public boolean isPushable() {
        return false;
    }
    
    @Override
    public boolean canBeCollidedWith() {
        return !this.isRemoved();
    }
}
