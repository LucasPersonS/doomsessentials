package org.lupz.doomsdayessentials.professions.shop;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public final class EngineerShopUtil {

    private static final Path RECIPE_PATH = FMLPaths.CONFIGDIR.get().resolve("doomsdayessentials/recipes.json");
    private static final Map<String, Entry> ENTRIES = new HashMap<>();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().enableComplexMapKeySerialization().create();

    public record Entry(ResourceLocation outputId, int outputCount, Map<ResourceLocation, Integer> costs) {
    }

    public static void loadConfig() {
        ENTRIES.clear();
        if (!Files.exists(RECIPE_PATH)) {
            createDefaultConfig();
        }

        if (Files.exists(RECIPE_PATH)) {
            try (Reader reader = Files.newBufferedReader(RECIPE_PATH)) {
                TypeToken<Map<String, Entry>> typeToken = new TypeToken<>() {};
                Map<String, Entry> loadedEntries = GSON.fromJson(reader, typeToken.getType());
                if (loadedEntries != null) {
                    ENTRIES.putAll(loadedEntries);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void addRecipe(String alias, Entry entry) {
        Map<String, Entry> currentEntries = new HashMap<>();
        if (Files.exists(RECIPE_PATH)) {
            try (Reader reader = Files.newBufferedReader(RECIPE_PATH)) {
                TypeToken<Map<String, Entry>> typeToken = new TypeToken<>() {};
                Map<String, Entry> loadedEntries = GSON.fromJson(reader, typeToken.getType());
                if(loadedEntries != null) {
                    currentEntries.putAll(loadedEntries);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        currentEntries.put(alias, entry);

        try (Writer writer = Files.newBufferedWriter(RECIPE_PATH)) {
            GSON.toJson(currentEntries, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void createDefaultConfig() {
        try {
            Files.createDirectories(RECIPE_PATH.getParent());
            Map<String, Entry> defaultRecipes = new HashMap<>();

            Map<ResourceLocation, Integer> costs = new HashMap<>();
            costs.put(new ResourceLocation("minecraft", "diamond"), 3);
            costs.put(new ResourceLocation("minecraft", "stick"), 2);

            Entry exampleEntry = new Entry(new ResourceLocation("minecraft", "diamond_pickaxe"), 1, costs);
            defaultRecipes.put("example_recipe", exampleEntry);

            try (Writer writer = Files.newBufferedWriter(RECIPE_PATH)) {
                GSON.toJson(defaultRecipes, writer);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static Map<String, Entry> getEntries() {
        return Collections.unmodifiableMap(ENTRIES);
    }
} 