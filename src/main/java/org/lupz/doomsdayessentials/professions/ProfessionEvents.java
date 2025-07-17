package org.lupz.doomsdayessentials.professions;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lupz.doomsdayessentials.EssentialsMod;
import org.lupz.doomsdayessentials.professions.capability.TrackerCapabilityProvider;
import org.lupz.doomsdayessentials.professions.EngenheiroProfession;

/**
 * Re-apply profession passive bonuses when the player logs in or respawns.
 */
@Mod.EventBusSubscriber(modid = EssentialsMod.MOD_ID)
public final class ProfessionEvents {

    @SubscribeEvent
    public static void onAttachCapabilities(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof Player) {
            if (!event.getObject().getCapability(TrackerCapabilityProvider.TRACKER_CAPABILITY).isPresent()) {
                event.addCapability(TrackerCapabilityProvider.TRACKER_CAPABILITY_ID, new TrackerCapabilityProvider());
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        apply(event.getEntity());
    }

    @SubscribeEvent
    public static void onClone(PlayerEvent.Clone event) {
        if (event.isWasDeath()) {
            net.minecraft.world.entity.player.Player newPlayer = event.getEntity();
            java.util.UUID uuid = newPlayer.getUUID();

            if (event.getOriginal().getPersistentData().getBoolean("isRastreador")) {
                newPlayer.getPersistentData().putBoolean("isRastreador", true);
            }
            if (event.getOriginal().getPersistentData().getBoolean("isEngenheiro")) {
                newPlayer.getPersistentData().putBoolean("isEngenheiro", true);
            }

            apply(newPlayer);
            event.getOriginal().getCapability(TrackerCapabilityProvider.TRACKER_CAPABILITY).ifPresent(oldCap -> {
                event.getEntity().getCapability(TrackerCapabilityProvider.TRACKER_CAPABILITY).ifPresent(newCap -> {
                    newCap.deserializeNBT(oldCap.serializeNBT());
                });
            });
        }
    }

    private static void apply(net.minecraft.world.entity.player.Player player) {
        String prof = ProfissaoManager.getProfession(player.getUUID());
        if (prof == null) return;
        switch (prof.toLowerCase()) {
            case "combatente" -> CombatenteProfession.applyBonuses(player);
            case "rastreador" -> RastreadorProfession.applyBonuses(player);
            case "engenheiro" -> EngenheiroProfession.applyBonuses(player);
            // Add future professions here
        }
    }
} 