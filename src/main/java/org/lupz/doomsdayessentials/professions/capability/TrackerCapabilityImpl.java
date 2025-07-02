package org.lupz.doomsdayessentials.professions.capability;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class TrackerCapabilityImpl implements TrackerCapability {
    private final List<UUID> whitelist = new ArrayList<>();

    @Override
    public List<UUID> getWhitelist() {
        return whitelist;
    }

    @Override
    public void addToWhitelist(UUID player) {
        if (!whitelist.contains(player)) {
            whitelist.add(player);
        }
    }

    @Override
    public void removeFromWhitelist(UUID player) {
        whitelist.remove(player);
    }

    @Override
    public boolean isWhitelisted(UUID player) {
        return whitelist.contains(player);
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        ListTag listTag = new ListTag();
        for (UUID uuid : whitelist) {
            listTag.add(StringTag.valueOf(uuid.toString()));
        }
        tag.put("whitelist", listTag);
        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        whitelist.clear();
        ListTag listTag = nbt.getList("whitelist", 8); // 8 is the NBT type for String
        for (int i = 0; i < listTag.size(); i++) {
            whitelist.add(UUID.fromString(listTag.getString(i)));
        }
    }
} 