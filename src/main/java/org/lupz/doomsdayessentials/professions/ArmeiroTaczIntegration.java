package org.lupz.doomsdayessentials.professions;

import com.tacz.guns.api.event.common.GunReloadEvent;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lupz.doomsdayessentials.EssentialsMod;

/**
 * TACZ Integration for Armeiro profession
 * 
 * After checking TACZ source code (https://github.com/MCModderAnchor/TACZ/blob/1.20.1/src/main/java/com/tacz/guns/api/event/common/GunReloadEvent.java),
 * the GunReloadEvent only provides:
 * - getEntity()
 * - getGunItemStack()
 * - getLogicalSide()
 * - isCancelable() = true
 * 
 * It does NOT have methods to modify reload speed.
 * 
 * SOLUTIONS IMPLEMENTED:
 * 1. Event-based (PRIMARY): ArmeiroReloadHandler - Uses TickEvent to modify reload timestamp
 * 2. Mixin-based (BACKUP): TaczReloadSpeedMixin - Injects into LivingEntity methods
 * 
 * Both approaches dynamically adjust the reloadTimestamp to make reload appear 30% faster.
 */
@Mod.EventBusSubscriber(modid = EssentialsMod.MOD_ID)
public class ArmeiroTaczIntegration {

    /**
     * This event fires when reload starts, but we cannot modify the reload speed.
     * Kept for future use if TACZ adds the ability to modify reload speed.
     */
    @SubscribeEvent
    public static void onGunReload(GunReloadEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        
        if (ArmeiroProfession.isArmeiro(player)) {
            // TODO: Implement reload speed bonus when TACZ API supports it
            // Current event only notifies of reload start, cannot modify speed
        }
    }
}
