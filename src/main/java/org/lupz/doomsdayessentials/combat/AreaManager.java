package org.lupz.doomsdayessentials.combat;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.lupz.doomsdayessentials.network.PacketHandler;
import org.lupz.doomsdayessentials.network.packet.s2c.SyncAreasPacket;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Loads / stores combat areas to disk and provides lookup helpers.
 */
public class AreaManager {

    private static final Codec<Map<String, ManagedArea>> AREAS_CODEC = Codec.unboundedMap(Codec.STRING, ManagedArea.CODEC);
    private static final AreaManager INSTANCE = new AreaManager();

    public static AreaManager get() {
        return INSTANCE;
    }

    // ---------------------------------------------------------------------
    // Fields
    // ---------------------------------------------------------------------

    private final Map<ResourceKey<Level>, List<ManagedArea>> areasByDimension = new ConcurrentHashMap<>();
    private final Map<String, ManagedArea> areasByName = new ConcurrentHashMap<>();

    private final Path saveFile;
    private final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private AreaManager() {
        Path configDir = Path.of("config", "doomsdayessentials");
        this.saveFile = configDir.resolve("areas.json");

        if (!Files.exists(configDir)) {
            try {
                Files.createDirectories(configDir);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        load();
    }

    // ---------------------------------------------------------------------
    // Public API
    // ---------------------------------------------------------------------

    public Collection<ManagedArea> getAreas() {
        return Collections.unmodifiableCollection(areasByName.values());
    }

    public ManagedArea getArea(String name) {
        return areasByName.get(name.toLowerCase());
    }

    public void addArea(@NotNull ManagedArea area) {
        deleteArea(area.getName());
        areasByName.put(area.getName().toLowerCase(), area);
        areasByDimension.computeIfAbsent(area.getDimension(), k -> new java.util.ArrayList<>()).add(area);
        saveAreas();
        broadcastAreaUpdate();
    }

    public boolean deleteArea(String name) {
        String key = name.toLowerCase();
        ManagedArea removed = areasByName.remove(key);
        boolean removedSomething = removed != null;

        // If we had a direct match in the name map, remove *all* matching areas from the dimension list
        if (removed != null) {
            java.util.List<ManagedArea> list = areasByDimension.get(removed.getDimension());
            if (list != null) {
                removedSomething |= list.removeIf(a -> a.getName().equalsIgnoreCase(name));
            }
        } else {
            // Fallback – iterate every dimension list looking for matching names (handles rare desyncs)
            for (java.util.List<ManagedArea> list : areasByDimension.values()) {
                removedSomething |= list.removeIf(a -> a.getName().equalsIgnoreCase(name));
            }
        }

        if (removedSomething) {
            saveAreas();
            broadcastAreaUpdate();
        }
        return removedSomething;
    }

    public ManagedArea getAreaAt(ServerLevel level, BlockPos pos) {
        ResourceKey<Level> dim = level.dimension();
        List<ManagedArea> areasInDim = areasByDimension.get(dim);
        if (areasInDim == null) {
            return null;
        }
        ManagedArea overlayBest = null;
        long overlayBestVol = Long.MAX_VALUE;
        ManagedArea normalBest = null;
        long normalBestVol = Long.MAX_VALUE;
        for (ManagedArea area : areasInDim) {
            if (!area.contains(pos) || !area.isCurrentlyOpen()) continue;
            long dx = (long) (area.getPos2().getX() - area.getPos1().getX() + 1);
            long dy = (long) (area.getPos2().getY() - area.getPos1().getY() + 1);
            long dz = (long) (area.getPos2().getZ() - area.getPos1().getZ() + 1);
            long vol = dx * dy * dz;
            if (area.isOverlayPreferred()) {
                if (vol < overlayBestVol) { overlayBestVol = vol; overlayBest = area; }
            } else {
                if (vol < normalBestVol) { normalBestVol = vol; normalBest = area; }
            }
        }
        return overlayBest != null ? overlayBest : normalBest;
    }

    /**
     * Same as {@link #getAreaAt(ServerLevel, BlockPos)} but ignores opening-hours
     * restrictions – it will return the matching area even if it is currently
     * closed.
     */
    public ManagedArea getAreaAtIncludingClosed(ServerLevel level, BlockPos pos) {
        ResourceKey<Level> dim = level.dimension();
        List<ManagedArea> areasInDim = areasByDimension.get(dim);
        if (areasInDim == null) return null;

        ManagedArea overlayBest = null;
        long overlayBestVol = Long.MAX_VALUE;
        ManagedArea normalBest = null;
        long normalBestVol = Long.MAX_VALUE;
        for (ManagedArea area : areasInDim) {
            if (!area.contains(pos)) continue;
            long dx = (long) (area.getPos2().getX() - area.getPos1().getX() + 1);
            long dy = (long) (area.getPos2().getY() - area.getPos1().getY() + 1);
            long dz = (long) (area.getPos2().getZ() - area.getPos1().getZ() + 1);
            long vol = dx * dy * dz;
            if (area.isOverlayPreferred()) {
                if (vol < overlayBestVol) { overlayBestVol = vol; overlayBest = area; }
            } else {
                if (vol < normalBestVol) { normalBestVol = vol; normalBest = area; }
            }
        }
        return overlayBest != null ? overlayBest : normalBest;
    }

    public void broadcastAreaUpdate() {
        if (ServerLifecycleHooks.getCurrentServer() != null) {
            PacketHandler.CHANNEL.send(net.minecraftforge.network.PacketDistributor.ALL.noArg(), new SyncAreasPacket(getAreas()));
        }
    }

    // ---------------------------------------------------------------------
    // Persistence helpers
    // ---------------------------------------------------------------------

    private void load() {
        if (!Files.exists(saveFile)) {
            return;
        }
        try {
            String jsonString = Files.readString(saveFile);
            if (jsonString.isEmpty()) return;
            JsonElement jsonElement = JsonParser.parseString(jsonString);

            AREAS_CODEC.parse(JsonOps.INSTANCE, jsonElement)
                    .resultOrPartial(error -> System.err.println("[DoomsEssentials] Failed to load areas.json: " + error))
                    .ifPresent(loaded -> {
                        areasByName.clear();
                        areasByDimension.clear();
                        loaded.forEach((k, v) -> {
                            areasByName.put(k.toLowerCase(), v);
                            areasByDimension.computeIfAbsent(v.getDimension(), key -> new ArrayList<>()).add(v);
                        });

                        if (jsonElement.isJsonObject()) {
                            com.google.gson.JsonObject root = jsonElement.getAsJsonObject();
                            for (java.util.Map.Entry<String, com.google.gson.JsonElement> en : root.entrySet()) {
                                String name = en.getKey();
                                ManagedArea area = areasByName.get(name.toLowerCase());
                                if (area == null) continue;
                                if (en.getValue().isJsonObject()) {
                                    com.google.gson.JsonObject obj = en.getValue().getAsJsonObject();
                                    try {
                                        if (obj.has("overlay")) {
                                            area.setOverlayPreferred(obj.get("overlay").getAsBoolean());
                                        }
                                        if (obj.has("open_windows") && obj.get("open_windows").isJsonArray()) {
                                            java.util.List<ManagedArea.TimeWindow> wins = new java.util.ArrayList<>();
                                            for (com.google.gson.JsonElement el2 : obj.get("open_windows").getAsJsonArray()) {
                                                if (!el2.isJsonObject()) continue;
                                                com.google.gson.JsonObject w = el2.getAsJsonObject();
                                                String s = w.has("start") ? w.get("start").getAsString() : null;
                                                String e = w.has("end") ? w.get("end").getAsString() : null;
                                                if (s != null && e != null) {
                                                    try {
                                                        java.time.LocalTime ls = java.time.LocalTime.parse(s);
                                                        java.time.LocalTime le = java.time.LocalTime.parse(e);
                                                        wins.add(new ManagedArea.TimeWindow(ls, le));
                                                    } catch (Exception ignored) {}
                                                }
                                            }
                                            area.setOpenWindows(wins);
                                        }
                                    } catch (Exception ignored) {}
                                }
                            }
                        }
                    });

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void saveAreas() {
        try {
            AREAS_CODEC.encodeStart(JsonOps.INSTANCE, areasByName)
                    .resultOrPartial(error -> System.err.println("[DoomsEssentials] Failed to save areas.json: " + error))
                    .ifPresent(jsonElement -> {
                        try {
                            if (jsonElement.isJsonObject()) {
                                com.google.gson.JsonObject root = jsonElement.getAsJsonObject();
                                for (java.util.Map.Entry<String, ManagedArea> en : areasByName.entrySet()) {
                                    String name = en.getKey();
                                    ManagedArea area = en.getValue();
                                    com.google.gson.JsonObject obj = root.getAsJsonObject(name);
                                    if (obj == null) {
                                        obj = new com.google.gson.JsonObject();
                                        root.add(name, obj);
                                    }
                                    obj.addProperty("overlay", area.isOverlayPreferred());
                                    com.google.gson.JsonArray wins = new com.google.gson.JsonArray();
                                    for (ManagedArea.TimeWindow tw : area.getOpenWindows()) {
                                        com.google.gson.JsonObject o = new com.google.gson.JsonObject();
                                        o.addProperty("start", tw.start().toString());
                                        o.addProperty("end", tw.end().toString());
                                        wins.add(o);
                                    }
                                    obj.add("open_windows", wins);
                                }
                            }
                            Files.writeString(saveFile, GSON.toJson(jsonElement));
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
} 
