package org.lupz.doomsdayessentials.injury.capability;

import net.minecraft.nbt.CompoundTag;
import org.lupz.doomsdayessentials.config.EssentialsConfig;

public class InjuryCapabilityImpl implements InjuryCapability {
   private int injuryLevel = 0;
   private int deathCount = 0;
   private float healingProgress = 0.0f;
   private boolean downed = false;
   private float downedHealth = 20.0f;
   private long downedUntil = 0L;
   private java.util.UUID lastAttacker = null;

   public InjuryCapabilityImpl() {
   }

   @Override
   public int getInjuryLevel() {
      return this.injuryLevel;
   }

   public void setInjuryLevel(int level) {
      this.injuryLevel = Math.max(0, Math.min(5, level));
   }

   public int incrementInjuryLevel() {
      if (this.injuryLevel < 5) {
         ++this.injuryLevel;
      }

      return this.injuryLevel;
   }

   public int decrementInjuryLevel(int amount) {
      this.injuryLevel = Math.max(0, this.injuryLevel - amount);
      return this.injuryLevel;
   }

   public float getHealingProgress() {
      return this.healingProgress;
   }

   public void setHealingProgress(float progress) {
      this.healingProgress = progress;
   }

   public int getDeathCount() {
      return this.deathCount;
   }

   public void setDeathCount(int count) {
      this.deathCount = count;
   }

   public int incrementDeathCount() {
      ++this.deathCount;
      return this.deathCount;
   }

   public boolean isDowned() {
      return this.downed;
   }

   public void setDowned(boolean downed, @javax.annotation.Nullable java.util.UUID lastAttacker) {
      this.downed = downed;
      this.lastAttacker = lastAttacker;
   }

   public float getDownedHealth() {
      return this.downedHealth;
   }

   public void setDownedHealth(float health) {
      this.downedHealth = health;
   }

   @Override
   public long getDownedUntil() {
      return this.downedUntil;
   }

   @Override
   public void setDownedUntil(long time) {
      this.downedUntil = time;
   }

   @Override
   public java.util.UUID getLastAttacker() {
      return this.lastAttacker;
   }

   @Override
   public void setLastAttacker(java.util.UUID attacker) {
      this.lastAttacker = attacker;
   }

   public CompoundTag serializeNBT() {
      CompoundTag tag = new CompoundTag();
      tag.putInt("InjuryLevel", this.injuryLevel);
      tag.putFloat("HealingProgress", this.healingProgress);
      tag.putInt("DeathCount", this.deathCount);
      tag.putBoolean("Downed", this.downed);
      tag.putFloat("DownedHealth", this.downedHealth);
      tag.putLong("DownedUntil", this.downedUntil);
      if (this.lastAttacker != null) {
         tag.putUUID("LastAttacker", this.lastAttacker);
      }
      return tag;
   }

   public void deserializeNBT(CompoundTag nbt) {
      this.injuryLevel = nbt.getInt("InjuryLevel");
      this.healingProgress = nbt.getFloat("HealingProgress");
      this.deathCount = nbt.getInt("DeathCount");
      this.downed = nbt.getBoolean("Downed");
      this.downedHealth = nbt.getFloat("DownedHealth");
      this.downedUntil = nbt.getLong("DownedUntil");
      if (nbt.hasUUID("LastAttacker")) {
         this.lastAttacker = nbt.getUUID("LastAttacker");
      } else {
         this.lastAttacker = null;
      }
   }
} 