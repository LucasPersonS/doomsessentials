package org.lupz.doomsdayessentials.professions.capability;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import net.minecraft.resources.ResourceLocation;

public class TrackerCapabilityProvider implements ICapabilityProvider, INBTSerializable<CompoundTag> {
    public static final Capability<TrackerCapability> TRACKER_CAPABILITY = CapabilityManager.get(new CapabilityToken<>() {});
    public static final ResourceLocation TRACKER_CAPABILITY_ID = ResourceLocation.fromNamespaceAndPath("doomsdayessentials", "tracker");

    private TrackerCapability capability;
    private final LazyOptional<TrackerCapability> optional = LazyOptional.of(this::getOrCreate);

    @NotNull
    @Override
    public <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        return cap == TRACKER_CAPABILITY ? optional.cast() : LazyOptional.empty();
    }

    private TrackerCapability getOrCreate() {
        if (capability == null) {
            capability = new TrackerCapabilityImpl();
        }
        return capability;
    }

    @Override
    public CompoundTag serializeNBT() {
        return getOrCreate().serializeNBT();
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        getOrCreate().deserializeNBT(nbt);
    }
} 