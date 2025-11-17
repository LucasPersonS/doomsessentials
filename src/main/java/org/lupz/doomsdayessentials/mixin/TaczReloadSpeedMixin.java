package org.lupz.doomsdayessentials.mixin;

import com.tacz.guns.api.entity.IGunOperator;
import com.tacz.guns.entity.shooter.ShooterDataHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.lupz.doomsdayessentials.professions.ArmeiroProfession;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin to modify TACZ reload speed for Armeiro profession
 * 
 * APPROACH:
 * Hook into the reload() method to detect when reload starts,
 * then modify the reloadTimestamp field directly.
 * 
 * CALCULATION:
 * - Normal: elapsedTime = currentTime - reloadTimestamp
 * - For 30% faster: we make elapsedTime appear 1.3x larger
 * - Adjusted timestamp = currentTime - (actualElapsed * 1.3)
 */
@Mixin(value = LivingEntity.class, priority = 1500, remap = false)
public abstract class TaczReloadSpeedMixin {
    
    @Unique
    private long doomsessentials$realReloadTimestamp = -1;
    
    /**
     * Inject into tick to continuously adjust reload timestamp
     */
    @Inject(
        method = "tick",
        at = @At("HEAD"),
        require = 0
    )
    private void doomsessentials$adjustReloadOnTick(CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (!(self instanceof Player player)) return;
        if (!ArmeiroProfession.isArmeiro(player)) return;
        
        // Cast to IGunOperator to access TACZ's getDataHolder method
        if (!(self instanceof IGunOperator gunOperator)) return;
        ShooterDataHolder data = gunOperator.getDataHolder();
        if (data == null) return;
        
        // If currently reloading
        if (data.reloadTimestamp > 0) {
            // Store the real timestamp if this is the first time we see it
            if (doomsessentials$realReloadTimestamp == -1 || 
                data.reloadTimestamp > doomsessentials$realReloadTimestamp) {
                doomsessentials$realReloadTimestamp = data.reloadTimestamp;
            }
            
            long currentTime = System.currentTimeMillis();
            long actualElapsed = currentTime - doomsessentials$realReloadTimestamp;
            
            // Make reload 30% faster by making elapsed time appear 1.3x larger
            long boostedElapsed = (long) (actualElapsed * 1.3f);
            data.reloadTimestamp = currentTime - boostedElapsed;
        } else {
            // Reset when not reloading
            doomsessentials$realReloadTimestamp = -1;
        }
    }
}
