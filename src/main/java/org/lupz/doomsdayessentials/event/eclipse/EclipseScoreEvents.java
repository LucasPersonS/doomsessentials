package org.lupz.doomsdayessentials.event.eclipse;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lupz.doomsdayessentials.EssentialsMod;

@Mod.EventBusSubscriber(modid = EssentialsMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class EclipseScoreEvents {

    private EclipseScoreEvents() {}

    @SubscribeEvent
    public static void onAttachCapabilities(AttachCapabilitiesEvent<Entity> event){
        if (event.getObject() instanceof Player p) {
            if (!p.getCapability(EclipseScoreCapabilityProvider.SCORE_CAPABILITY).isPresent()) {
                event.addCapability(EclipseScoreCapabilityProvider.SCORE_CAPABILITY_ID, new EclipseScoreCapabilityProvider());
            }
        }
    }

    @SubscribeEvent
    public static void onClone(PlayerEvent.Clone event){
        if (!event.isWasDeath()) return;
        var oldP = event.getOriginal();
        var newP = event.getEntity();
        oldP.getCapability(EclipseScoreCapabilityProvider.SCORE_CAPABILITY).ifPresent(oldCap ->
            newP.getCapability(EclipseScoreCapabilityProvider.SCORE_CAPABILITY).ifPresent(newCap ->
                newCap.deserializeNBT(oldCap.serializeNBT()))
        );
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event){
        if (!EclipseEventManager.get().isActive()) return;
        if (event.getEntity() instanceof Player victim){
            // Count all deaths (infection system removed)
            victim.getCapability(EclipseScoreCapabilityProvider.SCORE_CAPABILITY).ifPresent(EclipseScoreCapability::addDeath);
            victim.getCapability(EclipseScoreCapabilityProvider.SCORE_CAPABILITY).ifPresent(cap -> {
                if (cap.getScore() <= -5) cap.setPermaDead(true);
            });
        }
        if (event.getSource().getEntity() instanceof Player killer){
            killer.getCapability(EclipseScoreCapabilityProvider.SCORE_CAPABILITY).ifPresent(EclipseScoreCapability::addKill);
        }
    }
} 