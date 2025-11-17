package org.lupz.doomsdayessentials.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.loot.LootDataType;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lupz.doomsdayessentials.EssentialsMod;
import org.lupz.doomsdayessentials.entity.AirdropEntity;
import org.lupz.doomsdayessentials.entity.ModEntities;
import org.lupz.doomsdayessentials.airdrop.AirdropLootManager;

import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

@Mod.EventBusSubscriber(modid = EssentialsMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class AirdropCommand {

    private AirdropCommand() {}

    // Custom suggestion provider for loot tables
    private static final SuggestionProvider<CommandSourceStack> LOOT_TABLE_SUGGESTIONS = (context, builder) -> {
        // Get all available loot tables from the server
        ServerLevel level = context.getSource().getLevel();
        Stream<ResourceLocation> lootTables = level.getServer().getLootData().getKeys(LootDataType.TABLE).stream();
        
        // Also include currently configured airdrop loot tables
        Stream<ResourceLocation> configuredTables = AirdropLootManager.getLootTables().stream();
        
        // Combine both streams and provide suggestions
        Stream<String> allSuggestions = Stream.concat(lootTables, configuredTables)
            .distinct()
            .map(ResourceLocation::toString);
            
        return SharedSuggestionProvider.suggest(allSuggestions, builder);
    };

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent e) {
        register(e.getDispatcher());
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("airdrop")
                .requires(src -> src.hasPermission(3))
                .then(Commands.literal("call")
                    .then(Commands.argument("pos", BlockPosArgument.blockPos())
                        .executes(AirdropCommand::callAt)))
                .then(Commands.literal("addloot")
                    .then(Commands.argument("loot_table", ResourceLocationArgument.id())
                        .suggests(LOOT_TABLE_SUGGESTIONS)
                        .executes(AirdropCommand::addLootTable)))
                .then(Commands.literal("removeloot")
                    .then(Commands.argument("loot_table", ResourceLocationArgument.id())
                        .suggests(LOOT_TABLE_SUGGESTIONS)
                        .executes(AirdropCommand::removeLootTable)))
                .then(Commands.literal("listloot")
                    .executes(AirdropCommand::listLootTables))
                .then(Commands.literal("reloadloot")
                    .executes(AirdropCommand::reloadLootTables))
        );
    }

    private static int callAt(CommandContext<CommandSourceStack> ctx) {
        BlockPos pos = BlockPosArgument.getBlockPos(ctx, "pos");
        ServerLevel level = ctx.getSource().getLevel();

        // Spawn the airdrop entity at the specified position
        AirdropEntity entity = new AirdropEntity(ModEntities.AIRDROP.get(), level);
        entity.setPos(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D);
        // Give initial downward velocity to ensure it starts falling
        entity.setDeltaMovement(0, -0.1, 0);
        level.addFreshEntity(entity);

        // Broadcast inbound notification to all players
        broadcastInbound(level, pos);

        ctx.getSource().sendSuccess(() -> Component.literal("§6§l» §e§lAIRDROP §6§l« §7Chamado em §f" + pos.getX() + " " + pos.getY() + " " + pos.getZ()), true);
        return 1;
    }

    private static void broadcastInbound(ServerLevel level, BlockPos pos) {
        String msg = String.format("[Airdrop] Um airdrop foi chamado em %d, %d, %d", pos.getX(), pos.getY(), pos.getZ());
        for (ServerPlayer p : level.getServer().getPlayerList().getPlayers()) {
            p.sendSystemMessage(Component.literal(msg));
        }
    }

    private static int addLootTable(CommandContext<CommandSourceStack> ctx) {
        ResourceLocation lootTable = ResourceLocationArgument.getId(ctx, "loot_table");
        
        if (AirdropLootManager.addLootTable(lootTable)) {
            ctx.getSource().sendSuccess(() -> Component.literal("§a§l» §2§lAIRDROP §a§l« §7Loot table §f" + lootTable + " §7adicionada com sucesso!"), true);
        } else {
            ctx.getSource().sendFailure(Component.literal("§c§l» §4§lAIRDROP §c§l« §7Loot table §f" + lootTable + " §7já existe!"));
        }
        return 1;
    }

    private static int removeLootTable(CommandContext<CommandSourceStack> ctx) {
        ResourceLocation lootTable = ResourceLocationArgument.getId(ctx, "loot_table");
        
        if (AirdropLootManager.removeLootTable(lootTable)) {
            ctx.getSource().sendSuccess(() -> Component.literal("§a§l» §2§lAIRDROP §a§l« §7Loot table §f" + lootTable + " §7removida com sucesso!"), true);
        } else {
            ctx.getSource().sendFailure(Component.literal("§c§l» §4§lAIRDROP §c§l« §7Loot table §f" + lootTable + " §7não encontrada!"));
        }
        return 1;
    }

    private static int listLootTables(CommandContext<CommandSourceStack> ctx) {
        var lootTables = AirdropLootManager.getLootTables();
        
        if (lootTables.isEmpty()) {
            ctx.getSource().sendSuccess(() -> Component.literal("§e§l» §6§lAIRDROP §e§l« §7Nenhuma loot table configurada."), false);
        } else {
            ctx.getSource().sendSuccess(() -> Component.literal("§e§l» §6§lAIRDROP §e§l« §7Loot tables configuradas:"), false);
            for (ResourceLocation lootTable : lootTables) {
                ctx.getSource().sendSuccess(() -> Component.literal("§7- §f" + lootTable), false);
            }
        }
        return 1;
    }

    private static int reloadLootTables(CommandContext<CommandSourceStack> ctx) {
        AirdropLootManager.reloadLootTables();
        ctx.getSource().sendSuccess(() -> Component.literal("§a§l» §2§lAIRDROP §a§l« §7Loot tables recarregadas com sucesso!"), true);
        return 1;
    }
}