package org.lupz.doomsdayessentials.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import org.lupz.doomsdayessentials.utils.ChatUtils;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lupz.doomsdayessentials.EssentialsMod;

@Mod.EventBusSubscriber(modid = EssentialsMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class GlobalChatCommand {

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("g")
                .then(Commands.argument("message", StringArgumentType.greedyString())
                        .executes(context -> {
                            ServerPlayer sender = context.getSource().getPlayerOrException();
                            String message = StringArgumentType.getString(context, "message");

                            // Mention notifications
                            ChatUtils.processMentions(sender, message);

                            MutableComponent prefix = Component.literal("[G] ").withStyle(ChatFormatting.YELLOW);
                            Component formattedMessage = ChatUtils.formatMessage(message);
                            MutableComponent fullMessage = prefix.append(sender.getDisplayName()).append(Component.literal(": ")).append(formattedMessage);

                            PlayerList playerList = sender.getServer().getPlayerList();
                            playerList.broadcastSystemMessage(fullMessage, false);

                            return 1;
                        })
                )
        );
    }
} 