package org.lupz.doomsdayessentials.professions;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lupz.doomsdayessentials.EssentialsMod;
import org.lupz.doomsdayessentials.block.ReinforcedBlock;
import org.lupz.doomsdayessentials.block.ReinforcedBlockEntity;
import org.lupz.doomsdayessentials.block.ModBlocks;
import org.lupz.doomsdayessentials.guild.Guild;
import org.lupz.doomsdayessentials.guild.GuildsManager;
import net.minecraftforge.event.level.BlockEvent;

/**
 * Extra interactions for engineers with their hammer inside own territory.
 */
@Mod.EventBusSubscriber(modid = EssentialsMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class EngineerBlockEvents {

    private EngineerBlockEvents() {}

    // Repair with empty hand (no item) on sneak + right-click
    @SubscribeEvent
    public static void onEngineerRepair(PlayerInteractEvent.RightClickBlock e) {
        Player player = e.getEntity();
        Level level = e.getLevel();
        if (level.isClientSide) return;
        ItemStack held = player.getMainHandItem();
        // Only allow empty hand for repair – engineer hammer removed
        if (!held.isEmpty()) return;
        if (!EngenheiroProfession.isEngineer(player)) return;
        if (!player.isShiftKeyDown()) return;

        BlockPos pos = e.getPos();
        BlockState state = level.getBlockState(pos);
        Block block = state.getBlock();
        if (!(block instanceof ReinforcedBlock)) return;

        GuildsManager gm = GuildsManager.get((net.minecraft.server.level.ServerLevel) level);
        Guild territoryGuild = gm.getGuildAt(pos);
        Guild playerGuild = gm.getGuildByMember(player.getUUID());
        if (playerGuild == null || territoryGuild == null || !playerGuild.getName().equals(territoryGuild.getName())) {
            return; // Not owner
        }
        if (gm.isTerritoryUnderWar(playerGuild.getName())) return; // Can't repair during invasion

        if (level.getBlockEntity(pos) instanceof ReinforcedBlockEntity rbe) {
            float before = rbe.getDurability();
            float target = ((ReinforcedBlock) block).getInitialDurability();
            if (before < target) {
                rbe.setDurability(target);
                player.displayClientMessage(net.minecraft.network.chat.Component.literal("§aBloco reparado."), true);
                e.setCanceled(true);
            }
        }
    }

    // Hammer functionality removed: no instant-break or special drop handling
} 
