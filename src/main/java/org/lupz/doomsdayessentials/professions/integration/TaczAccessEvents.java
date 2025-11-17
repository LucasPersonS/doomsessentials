package org.lupz.doomsdayessentials.professions.integration;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.network.PacketDistributor;
import org.lupz.doomsdayessentials.EssentialsMod;
import org.lupz.doomsdayessentials.airdrop.network.AirdropNoticePacket;
import org.lupz.doomsdayessentials.network.PacketHandler;
import org.lupz.doomsdayessentials.professions.ArmeiroProfession;

/**
 * Restricts access to TACZ workbenches to the Armeiro profession.
 *
 * Blocks:
 * - tacz:gun_smith_table
 * - tacz:workbench_b with BlockId == "ea:ammobench"
 * - tacz:workbench_c with BlockId == "tacz:attachment_workbench"
 *
 * Non-Armeiro players who try to access see a red typing overlay message.
 */
@Mod.EventBusSubscriber(modid = EssentialsMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class TaczAccessEvents {
    private TaczAccessEvents() {}

    @net.minecraftforge.eventbus.api.SubscribeEvent(priority = net.minecraftforge.eventbus.api.EventPriority.HIGHEST)
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        var level = event.getLevel();
        var player = event.getEntity();
        if (level == null || player == null) return;

        // Only enforce on server side to properly cancel and send S2C packet
        if (level.isClientSide) return;

        BlockPos pos = event.getPos();
        BlockState state = level.getBlockState(pos);
        Block block = state.getBlock();
        ResourceLocation key = ForgeRegistries.BLOCKS.getKey(block);
        if (key == null) return;

        // We primarily care about TACZ benches, but some tops may resolve to non-tacz blocks.
        // Do not early return on non-tacz; instead, probe block entities around the position.
        boolean isRestrictedWorkbench = false;
        String path = key.getPath();

        // Direct gun smith table and generic workbench name match (single-block or variant)
        if ("tacz".equals(key.getNamespace()) && ("gun_smith_table".equals(path) || path.contains("workbench") || path.contains("smith"))) {
            isRestrictedWorkbench = true;
        } else {
            // TACZ benches (ammo/attachment) use a two-block model – the BlockEntity can live on either half
            // Probe BE at the clicked position as well as below and above to find the bench id
            String benchId = getBenchIdFromNeighbors(level, pos);
            if (benchId != null) {
                // Restrict specific bench types regardless of which half was clicked
                if ("ea:ammobench".equals(benchId) || "tacz:attachment_workbench".equals(benchId)) {
                    isRestrictedWorkbench = true;
                }
            } else {
                // Fallback: top-half blocks may not have a BlockEntity – match by path
                // workbench_b* => ammo benches, workbench_c* => attachment benches
                if (("tacz".equals(key.getNamespace()) && (path.contains("workbench_b") || path.contains("workbench_c") || path.contains("workbench") || path.contains("smith"))) ||
                    isTaczBenchByBEType(level, pos)) {
                    isRestrictedWorkbench = true;
                }
            }
        }

        if (!isRestrictedWorkbench) return;

        // Allow only Armeiro to access
        if (!ArmeiroProfession.isArmeiro(player)) {
            event.setCanceled(true);
            // Thoroughly deny interaction in case some mods check these flags instead of cancellation only
            event.setUseBlock(net.minecraftforge.eventbus.api.Event.Result.DENY);
            event.setUseItem(net.minecraftforge.eventbus.api.Event.Result.DENY);
            event.setCancellationResult(net.minecraft.world.InteractionResult.FAIL);
            if (player instanceof ServerPlayer sp) {
                try {
                    PacketHandler.CHANNEL.send(PacketDistributor.PLAYER.with(() -> sp),
                            new AirdropNoticePacket("profession.armeiro.required", AirdropNoticePacket.STATE_DESPAWNED));
                } catch (Exception ignored) {}
            }
        }
    }

    /**
     * Read the bench BlockId from a BlockEntity if present at pos, below, or above.
     * TACZ benches store a "BlockId"/"block_id" tag indicating the specific bench type.
     */
    private static String getBenchIdFromNeighbors(net.minecraft.world.level.Level level, BlockPos pos) {
        // Try the clicked block entity first
        String id = getBenchId(level.getBlockEntity(pos));
        if (id != null) return id;
        // Then try the base (below) – common for double-height benches
        id = getBenchId(level.getBlockEntity(pos.below()));
        if (id != null) return id;
        // Finally try above – in case the BE is stored on the top half
        id = getBenchId(level.getBlockEntity(pos.above()));
        return id;
    }

    private static String getBenchId(BlockEntity be) {
        if (be == null) return null;
        try {
            CompoundTag full = be.saveWithFullMetadata();
            if (full == null) return null;
            if (full.contains("BlockId")) return full.getString("BlockId");
            if (full.contains("block_id")) return full.getString("block_id");
        } catch (Throwable ignored) {}
        return null;
    }

    /**
     * Extra heuristic: determine if this position is part of a TACZ bench by BlockEntity type.
     * Some top halves may resolve to a different namespace; checking BE type catches those.
     */
    private static boolean isTaczBenchByBEType(net.minecraft.world.level.Level level, BlockPos pos) {
        return isTaczBenchBE(level.getBlockEntity(pos)) ||
               isTaczBenchBE(level.getBlockEntity(pos.above())) ||
               isTaczBenchBE(level.getBlockEntity(pos.below()));
    }

    private static boolean isTaczBenchBE(BlockEntity be) {
        if (be == null) return false;
        try {
            var typeKey = ForgeRegistries.BLOCK_ENTITY_TYPES.getKey(be.getType());
            if (typeKey == null) return false;
            String ns = typeKey.getNamespace();
            String path = typeKey.getPath();
            // TACZ benches are registered under tacz namespace; ammo bench BE sometimes reports ea namespace
            return ("tacz".equals(ns) && (path.contains("workbench") || path.contains("smith"))) ||
                   ("ea".equals(ns) && path.contains("ammobench"));
        } catch (Throwable ignored) {}
        return false;
    }
}
