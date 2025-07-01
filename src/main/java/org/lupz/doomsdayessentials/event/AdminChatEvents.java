package org.lupz.doomsdayessentials.event;

import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lupz.doomsdayessentials.EssentialsMod;
import org.lupz.doomsdayessentials.command.AdminChatCommand;

@Mod.EventBusSubscriber(modid = EssentialsMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class AdminChatEvents {

    @SubscribeEvent
    public static void onServerChat(ServerChatEvent event) {
        var player = event.getPlayer();
        if (AdminChatCommand.isInAdminChat(player.getUUID())) {
            // Cancel normal chat broadcast and send to admins only
            event.setCanceled(true);
            AdminChatCommand.broadcastAdminChat(player, event.getRawText());
        }
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        var entity = event.getEntity();
        if (entity != null) {
            AdminChatCommand.clearAdminChat(entity.getUUID());
        }
    }
} 