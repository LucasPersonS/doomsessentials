package org.lupz.doomsdayessentials.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.item.ItemArgument;
import net.minecraft.commands.arguments.item.ItemInput;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import org.lupz.doomsdayessentials.EssentialsMod;
import org.lupz.doomsdayessentials.recycler.RecycleRecipeManager;
import net.minecraft.commands.CommandBuildContext;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Mod.EventBusSubscriber(modid = EssentialsMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class RecyclerCommand {

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        register(event.getDispatcher(), event.getBuildContext());
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext context) {
        dispatcher.register(Commands.literal("recicladora")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("add")
                        .then(Commands.argument("input", ItemArgument.item(context))
                                .then(Commands.argument("output1", ItemArgument.item(context))
                                        .then(Commands.argument("outputCount1", IntegerArgumentType.integer(1))
                                                .executes(c -> addRecipe(c, 1))
                                                .then(Commands.argument("output2", ItemArgument.item(context))
                                                        .then(Commands.argument("outputCount2", IntegerArgumentType.integer(1))
                                                                .executes(c -> addRecipe(c, 2))))))))
                .then(Commands.literal("reload")
                        .executes(RecyclerCommand::reloadRecipes))
                .then(Commands.literal("list")
                        .executes(RecyclerCommand::listRecipes)));
    }

    private static int listRecipes(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        Map<ResourceLocation, RecycleRecipeManager.Recipe> recipes = RecycleRecipeManager.getRecipes();

        if (recipes.isEmpty()) {
            source.sendSuccess(() -> Component.translatable("command.generic.no_recipes"), false);
            return 1;
        }

        source.sendSuccess(() -> Component.translatable("command.recycler.list.header"), false);
        for (RecycleRecipeManager.Recipe recipe : recipes.values()) {
            Item inputItem = ForgeRegistries.ITEMS.getValue(recipe.input());
            String inputName = inputItem != null ? inputItem.getDescription().getString() : recipe.input().toString();

            MutableComponent recipeComp = Component.literal("§a" + inputName + " -> ");

            for (int i = 0; i < recipe.outputs().size(); i++) {
                RecycleRecipeManager.OutputEntry output = recipe.outputs().get(i);
                Item outputItem = ForgeRegistries.ITEMS.getValue(output.output());
                String outputName = outputItem != null ? outputItem.getDescription().getString() : output.output().toString();
                recipeComp.append(Component.literal(output.count() + "x " + outputName));
                if (i < recipe.outputs().size() - 1) {
                    recipeComp.append(Component.literal(", "));
                }
            }
            source.sendSuccess(() -> recipeComp, false);
        }

        return 1;
    }

    private static int addRecipe(CommandContext<CommandSourceStack> context, int outputPairs) throws CommandSyntaxException {
        ItemInput input = ItemArgument.getItem(context, "input");
        CommandSourceStack source = context.getSource();
        Item inputItem = input.getItem();
        ResourceLocation inputId = ForgeRegistries.ITEMS.getKey(inputItem);

        if (inputId == null) {
            source.sendFailure(Component.literal("Invalid input item specified."));
            return 0;
        }

        List<RecycleRecipeManager.OutputEntry> outputs = new ArrayList<>();
        for (int i = 1; i <= outputPairs; i++) {
            ItemInput output = ItemArgument.getItem(context, "output" + i);
            int outputCount = IntegerArgumentType.getInteger(context, "outputCount" + i);
            Item outputItem = output.getItem();
            ResourceLocation outputId = ForgeRegistries.ITEMS.getKey(outputItem);

            if (outputId == null) {
                source.sendFailure(Component.literal("Invalid output item specified for output #" + i));
                return 0;
            }
            outputs.add(new RecycleRecipeManager.OutputEntry(outputId, outputCount));
        }

        RecycleRecipeManager.Recipe newRecipe = new RecycleRecipeManager.Recipe(inputId, outputs);
        RecycleRecipeManager.addRecipe(inputId.getPath(), newRecipe);

        source.sendSuccess(() -> Component.literal("Recycling recipe added for " + inputItem.getDescription().getString()), true);
        return 1;
    }

    private static int reloadRecipes(CommandContext<CommandSourceStack> context) {
        RecycleRecipeManager.loadRecipes();
        context.getSource().sendSuccess(() -> Component.translatable("command.recycler.reloaded"), true);
        return 1;
    }
} 