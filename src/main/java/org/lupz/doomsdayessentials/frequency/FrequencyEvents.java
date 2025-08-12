package org.lupz.doomsdayessentials.frequency;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lupz.doomsdayessentials.EssentialsMod;
import org.lupz.doomsdayessentials.frequency.capability.FrequencyCapability;
import org.lupz.doomsdayessentials.frequency.capability.FrequencyCapabilityProvider;

@Mod.EventBusSubscriber(modid = EssentialsMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class FrequencyEvents {
    private FrequencyEvents() {}

    @SubscribeEvent
    public static void onAttachCapabilities(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof net.minecraft.world.entity.player.Player player) {
            if (!player.getCapability(FrequencyCapabilityProvider.FREQUENCY_CAPABILITY).isPresent()) {
                event.addCapability(FrequencyCapabilityProvider.FREQUENCY_CAPABILITY_ID, new FrequencyCapabilityProvider());
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (!(event.player instanceof ServerPlayer player)) return;
        if (event.phase != TickEvent.Phase.END) return;

        player.getCapability(FrequencyCapabilityProvider.FREQUENCY_CAPABILITY).ifPresent(cap -> {
            int lvl = cap.getLevel();
            if (lvl >= 90) {
                // Blindness refresh every second
                if (player.tickCount % 20 == 0) player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 40, 0, false, false));
            } else if (lvl >= 50) {
                // Darkness refresh every second
                if (player.tickCount % 20 == 0) player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 40, 0, false, false));
            }
        });
    }
} 