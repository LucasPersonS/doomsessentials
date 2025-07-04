package org.lupz.doomsdayessentials.guild;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lupz.doomsdayessentials.EssentialsMod;

/**
 * Simple particle-based territory visualization for the /organizacao mapa toggle.
 *
 * When enabled, every second the server spawns END_ROD particles along the edge
 * of the guild territory in the chunk where the player is currently located.
 * This is lightweight yet clearly visible.
 */
@Mod.EventBusSubscriber(modid = EssentialsMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class MapVisualizationEvents {
    private static int tickCounter = 0;

    @SubscribeEvent
    public static void onServerPlayerTick(TickEvent.PlayerTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        if (!(e.player instanceof ServerPlayer player)) return;
        if (!ClientGuildData.isMapOn(player.getUUID())) return;

        tickCounter++;
        if (tickCounter % 20 != 0) return; // every 1 second

        ServerLevel level = player.serverLevel();
        GuildsManager manager = GuildsManager.get(level);
        Guild guild = manager.getGuildByMember(player.getUUID());
        if (guild == null || guild.getTotemPosition() == null) {
            return;
        }

        int radiusChunks = GuildConfig.TERRITORY_RADIUS_CHUNKS.get();
        BlockPos totem = guild.getTotemPosition();

        int totemChunkX = totem.getX() >> 4;
        int totemChunkZ = totem.getZ() >> 4;

        // Determine the border of this chunk within the territory
        int playerChunkX = player.blockPosition().getX() >> 4;
        int playerChunkZ = player.blockPosition().getZ() >> 4;

        int minChunkX = totemChunkX - radiusChunks;
        int maxChunkX = totemChunkX + radiusChunks;
        int minChunkZ = totemChunkZ - radiusChunks;
        int maxChunkZ = totemChunkZ + radiusChunks;

        // If player is outside territory, no need to render
        if (playerChunkX < minChunkX || playerChunkX > maxChunkX || playerChunkZ < minChunkZ || playerChunkZ > maxChunkZ) {
            return;
        }

        // Compute absolute block borders of the territory square
        int minX = minChunkX << 4;
        int maxX = (maxChunkX << 4) + 15;
        int minZ = minChunkZ << 4;
        int maxZ = (maxChunkZ << 4) + 15;

        // Step every 4 blocks for performance
        int step = 4;

        int minY = Math.max(level.getMinBuildHeight(), totem.getY() - 48);
        int maxY = Math.min(level.getMaxBuildHeight() - 1, totem.getY() + 48);
        int stepY = 8;

        for (int yIter = minY; yIter <= maxY; yIter += stepY) {
            // West & East vertical walls
            for (int z = minZ; z <= maxZ; z += step) {
                level.sendParticles(ParticleTypes.END_ROD, minX + 0.1, yIter, z + 0.1, 1, 0, 0, 0, 0);
                level.sendParticles(ParticleTypes.END_ROD, maxX + 0.9, yIter, z + 0.1, 1, 0, 0, 0, 0);
            }

            // North & South
            for (int x = minX; x <= maxX; x += step) {
                level.sendParticles(ParticleTypes.END_ROD, x + 0.1, yIter, minZ + 0.1, 1, 0, 0, 0, 0);
                level.sendParticles(ParticleTypes.END_ROD, x + 0.1, yIter, maxZ + 0.9, 1, 0, 0, 0, 0);
            }
        }
    }
} 