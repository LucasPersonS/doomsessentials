package org.lupz.doomsdayessentials.event.eclipse;

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

public class EclipseScoreCapabilityProvider implements ICapabilityProvider, INBTSerializable<CompoundTag> {
    public static final Capability<EclipseScoreCapability> SCORE_CAPABILITY = CapabilityManager.get(new CapabilityToken<>() {});
    public static final ResourceLocation SCORE_CAPABILITY_ID = ResourceLocation.fromNamespaceAndPath(EssentialsMod.MOD_ID, "eclipse_score");

    private EclipseScoreCapability impl;
    private final LazyOptional<EclipseScoreCapability> optional = LazyOptional.of(this::getOrCreate);

    private EclipseScoreCapability getOrCreate(){ if (impl == null) impl = new EclipseScoreCapabilityImpl(); return impl; }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        return cap == SCORE_CAPABILITY ? optional.cast() : LazyOptional.empty();
    }

    @Override
    public CompoundTag serializeNBT() { return getOrCreate().serializeNBT(); }

    @Override
    public void deserializeNBT(CompoundTag nbt) { getOrCreate().deserializeNBT(nbt); }
} 