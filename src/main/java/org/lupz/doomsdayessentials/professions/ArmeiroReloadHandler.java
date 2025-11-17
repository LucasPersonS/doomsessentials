package org.lupz.doomsdayessentials.professions;

import com.tacz.guns.api.entity.IGunOperator;
import com.tacz.guns.entity.shooter.ShooterDataHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lupz.doomsdayessentials.EssentialsMod;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Event-based handler for Armeiro reload speed bonus
 * This approach modifies the reload timestamp every tick for Armeiro players
 */
@Mod.EventBusSubscriber(modid = EssentialsMod.MOD_ID)
public class ArmeiroReloadHandler {
    
    private static final Map<UUID, Long> realReloadTimestamps = new HashMap<>();
    
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.START) return;
        Player player = event.player;
        if (player.level().isClientSide) return; // Server only
        
        UUID playerId = player.getUUID();
        boolean isArmeiro = ArmeiroProfession.isArmeiro(player);
        
        // Safety check: ensure tag matches profession registry
        String actualProfession = ProfissaoManager.getProfession(playerId);
        if (isArmeiro && !"armeiro".equalsIgnoreCase(actualProfession)) {
            // Force cleanup if mismatch detected
            player.getPersistentData().putBoolean("isArmeiro", false);
            return;
        }
        
        // Get TACZ data
        IGunOperator operator = IGunOperator.fromLivingEntity(player);
        ShooterDataHolder data = operator.getDataHolder();
        
        if (data == null) return;
        
        // If currently reloading
        if (data.reloadTimestamp > 0) {
            // Store the real timestamp if this is the first time we see it
            if (!realReloadTimestamps.containsKey(playerId) || 
                data.reloadTimestamp > realReloadTimestamps.getOrDefault(playerId, -1L)) {
                realReloadTimestamps.put(playerId, data.reloadTimestamp);
            }
            
            long realTimestamp = realReloadTimestamps.get(playerId);
            long currentTime = System.currentTimeMillis();
            long actualElapsed = currentTime - realTimestamp;
            
            // Apply speed boost ONLY for Armeiro players
            if (isArmeiro) {
                // Make reload 30% faster by making elapsed time appear 1.3x larger
                long boostedElapsed = (long) (actualElapsed * 1.3f);
                data.reloadTimestamp = currentTime - boostedElapsed;
            }
        } else {
            // Cleanup when not reloading
            realReloadTimestamps.remove(playerId);
        }
    }
}
