package org.lupz.doomsdayessentials.guild;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lupz.doomsdayessentials.EssentialsMod;
import org.lupz.doomsdayessentials.block.ModBlocks;

/**
 * Handles block interaction & movement restrictions inside guild territories.
 */
@Mod.EventBusSubscriber(modid = EssentialsMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class TerritoryEvents {
    /** Tracks each player's current territory tag for entry/leave notifications. */
    private static final java.util.Map<java.util.UUID, String> LAST_TERRITORY = new java.util.HashMap<>();

    /**
     * Clears an area around the newly placed totem so players cannot hide it
     * inside blocks. The cleared area is a (2*radius+1)^2 square centred on the
     * totem and extends 3 blocks upwards.
     */
    private static void clearAreaAroundTotem(ServerLevel level, BlockPos center, int radius) {
        int clearHeight = 3; // number of blocks above totem to clear
        BlockPos.MutableBlockPos mut = new BlockPos.MutableBlockPos();
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                for (int dy = 0; dy <= clearHeight; dy++) {
                    mut.set(center.getX() + dx, center.getY() + dy, center.getZ() + dz);
                    // Do not destroy the totem blocks themselves
                    if (mut.equals(center)) continue;
                    var blk = level.getBlockState(mut).getBlock();
                    if (blk == ModBlocks.TOTEM_BLOCK.get() || blk == ModBlocks.TOTEM_BLOCK_TOP.get()) continue;
                    if (!level.isEmptyBlock(mut)) {
                        level.destroyBlock(mut, false);
                    }
                }
            }
        }
    }

    // ---------------------------------------------------------------------
    // Block place – detect totem placement & general permission check
    // ---------------------------------------------------------------------

    @SubscribeEvent
    public static void onBlockPlace(BlockEvent.EntityPlaceEvent e) {
        if (!(e.getLevel() instanceof ServerLevel level)) return;
        if (!(e.getEntity() instanceof Player player)) return;

        BlockPos pos = e.getPos();
        GuildsManager manager = GuildsManager.get(level);
        Block placedBlock = e.getPlacedBlock().getBlock();

        // -----------------------------------------------------------------
        // Totem placement logic
        // -----------------------------------------------------------------
        if (placedBlock == ModBlocks.TOTEM_BLOCK.get()) {
            Guild guild = manager.getGuildByMember(player.getUUID());
            if (guild == null) {
                player.sendSystemMessage(Component.literal("Você precisa estar em uma organização para colocar o totem.").withStyle(ChatFormatting.RED));
                e.setCanceled(true);
                return;
            }
            if (guild.getMember(player.getUUID()).getRank() != GuildMember.Rank.LEADER) {
                player.sendSystemMessage(Component.literal("Apenas o líder pode colocar o totem.").withStyle(ChatFormatting.RED));
                e.setCanceled(true);
                return;
            }
            if (guild.getTotemPosition() != null) {
                player.sendSystemMessage(Component.literal("O totem da organização já está colocado.").withStyle(ChatFormatting.RED));
                e.setCanceled(true);
                return;
            }

            // Cannot place within 50-block radius of another guild's totem
            int minDistanceSq = 50 * 50;
            for (Guild other : manager.getAllGuilds()) {
                if (other.getTotemPosition() == null) continue;
                if (other.getName().equals(guild.getName())) continue;
                if (other.getTotemPosition().closerThan(pos, 50)) {
                    player.sendSystemMessage(Component.literal("Não é possível posicionar o totem tão perto de outra organização.").withStyle(ChatFormatting.RED));
                    e.setCanceled(true);
                    return;
                }
            }

            // Y-level validation
            int minY = GuildConfig.TOTEM_PLACE_MIN_Y.get();
            int maxY = GuildConfig.TOTEM_PLACE_MAX_Y.get();
            if (pos.getY() < minY || pos.getY() > maxY) {
                player.sendSystemMessage(Component.literal("A bandeira só pode ser colocada entre os níveis Y " + minY + " e " + maxY + ".").withStyle(ChatFormatting.RED));
                e.setCanceled(true);
                return;
            }

            guild.setTotemPosition(pos.immutable());
            manager.setDirty();

            // Clear surrounding blocks
            clearAreaAroundTotem(level, pos, GuildConfig.TOTEM_CLEARING_RADIUS.get());

            // Notify player
            player.sendSystemMessage(Component.literal("Totem posicionado! Território protegido criado.").withStyle(ChatFormatting.GREEN));
            return; // leaders can always place their own totem.
        }

        // -----------------------------------------------------------------
        // Other blocks – permission & proximity checks
        // -----------------------------------------------------------------

        Guild territoryGuild = manager.getGuildAt(pos);

        // Deny building too close to any totem (own or foreign)
        if (territoryGuild != null && territoryGuild.getTotemPosition() != null) {
            int radius = GuildConfig.TOTEM_CLEARING_RADIUS.get();
            int clearHeight = 3; // must match clearAreaAroundTotem
            BlockPos totem = territoryGuild.getTotemPosition();
            int dx = Math.abs(pos.getX() - totem.getX());
            int dz = Math.abs(pos.getZ() - totem.getZ());
            int dy = pos.getY() - totem.getY(); // only consider blocks at or above the totem Y (prevent sky blocking)
            if (dx <= radius && dz <= radius && dy >= 0 && dy <= clearHeight) {
                player.sendSystemMessage(Component.literal("Não é possível construir tão perto do totem.").withStyle(ChatFormatting.RED));
                e.setCanceled(true);
                return;
            }
        }

        // Standard territory permission check
        if (territoryGuild != null && !manager.canModifyTerritory(player.getUUID(), territoryGuild.getName())) {
            player.sendSystemMessage(Component.literal("Você não pode modificar blocos neste território.").withStyle(ChatFormatting.RED));
            e.setCanceled(true);
        }
    }

    // ---------------------------------------------------------------------
    // Block break permission
    // ---------------------------------------------------------------------

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent e) {
        if (!(e.getPlayer() instanceof ServerPlayer player)) return;
        ServerLevel level = player.serverLevel();
        GuildsManager manager = GuildsManager.get(level);
        BlockPos pos = e.getPos();
        // Handle totem break: only leader, remove territory
        if (e.getState().getBlock() == ModBlocks.TOTEM_BLOCK.get() || e.getState().getBlock() == ModBlocks.TOTEM_BLOCK_TOP.get()) {
            Guild guild = manager.getGuildByMember(player.getUUID());
            if (guild == null || guild.getTotemPosition() == null) {
                player.sendSystemMessage(Component.literal("Você não pode quebrar este totem.").withStyle(ChatFormatting.RED));
                e.setCanceled(true);
                return;
            }

            boolean isLeader = guild.getMember(player.getUUID()).getRank() == GuildMember.Rank.LEADER;
            if (!isLeader) {
                player.sendSystemMessage(Component.literal("Apenas o líder pode quebrar o totem.").withStyle(ChatFormatting.RED));
                e.setCanceled(true);
                return;
            }

            BlockPos bottomExpected = guild.getTotemPosition();
            BlockPos targetBottom = e.getState().getBlock() == ModBlocks.TOTEM_BLOCK_TOP.get() ? pos.below() : pos;
            if (!targetBottom.equals(bottomExpected)) {
                player.sendSystemMessage(Component.literal("Este não é o totem da sua organização.").withStyle(ChatFormatting.RED));
                e.setCanceled(true);
                return;
            }

            // Remove territory
            guild.setTotemPosition(null);
            manager.setDirty();
            player.sendSystemMessage(Component.literal("Totem removido. Território liberado.").withStyle(ChatFormatting.YELLOW));
            // allow block to break; top/bottom counterpart will be destroyed by block logic
            return;
        }
        Guild territoryGuild = manager.getGuildAt(pos);
        if (territoryGuild != null && !manager.canModifyTerritory(player.getUUID(), territoryGuild.getName())) {
            player.sendSystemMessage(Component.literal("Você não pode quebrar blocos neste território.").withStyle(ChatFormatting.RED));
            e.setCanceled(true);
        }
    }

    // ---------------------------------------------------------------------
    // Movement restriction inside enemy territory during war
    // ---------------------------------------------------------------------

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        Player player = e.player;
        if (!(player instanceof ServerPlayer sp)) return;
        ServerLevel level = sp.serverLevel();
        GuildsManager manager = GuildsManager.get(level);

        BlockPos pos = sp.blockPosition();
        Guild territoryGuild = manager.getGuildAt(pos);

        // Entry/leave messages ------------------------------------------
        String previousTag = LAST_TERRITORY.get(sp.getUUID());
        String currentTag = territoryGuild != null ? territoryGuild.getTag() : null;
        if (!java.util.Objects.equals(previousTag, currentTag)) {
            if (currentTag != null) {
                sp.sendSystemMessage(Component.literal("Você está no território de " + currentTag).withStyle(ChatFormatting.GOLD));
            } else if (previousTag != null) {
                sp.sendSystemMessage(Component.literal("Você saiu do território de " + previousTag).withStyle(ChatFormatting.YELLOW));
            }
            if (currentTag != null) LAST_TERRITORY.put(sp.getUUID(), currentTag); else LAST_TERRITORY.remove(sp.getUUID());
        }

        if (territoryGuild == null) return; // No territory – nothing else to check

        if (!manager.canEnterTerritory(sp.getUUID(), territoryGuild.getName())) {
            // push player out to nearest chunk edge (similar to original logic)
            int chunkX = pos.getX() >> 4;
            int chunkZ = pos.getZ() >> 4;
            double minX = (chunkX << 4);
            double maxX = minX + 16;
            double minZ = (chunkZ << 4);
            double maxZ = minZ + 16;
            double px = sp.getX();
            double pz = sp.getZ();
            double dxMin = px - minX;
            double dxMax = maxX - px;
            double dzMin = pz - minZ;
            double dzMax = maxZ - pz;

            double minDist = Math.min(Math.min(dxMin, dxMax), Math.min(dzMin, dzMax));
            double tpX = px;
            double tpZ = pz;
            if (minDist == dxMin) tpX = minX - 0.5;
            else if (minDist == dxMax) tpX = maxX + 0.5;
            else if (minDist == dzMin) tpZ = minZ - 0.5;
            else tpZ = maxZ + 0.5;

            sp.teleportTo(tpX, sp.getY(), tpZ);
            sp.sendSystemMessage(Component.literal("Você não pode entrar neste território.").withStyle(ChatFormatting.RED));
        }
    }
} 