package org.lupz.doomsdayessentials.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lupz.doomsdayessentials.EssentialsMod;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Mod.EventBusSubscriber(modid = EssentialsMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class AdminChatCommand {
    // Holds UUIDs of players who currently have admin chat toggled on
    private static final Set<UUID> TOGGLED = ConcurrentHashMap.newKeySet();

    // ---------------------------------------------------------------------
    // Command registration
    // ---------------------------------------------------------------------

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("admin")
                        .requires(src -> src.hasPermission(2)) // Only operators (level 2+) can use
                        .then(Commands.literal("chat")
                                // /admin chat <message>
                                .then(Commands.argument("message", StringArgumentType.greedyString())
                                        .executes(AdminChatCommand::sendOnce))
                                // /admin chat (toggle)
                                .executes(AdminChatCommand::toggleChat))
        );
    }

    // ---------------------------------------------------------------------
    // Command implementations
    // ---------------------------------------------------------------------

    private static int toggleChat(CommandContext<CommandSourceStack> ctx) {
        if (!(ctx.getSource().getEntity() instanceof ServerPlayer player)) {
            ctx.getSource().sendFailure(Component.literal("Only players can use this command.").withStyle(ChatFormatting.RED));
            return 0;
        }

        UUID uuid = player.getUUID();
        boolean enabled;
        if (TOGGLED.contains(uuid)) {
            TOGGLED.remove(uuid);
            enabled = false;
        } else {
            TOGGLED.add(uuid);
            enabled = true;
        }

        ctx.getSource().sendSuccess(() -> Component.literal("Admin chat ")
                .withStyle(ChatFormatting.GOLD)
                .append(Component.literal(enabled ? "ativado" : "desativado")
                        .withStyle(enabled ? ChatFormatting.GREEN : ChatFormatting.RED)), false);
        return 1;
    }

    private static int sendOnce(CommandContext<CommandSourceStack> ctx) {
        if (!(ctx.getSource().getEntity() instanceof ServerPlayer player)) {
            ctx.getSource().sendFailure(Component.literal("Only players can use this command.").withStyle(ChatFormatting.RED));
            return 0;
        }

        String message = StringArgumentType.getString(ctx, "message");
        broadcastAdminChat(player, message);
        return 1;
    }

    // ---------------------------------------------------------------------
    // Helper API used by event handler
    // ---------------------------------------------------------------------

    public static boolean isInAdminChat(UUID playerId) {
        return TOGGLED.contains(playerId);
    }

    public static void clearAdminChat(UUID playerId) {
        TOGGLED.remove(playerId);
    }

    public static void broadcastAdminChat(ServerPlayer sender, String rawMessage) {
        Component prefix = Component.literal("[Admin] ").withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD);
        Component name = Component.literal(sender.getName().getString()).withStyle(ChatFormatting.GOLD);
        Component sep = Component.literal(": ").withStyle(ChatFormatting.GRAY);
        Component msg = Component.literal(rawMessage).withStyle(ChatFormatting.YELLOW);
        Component finalComponent = prefix.copy().append(name).append(sep).append(msg);

        sender.getServer().getPlayerList().getPlayers().forEach(player -> {
            if (player.hasPermissions(2)) {
                player.sendSystemMessage(finalComponent);
            }
        });
    }
} 