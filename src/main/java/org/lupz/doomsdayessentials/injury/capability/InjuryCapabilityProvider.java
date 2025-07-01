package org.lupz.doomsdayessentials.injury.capability;

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

public class InjuryCapabilityProvider implements ICapabilityProvider, INBTSerializable<CompoundTag> {

    public static final Capability<InjuryCapability> INJURY_CAPABILITY = CapabilityManager.get(new CapabilityToken<>() {});
    public static final ResourceLocation INJURY_CAPABILITY_ID = ResourceLocation.fromNamespaceAndPath(EssentialsMod.MOD_ID, "injury");

    private InjuryCapability impl;
    private final LazyOptional<InjuryCapability> optional = LazyOptional.of(this::createInjury);

    private InjuryCapability createInjury() {
        if (this.impl == null) {
            this.impl = new InjuryCapabilityImpl();
        }
        return this.impl;
    }

    @NotNull
    @Override
    public <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == INJURY_CAPABILITY) {
            return optional.cast();
        }
        return LazyOptional.empty();
    }

    @Override
    public CompoundTag serializeNBT() {
        return createInjury().serializeNBT();
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        createInjury().deserializeNBT(nbt);
    }
} 