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
import net.minecraftforge.event.TickEvent;
import org.lupz.doomsdayessentials.professions.ArmeiroProfession;

/**
 * Re-apply profession passive bonuses when the player logs in or respawns.
 */
@Mod.EventBusSubscriber(modid = EssentialsMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
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

            // Don't copy NBT tags - let apply() set them based on manager state
            // This prevents abandoned professions from being restored

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

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (event.player.level().isClientSide) return;

        Player player = event.player;
        String prof = ProfissaoManager.getProfession(player.getUUID());
        if (prof == null) return;

        // Call tick handlers for each profession
        switch (prof.toLowerCase()) {
            case "combatente" -> CombatenteProfession.tickCombatente(player);
            case "rastreador" -> RastreadorProfession.tickTracker(player);
            case "engenheiro" -> EngenheiroProfession.tickEngineer(player);
            case "medico" -> MedicoProfession.tickMedico(player);
            case "armeiro" -> ArmeiroProfession.tickArmeiro(player);
            // Caçador doesn't have tick handler currently
        }
    }
} 