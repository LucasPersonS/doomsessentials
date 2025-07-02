package org.lupz.doomsdayessentials.professions.capability;

import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.capabilities.AutoRegisterCapability;

import java.util.List;
import java.util.UUID;

@AutoRegisterCapability
public interface TrackerCapability {
    List<UUID> getWhitelist();
    void addToWhitelist(UUID player);
    void removeFromWhitelist(UUID player);
    boolean isWhitelisted(UUID player);

    CompoundTag serializeNBT();
    void deserializeNBT(CompoundTag nbt);
} 