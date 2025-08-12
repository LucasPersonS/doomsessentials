package org.lupz.doomsdayessentials.event.eclipse;

import net.minecraft.nbt.CompoundTag;

public interface EclipseScoreCapability {
    int getKills();
    int getDeaths();
    void addKill();
    void addDeath();

    boolean isPermaDead();
    void setPermaDead(boolean dead);

    default int getScore(){ return getKills() - getDeaths(); }

    CompoundTag serializeNBT();
    void deserializeNBT(CompoundTag nbt);
} 