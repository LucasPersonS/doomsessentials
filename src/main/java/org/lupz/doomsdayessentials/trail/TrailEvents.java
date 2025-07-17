package org.lupz.doomsdayessentials.trail;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE)
public class TrailEvents {

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)) {
            return;
        }

        TrailManager.getInstance().getActiveTrails().forEach((uuid, trailData) -> {
            if (player.getUUID().equals(uuid)) {
                // Special shape logic for the black smoke particle
                if (trailData.getParticle().toString().toLowerCase().contains("black_smoke")) {
                    // Replicate "emitter_shape_disc" from the JSON
                    double radius = 3.0;
                    double angle = player.level().random.nextDouble() * 2 * Math.PI;
                    double offsetX = Math.cos(angle) * radius;
                    double offsetZ = Math.sin(angle) * radius;
                    
                    // Replicate "offset" from the JSON
                    Vec3 pos = player.position().add(offsetX, 0.5, offsetZ); 
                    
                    player.serverLevel().sendParticles(trailData.getParticle(), pos.x(), pos.y(), pos.z(),
                        trailData.getCount(), trailData.getDx(), trailData.getDy(), trailData.getDz(), trailData.getSpeed());

                } else {
                    // Default trail behavior for all other particles
                    Vec3 pos = player.position().subtract(0, 0.2, 0);
                    player.serverLevel().sendParticles(trailData.getParticle(), pos.x(), pos.y(), pos.z(),
                        trailData.getCount(), trailData.getDx(), trailData.getDy(), trailData.getDz(), trailData.getSpeed());
                }
            }
        });
    }
} 