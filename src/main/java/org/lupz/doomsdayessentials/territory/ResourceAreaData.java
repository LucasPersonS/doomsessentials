package org.lupz.doomsdayessentials.territory;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

public class ResourceAreaData {
    public static class LootEntry {
        public String id;
        public int perHour;
        public int stored;

        public LootEntry(String id, int perHour) {
            this.id = id; this.perHour = perHour; this.stored = 0;
        }

        public JsonObject toJson() {
            JsonObject o = new JsonObject();
            o.addProperty("id", id);
            o.addProperty("perHour", perHour);
            o.addProperty("stored", stored);
            return o;
        }

        public static LootEntry fromJson(JsonObject o) {
            LootEntry e = new LootEntry(o.get("id").getAsString(), o.get("perHour").getAsInt());
            if (o.has("stored")) e.stored = o.get("stored").getAsInt();
            return e;
        }
    }

    public String areaName;
    public List<LootEntry> lootEntries = new ArrayList<>();
    public int storageCap;

    public String ownerGuild; // nullable
    public long lastTimestamp;
    public long claimTimestamp;

    public ResourceAreaData(String areaName, String lootId, int itemsPerHour, int storageCap) {
        this.areaName = areaName;
        this.storageCap = storageCap;
        this.lootEntries.add(new LootEntry(lootId, itemsPerHour));
        this.lastTimestamp = System.currentTimeMillis();
        this.claimTimestamp = 0;
    }

    public ResourceAreaData(String areaName, List<LootEntry> entries, int storageCap) {
        this.areaName = areaName;
        this.lootEntries.addAll(entries);
        this.storageCap = storageCap;
        this.lastTimestamp = System.currentTimeMillis();
        this.claimTimestamp = 0;
    }

    // ----------------------------------
    // Serialization helpers
    // ----------------------------------
    public JsonObject toJson() {
        JsonObject obj = new JsonObject();
        JsonArray arr = new JsonArray();
        for (LootEntry e : lootEntries) arr.add(e.toJson());
        obj.add("items", arr);
        obj.addProperty("storageCap", storageCap);
        if (ownerGuild != null) obj.addProperty("owner", ownerGuild);
        if (claimTimestamp > 0) obj.addProperty("claim", claimTimestamp);
        obj.addProperty("last", lastTimestamp);
        return obj;
    }

    public static ResourceAreaData fromJson(String name, JsonObject obj) {
        int cap = obj.has("storageCap") ? obj.get("storageCap").getAsInt() : 64;
        List<LootEntry> entries = new ArrayList<>();
        if (obj.has("items")) {
            for (var el : obj.getAsJsonArray("items")) {
                entries.add(LootEntry.fromJson(el.getAsJsonObject()));
            }
        } else if (obj.has("loot")) {
            // backward compatibility
            String id = obj.get("loot").getAsString();
            int per = obj.has("itemsPerHour") ? obj.get("itemsPerHour").getAsInt() : 1;
            LootEntry e = new LootEntry(id, per);
            e.stored = obj.has("stored") ? obj.get("stored").getAsInt() : 0;
            entries.add(e);
        }
        ResourceAreaData data = new ResourceAreaData(name, entries, cap);
        if (obj.has("owner")) data.ownerGuild = obj.get("owner").getAsString();
        if (obj.has("claim")) data.claimTimestamp = obj.get("claim").getAsLong();
        if (obj.has("last")) data.lastTimestamp = obj.get("last").getAsLong();
        return data;
    }
} 