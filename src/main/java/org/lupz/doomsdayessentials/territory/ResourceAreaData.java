package org.lupz.doomsdayessentials.territory;

import com.google.gson.JsonObject;

public class ResourceAreaData {
    public String areaName;
    public String lootId;
    public int itemsPerHour;
    public int storageCap;

    public String ownerGuild; // nullable
    public int storedItems;
    public long lastTimestamp;

    public ResourceAreaData(String areaName, String lootId, int itemsPerHour, int storageCap) {
        this.areaName = areaName;
        this.lootId = lootId;
        this.itemsPerHour = itemsPerHour;
        this.storageCap = storageCap;
        this.ownerGuild = null;
        this.storedItems = 0;
        this.lastTimestamp = System.currentTimeMillis();
    }

    // Serialization helpers
    public JsonObject toJson() {
        JsonObject obj = new JsonObject();
        obj.addProperty("loot", lootId);
        obj.addProperty("itemsPerHour", itemsPerHour);
        obj.addProperty("storageCap", storageCap);
        if (ownerGuild != null) obj.addProperty("owner", ownerGuild);
        obj.addProperty("stored", storedItems);
        obj.addProperty("last", lastTimestamp);
        return obj;
    }

    public static ResourceAreaData fromJson(String name, JsonObject obj) {
        String loot = obj.get("loot").getAsString();
        int iph = obj.has("itemsPerHour") ? obj.get("itemsPerHour").getAsInt() : 1;
        int cap = obj.has("storageCap") ? obj.get("storageCap").getAsInt() : 64;
        ResourceAreaData data = new ResourceAreaData(name, loot, iph, cap);
        if (obj.has("owner")) data.ownerGuild = obj.get("owner").getAsString();
        if (obj.has("stored")) data.storedItems = obj.get("stored").getAsInt();
        if (obj.has("last")) data.lastTimestamp = obj.get("last").getAsLong();
        return data;
    }
} 