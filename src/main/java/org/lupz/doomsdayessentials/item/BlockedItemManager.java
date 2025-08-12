package org.lupz.doomsdayessentials.item;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.minecraftforge.fml.loading.FMLPaths;
import org.lupz.doomsdayessentials.EssentialsMod;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Maintains a persistent list of blocked item registry ids (e.g. "minecraft:golden_apple").
 * The list is stored as a simple JSON array at <gameDir>/config/doomsdayessentials/blocked_items.json.
 */
public final class BlockedItemManager {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static final BlockedItemManager INSTANCE = new BlockedItemManager();
    public static BlockedItemManager get() { return INSTANCE; }

    private final Path savePath;

    private final Set<String> blockedIds = new HashSet<>();

    private BlockedItemManager() {
        // Resolve config directory lazily – FMLPaths is guaranteed to be ready at this point (during commonSetup)
        Path cfgDir = FMLPaths.CONFIGDIR.get().resolve("doomsdayessentials");
        this.savePath = cfgDir.resolve("blocked_items.json");
        load();
    }

    // ------------------------------------------------------------
    // Public API
    // ------------------------------------------------------------

    /** Returns an immutable view of blocked ids. */
    public Set<String> getBlockedIds() {
        return Collections.unmodifiableSet(blockedIds);
    }

    /** Returns true if the given registry id (namespace:path) is blocked. */
    public boolean isBlocked(String id) {
        return blockedIds.contains(id);
    }

    /** Adds the id to the blocked list and writes to disk. */
    public void block(String id) {
        if (blockedIds.add(id)) {
            save();
            EssentialsMod.LOGGER.info("Item {} blocked via command.", id);
        }
    }

    /** Removes the id from the blocked list and writes to disk. */
    public void unblock(String id) {
        if (blockedIds.remove(id)) {
            save();
            EssentialsMod.LOGGER.info("Item {} unblocked via command.", id);
        }
    }

    // ------------------------------------------------------------
    // Persistence helpers
    // ------------------------------------------------------------

    private void load() {
        try {
            Files.createDirectories(savePath.getParent());
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (!Files.exists(savePath)) {
            save(); // create empty file
            return;
        }
        try (Reader reader = Files.newBufferedReader(savePath)) {
            java.util.List<String> list = GSON.fromJson(reader, new TypeToken<java.util.List<String>>(){}.getType());
            if (list != null) {
                blockedIds.clear();
                blockedIds.addAll(list);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void save() {
        try (Writer writer = Files.newBufferedWriter(savePath)) {
            GSON.toJson(blockedIds, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
} 