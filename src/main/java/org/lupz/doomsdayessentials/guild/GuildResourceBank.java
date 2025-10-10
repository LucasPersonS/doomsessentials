package org.lupz.doomsdayessentials.guild;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;

import java.util.HashMap;
import java.util.Map;

/**
 * Persistent per-world storage for guild resource balances (e.g., scrapmetal).
 * Stores a mapping of guildName -> (resourceId -> amount).
 */
public class GuildResourceBank extends SavedData {
    private static final String DATA_NAME = "guild_resources";

    private final Map<String, Map<String, Integer>> balances = new HashMap<>();

    public GuildResourceBank() {}

    private GuildResourceBank(CompoundTag tag) {
        read(tag);
    }

    public static GuildResourceBank load(CompoundTag tag) { return new GuildResourceBank(tag); }

    public static GuildResourceBank get(ServerLevel level) {
        DimensionDataStorage storage = level.getDataStorage();
        return storage.computeIfAbsent(GuildResourceBank::load, GuildResourceBank::new, DATA_NAME);
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        CompoundTag root = new CompoundTag();
        for (var e : balances.entrySet()) {
            CompoundTag inner = new CompoundTag();
            for (var r : e.getValue().entrySet()) inner.putInt(r.getKey(), r.getValue());
            root.put(e.getKey(), inner);
        }
        tag.put("balances", root);
        return tag;
    }

    private void read(CompoundTag tag) {
        balances.clear();
        if (!tag.contains("balances", Tag.TAG_COMPOUND)) return;
        CompoundTag root = tag.getCompound("balances");
        for (String gname : root.getAllKeys()) {
            CompoundTag inner = root.getCompound(gname);
            Map<String, Integer> map = new HashMap<>();
            for (String k : inner.getAllKeys()) map.put(k, inner.getInt(k));
            balances.put(gname, map);
        }
    }

    public int get(String guildName, String resourceId) {
        if (guildName == null || resourceId == null) return 0;
        Map<String, Integer> map = balances.get(guildName);
        return map != null ? map.getOrDefault(resourceId, 0) : 0;
    }

    public void add(String guildName, String resourceId, int amount) {
        if (guildName == null || resourceId == null || amount <= 0) return;
        Map<String, Integer> map = balances.computeIfAbsent(guildName, k -> new HashMap<>());
        map.put(resourceId, map.getOrDefault(resourceId, 0) + amount);
        setDirty();
    }

    public boolean consume(String guildName, String resourceId, int amount) {
        if (guildName == null || resourceId == null || amount <= 0) return false;
        Map<String, Integer> map = balances.get(guildName);
        if (map == null) return false;
        int have = map.getOrDefault(resourceId, 0);
        if (have < amount) return false;
        int left = have - amount;
        if (left > 0) map.put(resourceId, left); else map.remove(resourceId);
        setDirty();
        return true;
    }

    public boolean isEmpty(String guildName) {
        Map<String, Integer> map = balances.get(guildName);
        if (map == null || map.isEmpty()) return true;
        for (int v : map.values()) if (v > 0) return false;
        return true;
    }

    public void removeGuild(String guildName) {
        if (guildName == null) return;
        if (balances.remove(guildName) != null) setDirty();
    }
}
