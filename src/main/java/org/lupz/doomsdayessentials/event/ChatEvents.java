package org.lupz.doomsdayessentials.event;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lupz.doomsdayessentials.EssentialsMod;
import org.lupz.doomsdayessentials.config.EssentialsConfig;
import org.lupz.doomsdayessentials.utils.ChatUtils;

import java.util.List;

@Mod.EventBusSubscriber(modid = EssentialsMod.MOD_ID)
public class ChatEvents {

    @SubscribeEvent
    public static void onServerChat(ServerChatEvent event) {
        ServerPlayer sender = event.getPlayer();
        String message = event.getRawText();

        event.setCanceled(true);
        sendLocalMessage(sender, message);
    }

    private static void sendLocalMessage(ServerPlayer sender, String message) {
        MutableComponent prefix = Component.literal("[L] ").withStyle(ChatFormatting.GRAY);
        Component formattedMessage = ChatUtils.formatMessage(message);
        MutableComponent fullMessage = prefix.append(sender.getDisplayName()).append(Component.literal(": ")).append(formattedMessage);

        int radius = EssentialsConfig.LOCAL_CHAT_RADIUS.get();
        double radiusSqr = radius * radius;

        List<ServerPlayer> players = sender.getServer().getPlayerList().getPlayers();
        for (ServerPlayer player : players) {
            if (sender.level() == player.level() && sender.distanceToSqr(player) <= radiusSqr) {
                player.sendSystemMessage(fullMessage);
            }
        }
    }
} 