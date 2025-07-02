package org.lupz.doomsdayessentials.injury.capability;

import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.capabilities.AutoRegisterCapability;

@AutoRegisterCapability
public interface InjuryCapability {
   int getInjuryLevel();

   void setInjuryLevel(int level);

   int incrementInjuryLevel();

   int decrementInjuryLevel(int amount);

   CompoundTag serializeNBT();

   void deserializeNBT(CompoundTag nbt);

   float getHealingProgress();

   void setHealingProgress(float progress);

   int getDeathCount();

   void setDeathCount(int count);

   int incrementDeathCount();

   boolean isDowned();

   void setDowned(boolean downed, @javax.annotation.Nullable java.util.UUID lastAttacker);

   float getDownedHealth();

   void setDownedHealth(float health);

   long getDownedUntil();

   void setDownedUntil(long time);

   java.util.UUID getLastAttacker();

   void setLastAttacker(java.util.UUID attacker);
} 