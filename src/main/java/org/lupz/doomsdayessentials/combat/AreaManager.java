package org.lupz.doomsdayessentials.combat;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
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
import java.util.concurrent.ConcurrentHashMap;

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

    private final Map<String, ManagedArea> areas = new ConcurrentHashMap<>();

    private final Gson gson;
    private final Path saveFile;

    private AreaManager() {
        this.gson = new GsonBuilder().setPrettyPrinting().create();
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
        return Collections.unmodifiableCollection(areas.values());
    }

    public ManagedArea getArea(String name) {
        return areas.get(name.toLowerCase());
    }

    public void addArea(@NotNull ManagedArea area) {
        areas.put(area.getName().toLowerCase(), area);
        saveAreas();
        broadcastAreaUpdate();
    }

    public boolean deleteArea(String name) {
        ManagedArea removed = areas.remove(name.toLowerCase());
        if (removed != null) {
            saveAreas();
            broadcastAreaUpdate();
            return true;
        }
        return false;
    }

    public ManagedArea getAreaAt(ServerLevel level, BlockPos pos) {
        ResourceKey<Level> dim = level.dimension();
        return areas.values().stream()
                .filter(a -> a.getDimension().equals(dim) && a.contains(pos))
                .findFirst()
                .orElse(null);
    }

    public void broadcastAreaUpdate() {
        if (ServerLifecycleHooks.getCurrentServer() != null) {
            PacketHandler.CHANNEL.send(net.minecraftforge.network.PacketDistributor.ALL.noArg(), new SyncAreasPacket(areas.values()));
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
            String json = Files.readString(saveFile);
            Type mapType = new TypeToken<Map<String, ManagedArea>>() {}.getType();
            Map<String, ManagedArea> loaded = gson.fromJson(json, mapType);
            if (loaded != null) {
                areas.clear();
                loaded.forEach((k, v) -> areas.put(k.toLowerCase(), v));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void saveAreas() {
        try {
            String json = gson.toJson(areas);
            Files.writeString(saveFile, json);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
} 