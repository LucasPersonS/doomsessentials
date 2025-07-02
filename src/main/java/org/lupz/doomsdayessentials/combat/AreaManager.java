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

    private static final AreaManager INSTANCE = new AreaManager();

    public static AreaManager get() {
        return INSTANCE;
    }

    // ---------------------------------------------------------------------
    // Fields
    // ---------------------------------------------------------------------

    private final Map<ResourceKey<Level>, List<ManagedArea>> areasByDimension = new ConcurrentHashMap<>();
    private final Map<String, ManagedArea> areasByName = new ConcurrentHashMap<>();

    private static final Codec<Map<String, ManagedArea>> AREAS_CODEC = Codec.unboundedMap(Codec.STRING, ManagedArea.CODEC);
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
        areasByName.put(area.getName().toLowerCase(), area);
        areasByDimension.computeIfAbsent(area.getDimension(), k -> new ArrayList<>()).add(area);
        saveAreas();
        broadcastAreaUpdate();
    }

    public boolean deleteArea(String name) {
        ManagedArea removed = areasByName.remove(name.toLowerCase());
        if (removed != null) {
            areasByDimension.getOrDefault(removed.getDimension(), new ArrayList<>()).remove(removed);
            saveAreas();
            broadcastAreaUpdate();
            return true;
        }
        return false;
    }

    public ManagedArea getAreaAt(ServerLevel level, BlockPos pos) {
        ResourceKey<Level> dim = level.dimension();
        List<ManagedArea> areasInDim = areasByDimension.get(dim);
        if (areasInDim == null) {
            return null;
        }
        return areasInDim.stream()
                .filter(a -> a.contains(pos))
                .findFirst()
                .orElse(null);
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