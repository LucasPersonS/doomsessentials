package org.lupz.doomsdayessentials.territory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.ForgeRegistries;
import org.lupz.doomsdayessentials.EssentialsMod;
import org.lupz.doomsdayessentials.combat.AreaManager;
import org.lupz.doomsdayessentials.combat.AreaType;
import org.lupz.doomsdayessentials.combat.ManagedArea;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Loads static territory area definitions from config/doomsdayessentials/territory_areas.json
 * and registers them in {@link AreaManager} on server start or reload.
 */
public class TerritoryAreaLoader {

    private static final Path FILE_PATH = Path.of("config", "doomsdayessentials", "territory_areas.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    /**
     * Reads the JSON file and registers every listed area that is not yet present.
     * Existing areas are left untouched (to preserve runtime changes).
     */
    public static void load() {
        AreaManager am = AreaManager.get();
        if (!Files.exists(FILE_PATH)) {
            try {
                Files.createDirectories(FILE_PATH.getParent());
                Files.writeString(FILE_PATH, "{}\n");
            } catch (IOException e) {
                EssentialsMod.LOGGER.error("Failed to create territory_areas.json", e);
            }
            return;
        }
        try {
            String txt = Files.readString(FILE_PATH);
            if (txt.isEmpty()) return;
            JsonObject root = JsonParser.parseString(txt).getAsJsonObject();
            for (var entry : root.entrySet()) {
                String name = entry.getKey();
                if (!entry.getValue().isJsonObject()) continue;
                if (am.getArea(name) != null) continue; // already registered
                JsonObject obj = entry.getValue().getAsJsonObject();
                try {
                    String dimStr = obj.get("dimension").getAsString();
                    ResourceKey<Level> dim = ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION, new net.minecraft.resources.ResourceLocation(dimStr));
                    JsonObject p1 = obj.getAsJsonObject("pos1");
                    JsonObject p2 = obj.getAsJsonObject("pos2");
                    BlockPos bp1 = new BlockPos(p1.get("x").getAsInt(), p1.get("y").getAsInt(), p1.get("z").getAsInt());
                    BlockPos bp2 = new BlockPos(p2.get("x").getAsInt(), p2.get("y").getAsInt(), p2.get("z").getAsInt());
                    AreaType type = AreaType.valueOf(obj.get("type").getAsString());
                    ManagedArea area = new ManagedArea(name, type, dim, bp1, bp2);
                    am.addArea(area);
                } catch (Exception ex) {
                    EssentialsMod.LOGGER.error("Failed to load area {} from json", name, ex);
                }
            }
        } catch (IOException e) {
            EssentialsMod.LOGGER.error("Failed to read territory_areas.json", e);
        }
    }
} 