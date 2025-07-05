package org.lupz.doomsdayessentials.professions;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lupz.doomsdayessentials.EssentialsMod;

import java.util.UUID;

/**
 * Sends real-time distance updates to medics that have accepted a help request.
 */
@Mod.EventBusSubscriber(modid = EssentialsMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class MedicalHelpEvents {

    private MedicalHelpEvents() {}

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!(event.player instanceof ServerPlayer player)) return;
        if (!player.getPersistentData().hasUUID("medicoHelpTarget")) return;

        UUID targetUuid = player.getPersistentData().getUUID("medicoHelpTarget");
        ServerPlayer target = player.server.getPlayerList().getPlayer(targetUuid);
        if (target == null) return;

        // Update once per second
        if (player.tickCount % 20 != 0) return;

        double dist = Math.sqrt(player.distanceToSqr(target));
        int metres = (int) Math.round(dist);
        player.displayClientMessage(Component.literal("§eDistância até paciente: " + metres + "m"), true);
    }
} 