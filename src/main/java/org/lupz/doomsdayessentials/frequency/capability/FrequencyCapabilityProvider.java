package org.lupz.doomsdayessentials.frequency.capability;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lupz.doomsdayessentials.EssentialsMod;

public class FrequencyCapabilityProvider implements ICapabilityProvider, INBTSerializable<CompoundTag> {

    public static final Capability<FrequencyCapability> FREQUENCY_CAPABILITY = CapabilityManager.get(new CapabilityToken<>() {});
    public static final ResourceLocation FREQUENCY_CAPABILITY_ID = ResourceLocation.fromNamespaceAndPath(EssentialsMod.MOD_ID, "frequency");

    private FrequencyCapability impl;
    private final LazyOptional<FrequencyCapability> optional = LazyOptional.of(this::create);

    private FrequencyCapability create() {
        if (this.impl == null) {
            this.impl = new FrequencyCapabilityImpl();
        }
        return this.impl;
    }

    @NotNull
    @Override
    public <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == FREQUENCY_CAPABILITY) {
            return optional.cast();
        }
        return LazyOptional.empty();
    }

    @Override
    public CompoundTag serializeNBT() {
        return create().serializeNBT();
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        create().deserializeNBT(nbt);
    }
} 