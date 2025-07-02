package org.lupz.doomsdayessentials.professions;

import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lupz.doomsdayessentials.EssentialsMod;

/**
 * Re-apply profession passive bonuses when the player logs in or respawns.
 */
@Mod.EventBusSubscriber(modid = EssentialsMod.MOD_ID)
public final class ProfessionEvents {

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        apply(event.getEntity());
    }

    @SubscribeEvent
    public static void onClone(PlayerEvent.Clone event) {
        if (event.isWasDeath()) {
            apply(event.getEntity());
        }
    }

    private static void apply(net.minecraft.world.entity.player.Player player) {
        String prof = ProfissaoManager.getProfession(player.getUUID());
        if (prof == null) return;
        switch (prof.toLowerCase()) {
            case "combatente" -> CombatenteProfession.applyBonuses(player);
            case "rastreador" -> RastreadorProfession.applyBonuses(player);
            // Add future professions here
        }
    }
} 