package org.lupz.doomsdayessentials.frequency.capability;

import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.capabilities.AutoRegisterCapability;

@AutoRegisterCapability
public interface FrequencyCapability {
    int getLevel();
    void setLevel(int level);
    default int addLevel(int delta) {
        int next = Math.max(0, Math.min(100, getLevel() + delta));
        setLevel(next);
        return next;
    }

    long getLastServerUpdateMs();
    void setLastServerUpdateMs(long epochMs);

    boolean isSoundsEnabled();
    void setSoundsEnabled(boolean enabled);

    boolean isImagesEnabled();
    void setImagesEnabled(boolean enabled);

    CompoundTag serializeNBT();
    void deserializeNBT(CompoundTag nbt);
} 