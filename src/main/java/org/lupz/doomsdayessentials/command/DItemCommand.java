package org.lupz.doomsdayessentials.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import org.lupz.doomsdayessentials.EssentialsMod;
import org.lupz.doomsdayessentials.item.BlockedItemManager;

/**
 * Command implementation for /ditem block <itemId> which adds the given item id to the blocked list.
 */
@Mod.EventBusSubscriber(modid = EssentialsMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class DItemCommand {

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent e) {
        register(e.getDispatcher());
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("ditem")
                        .requires(src -> src.hasPermission(2)) // operators only
                        .then(Commands.literal("block")
                                // /ditem block (no arg) -> block held item
                                .executes(DItemCommand::blockHeld)
                                // /ditem block <id>
                                .then(Commands.argument("itemId", StringArgumentType.string()).suggests(ITEM_PROVIDER)
                                        .executes(DItemCommand::blockItem)))
                        .then(Commands.literal("unblock")
                                .executes(DItemCommand::unblockHeld)
                                .then(Commands.argument("itemId", StringArgumentType.string()).suggests(ITEM_PROVIDER)
                                        .executes(DItemCommand::unblockItem)))
        );
    }

    // ------------------------------------------------------------
    // Suggestion provider – lists all registered item IDs
    // ------------------------------------------------------------
    private static final SuggestionProvider<CommandSourceStack> ITEM_PROVIDER = (ctx, builder) -> {
        String remaining = builder.getRemainingLowerCase();
        ForgeRegistries.ITEMS.getKeys().stream()
                .map(ResourceLocation::toString)
                .filter(id -> id.startsWith(remaining))
                .forEach(builder::suggest);
        return builder.buildFuture();
    };

    private static int blockItem(CommandContext<CommandSourceStack> ctx) {
        String id = StringArgumentType.getString(ctx, "itemId");
        ResourceLocation rl = ResourceLocation.tryParse(id);
        if (rl == null || !ForgeRegistries.ITEMS.containsKey(rl)) {
            ctx.getSource().sendFailure(Component.literal("Item inválido."));
            return 0;
        }
        BlockedItemManager.get().block(id);
        ctx.getSource().sendSuccess(() -> Component.literal("Item " + id + " bloqueado."), true);
        return 1;
    }

    private static int unblockItem(CommandContext<CommandSourceStack> ctx) {
        String id = StringArgumentType.getString(ctx, "itemId");
        ResourceLocation rl = ResourceLocation.tryParse(id);
        if (rl == null || !ForgeRegistries.ITEMS.containsKey(rl)) {
            ctx.getSource().sendFailure(Component.literal("Item inválido."));
            return 0;
        }
        BlockedItemManager.get().unblock(id);
        ctx.getSource().sendSuccess(() -> Component.literal("Item " + id + " desbloqueado."), true);
        return 1;
    }

    private static int blockHeld(CommandContext<CommandSourceStack> ctx) {
        return modifyHeld(ctx, true);
    }

    private static int unblockHeld(CommandContext<CommandSourceStack> ctx) {
        return modifyHeld(ctx, false);
    }

    private static int modifyHeld(CommandContext<CommandSourceStack> ctx, boolean block) {
        if (!(ctx.getSource().getEntity() instanceof net.minecraft.server.level.ServerPlayer player)) {
            ctx.getSource().sendFailure(Component.literal("Somente jogadores podem usar este atalho."));
            return 0;
        }
        var stack = player.getMainHandItem();
        if (stack.isEmpty()) {
            ctx.getSource().sendFailure(Component.literal("Você não está segurando nenhum item."));
            return 0;
        }
        ResourceLocation rl = ForgeRegistries.ITEMS.getKey(stack.getItem());
        if (rl == null) {
            ctx.getSource().sendFailure(Component.literal("Item inválido."));
            return 0;
        }
        if (block) {
            BlockedItemManager.get().block(rl.toString());
            ctx.getSource().sendSuccess(() -> Component.literal("Item " + rl + " bloqueado."), true);
        } else {
            BlockedItemManager.get().unblock(rl.toString());
            ctx.getSource().sendSuccess(() -> Component.literal("Item " + rl + " desbloqueado."), true);
        }
        return 1;
    }
} 