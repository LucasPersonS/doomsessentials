package org.lupz.doomsdayessentials.injury.capability;

import net.minecraft.nbt.CompoundTag;

public class InjuryCapabilityImpl implements InjuryCapability {
   private int injuryLevel = 0;
   private int healCooldown = 0;
   private boolean healing = false;
   private float healingProgress = 0.0F;
   private int deathCount = 0;

   public InjuryCapabilityImpl() {
   }

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

   public int getHealCooldown() {
      return this.healCooldown;
   }

   public void setHealCooldown(int cooldown) {
      this.healCooldown = Math.max(0, cooldown);
   }

   public boolean isHealing() {
      return this.healing;
   }

   public void setHealing(boolean healing) {
      this.healing = healing;
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

   public CompoundTag serializeNBT() {
      CompoundTag tag = new CompoundTag();
      tag.putInt("InjuryLevel", this.injuryLevel);
      tag.putInt("HealCooldown", this.healCooldown);
      tag.putBoolean("Healing", this.healing);
      tag.putFloat("HealingProgress", this.healingProgress);
      tag.putInt("DeathCount", this.deathCount);
      return tag;
   }

   public void deserializeNBT(CompoundTag nbt) {
      this.injuryLevel = nbt.getInt("InjuryLevel");
      this.healCooldown = nbt.getInt("HealCooldown");
      this.healing = nbt.getBoolean("Healing");
      this.healingProgress = nbt.getFloat("HealingProgress");
      this.deathCount = nbt.getInt("DeathCount");
   }
} 