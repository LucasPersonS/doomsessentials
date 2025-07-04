package org.lupz.doomsdayessentials.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.event.level.ExplosionEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lupz.doomsdayessentials.EssentialsMod;
import org.lupz.doomsdayessentials.guild.GuildConfig;

/**
 * Reduces durability of reinforced blocks hit by explosions.
 */
@Mod.EventBusSubscriber(modid = EssentialsMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ReinforcedExplosionHandler {

    @SubscribeEvent
    public static void onExplosionDetonate(ExplosionEvent.Detonate e) {
        var level = e.getLevel();
        double baseDamage = isTaczExplosion(e) ? GuildConfig.TACS_EXPLOSIVE_DAMAGE.get() : GuildConfig.GENERAL_EXPLOSION_DAMAGE.get();
        // First, apply damage to blocks that will be destroyed by vanilla logic (already in list)
        e.getExplosion().getToBlow().forEach(pos -> {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof ReinforcedBlockEntity rbe) {
                // Use explosion radius for scaling. We compute it below but need here; recalculate quickly.
                double radiusLocal;
                try {
                    radiusLocal = (double) e.getExplosion().getClass().getMethod("getRadius").invoke(e.getExplosion());
                } catch (Exception ex) {
                    try {
                        radiusLocal = (double) e.getExplosion().getClass().getField("radius").get(e.getExplosion());
                    } catch (Exception ex2) {
                        radiusLocal = 6.0;
                    }
                }
                float dmg = computeScaledDamage(baseDamage, e.getExplosion().getPosition(), pos, radiusLocal);
                if (dmg > 0)
                    rbe.decreaseDurability(dmg);
            }
        });

        // Additionally, apply damage to nearby reinforced blocks that are too strong to be destroyed by the explosion.
        var center = e.getExplosion().getPosition();
        double radius;
        try {
            // Forge exposes getRadius() starting from 1.19+. Use reflection as fallback.
            radius = (double) e.getExplosion().getClass().getMethod("getRadius").invoke(e.getExplosion());
        } catch (Exception ex) {
            try {
                radius = (double) e.getExplosion().getClass().getField("radius").get(e.getExplosion());
            } catch (Exception ex2) {
                radius = 6.0; // Default TNT radius if everything fails
            }
        }

        int ir = (int) Math.ceil(radius);
        BlockPos centerPos = BlockPos.containing(center);
        for (int dx = -ir; dx <= ir; dx++) {
            for (int dy = -ir; dy <= ir; dy++) {
                for (int dz = -ir; dz <= ir; dz++) {
                    double distSq = dx*dx + dy*dy + dz*dz;
                    if (distSq > radius*radius) continue;
                    BlockPos pos = centerPos.offset(dx, dy, dz);
                    // Skip if already processed via toBlow
                    if (e.getExplosion().getToBlow().contains(pos)) continue;
                    BlockEntity be = level.getBlockEntity(pos);
                    if (be instanceof ReinforcedBlockEntity rbe) {
                        float dmg = computeScaledDamage(baseDamage, center, pos, radius);
                        if (dmg > 0)
                            rbe.decreaseDurability(dmg);
                    }
                }
            }
        }
    }

    private static boolean isTaczExplosion(ExplosionEvent.Detonate e) {
        var exploder = e.getExplosion().getExploder();
        if (exploder != null) {
            String cls = exploder.getClass().getName();
            if (cls.startsWith("com.tacz")) return true;
        }
        // Fallback: check source block/entity registry name if available in future
        return false;
    }

    /**
     * Returns damage scaled linearly by distance: full at center, 0 at radius edge.
     */
    private static float computeScaledDamage(double baseDamage, net.minecraft.world.phys.Vec3 center, net.minecraft.core.BlockPos pos, double radius) {
        double dx = center.x - (pos.getX() + 0.5);
        double dy = center.y - (pos.getY() + 0.5);
        double dz = center.z - (pos.getZ() + 0.5);
        double dist = Math.sqrt(dx*dx + dy*dy + dz*dz);

        if (radius <= 0) radius = 1.0; // avoid div by zero
        double factor = 1.0 - (dist / radius);
        if (factor <= 0) return 0f;
        return (float) (baseDamage * factor);
    }
} 