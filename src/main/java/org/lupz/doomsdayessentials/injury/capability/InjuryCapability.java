package org.lupz.doomsdayessentials.injury.capability;

import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.capabilities.AutoRegisterCapability;

@AutoRegisterCapability
public interface InjuryCapability {
   int getInjuryLevel();

   void setInjuryLevel(int level);

   int incrementInjuryLevel();

   int decrementInjuryLevel(int amount);

   int getHealCooldown();

   void setHealCooldown(int cooldown);

   CompoundTag serializeNBT();

   void deserializeNBT(CompoundTag nbt);

   boolean isHealing();

   void setHealing(boolean healing);

   float getHealingProgress();

   void setHealingProgress(float progress);

   int getDeathCount();

   void setDeathCount(int count);

   int incrementDeathCount();
} 