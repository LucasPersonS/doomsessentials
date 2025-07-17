package org.lupz.doomsdayessentials.recycler;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.registries.ForgeRegistries;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

public class RecycleRecipeManager {

    private static final Path RECIPE_PATH = FMLPaths.CONFIGDIR.get().resolve("doomsdayessentials/recycler-recipes.json");
    private static final Map<ResourceLocation, Recipe> RECIPES = new HashMap<>();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public record OutputEntry(ResourceLocation output, int count) {
        public ItemStack toStack() {
            return new ItemStack(ForgeRegistries.ITEMS.getValue(output), count);
        }
    }

    public record Recipe(ResourceLocation input, List<OutputEntry> outputs) {
        public List<ItemStack> getOutputStacks() {
            return outputs.stream().map(OutputEntry::toStack).collect(Collectors.toList());
        }
    }

    public static void loadRecipes() {
        RECIPES.clear();
        if (!Files.exists(RECIPE_PATH)) {
            createDefaultConfig();
        }

        if (Files.exists(RECIPE_PATH)) {
            try (Reader reader = Files.newBufferedReader(RECIPE_PATH)) {
                TypeToken<Map<String, Recipe>> typeToken = new TypeToken<>() {};
                Map<String, Recipe> loadedRecipes = GSON.fromJson(reader, typeToken.getType());
                if (loadedRecipes != null) {
                    loadedRecipes.forEach((key, recipe) -> RECIPES.put(recipe.input(), recipe));
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void addRecipe(String key, Recipe recipe) {
        // Load existing recipes first to avoid overwriting them
        Map<String, Recipe> currentRecipes = new HashMap<>();
        if (Files.exists(RECIPE_PATH)) {
            try (Reader reader = Files.newBufferedReader(RECIPE_PATH)) {
                TypeToken<Map<String, Recipe>> typeToken = new TypeToken<>() {};
                Map<String, Recipe> loadedRecipes = GSON.fromJson(reader, typeToken.getType());
                if (loadedRecipes != null) {
                    currentRecipes.putAll(loadedRecipes);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // Add the new recipe and save back to the file
        currentRecipes.put(key, recipe);
        try (Writer writer = Files.newBufferedWriter(RECIPE_PATH)) {
            GSON.toJson(currentRecipes, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Also update the in-memory map
        RECIPES.put(recipe.input(), recipe);
    }

    private static void createDefaultConfig() {
        try {
            Files.createDirectories(RECIPE_PATH.getParent());
            Map<String, Recipe> defaultRecipes = new HashMap<>();

            Recipe exampleRecipe = new Recipe(
                    ResourceLocation.fromNamespaceAndPath("minecraft", "iron_helmet"),
                    Collections.singletonList(new OutputEntry(ResourceLocation.fromNamespaceAndPath("minecraft", "iron_nugget"), 5))
            );
            defaultRecipes.put("example_iron_helmet", exampleRecipe);

            try (Writer writer = Files.newBufferedWriter(RECIPE_PATH)) {
                GSON.toJson(defaultRecipes, writer);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static Optional<Recipe> getRecipe(ItemStack input) {
        if (input.isEmpty()) {
            return Optional.empty();
        }
        ResourceLocation inputId = ForgeRegistries.ITEMS.getKey(input.getItem());
        return Optional.ofNullable(RECIPES.get(inputId));
    }
     public static Map<ResourceLocation, Recipe> getRecipes() {
        return Collections.unmodifiableMap(RECIPES);
    }
} 