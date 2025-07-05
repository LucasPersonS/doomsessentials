package org.lupz.doomsdayessentials.territory;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lupz.doomsdayessentials.EssentialsMod;
import org.lupz.doomsdayessentials.combat.AreaManager;
import org.lupz.doomsdayessentials.combat.AreaType;
import org.lupz.doomsdayessentials.combat.ManagedArea;
import org.lupz.doomsdayessentials.guild.Guild;
import org.lupz.doomsdayessentials.guild.GuildsManager;

import java.util.HashMap;
import java.util.Map;

/**
 * Kicks players que não pertencem à guilda dona (ou aliada) de áreas SAFE de geradores.
 */
@Mod.EventBusSubscriber(modid = EssentialsMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class TerritoryAccessEvents {

    /** Checks access for a single player */
    private static void checkAccess(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        ManagedArea area = AreaManager.get().getAreaAt(level, player.blockPosition());
        if (area == null || area.getType() != AreaType.SAFE) return;

        String owner = ResourceGeneratorManager.get().getOwner(area.getName());
        if (owner == null) return;
        if (player.hasPermissions(2)) return; // Admin bypass

        GuildsManager gm = GuildsManager.get(level);
        Guild pGuild = gm.getGuildByMember(player.getUUID());
        if (pGuild != null && (pGuild.getName().equals(owner) || gm.areAllied(pGuild.getName(), owner))) return;

        eject(player, area, owner);
    }

    /** Run once per dimension every 40 ticks (~2s) */
    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        if (e.level.isClientSide()) return;
        long gameTime = e.level.getGameTime();
        if (gameTime % 40 != 0) return;

        ServerLevel level = (ServerLevel) e.level;
        for (ServerPlayer player : level.players()) {
            checkAccess(player);
        }
    }

    private static void eject(ServerPlayer player, ManagedArea area, String ownerGuild) {
        final int OFFSET = 4;
        int newX = player.getBlockX();
        int newZ = player.getBlockZ();

        boolean insideX = player.getX() >= area.getPos1().getX() && player.getX() <= area.getPos2().getX();
        boolean insideZ = player.getZ() >= area.getPos1().getZ() && player.getZ() <= area.getPos2().getZ();

        if (insideX && insideZ) {
            int distLeft  = (int)(player.getX() - area.getPos1().getX());
            int distRight = (int)(area.getPos2().getX() - player.getX());
            int distNorth = (int)(player.getZ() - area.getPos1().getZ());
            int distSouth = (int)(area.getPos2().getZ() - player.getZ());
            int minDist = Math.min(Math.min(distLeft, distRight), Math.min(distNorth, distSouth));
            if (minDist == distLeft) newX = area.getPos1().getX() - OFFSET;
            else if (minDist == distRight) newX = area.getPos2().getX() + OFFSET;
            else if (minDist == distNorth) newZ = area.getPos1().getZ() - OFFSET;
            else newZ = area.getPos2().getZ() + OFFSET;
        } else if (insideX) {
            int distLeft  = (int)(player.getX() - area.getPos1().getX());
            int distRight = (int)(area.getPos2().getX() - player.getX());
            newX = (distLeft < distRight) ? area.getPos1().getX() - OFFSET : area.getPos2().getX() + OFFSET;
        } else if (insideZ) {
            int distNorth = (int)(player.getZ() - area.getPos1().getZ());
            int distSouth = (int)(area.getPos2().getZ() - player.getZ());
            newZ = (distNorth < distSouth) ? area.getPos1().getZ() - OFFSET : area.getPos2().getZ() + OFFSET;
        }

        player.teleportTo(newX + 0.5, player.getY(), newZ + 0.5);
        player.sendSystemMessage(Component.literal("§cÁrea privada da guilda §6" + ownerGuild));
    }
} 