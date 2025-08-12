package org.lupz.doomsdayessentials.frequency.capability;

import net.minecraft.nbt.CompoundTag;

public class FrequencyCapabilityImpl implements FrequencyCapability {
    private int level;
    private long lastServerUpdateMs;
    private boolean soundsEnabled = true;
    private boolean imagesEnabled = true;

    @Override
    public int getLevel() {
        return level;
    }

    @Override
    public void setLevel(int level) {
        this.level = Math.max(0, Math.min(100, level));
    }

    @Override
    public long getLastServerUpdateMs() {
        return lastServerUpdateMs;
    }

    @Override
    public void setLastServerUpdateMs(long epochMs) {
        this.lastServerUpdateMs = epochMs;
    }

    @Override
    public boolean isSoundsEnabled() {
        return soundsEnabled;
    }

    @Override
    public void setSoundsEnabled(boolean enabled) {
        this.soundsEnabled = enabled;
    }

    @Override
    public boolean isImagesEnabled() {
        return imagesEnabled;
    }

    @Override
    public void setImagesEnabled(boolean enabled) {
        this.imagesEnabled = enabled;
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("level", level);
        tag.putLong("lastUpdate", lastServerUpdateMs);
        tag.putBoolean("sounds", soundsEnabled);
        tag.putBoolean("images", imagesEnabled);
        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        this.level = Math.max(0, Math.min(100, nbt.getInt("level")));
        this.lastServerUpdateMs = nbt.getLong("lastUpdate");
        if (nbt.contains("sounds")) this.soundsEnabled = nbt.getBoolean("sounds");
        if (nbt.contains("images")) this.imagesEnabled = nbt.getBoolean("images");
    }
} 