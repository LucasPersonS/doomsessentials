package org.lupz.doomsdayessentials.combat;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.lupz.doomsdayessentials.network.PacketHandler;
import org.lupz.doomsdayessentials.combat.AreaManager;
import org.lupz.doomsdayessentials.combat.AreaType;
import org.lupz.doomsdayessentials.network.packet.s2c.SyncCombatStatePacket;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Set;

/**
 * Tracks combat-tag state for each player.  A player is considered "in combat" for a certain
 * number of seconds after they deal or receive damage from another player.
 */
public class CombatManager {

    private static final CombatManager INSTANCE = new CombatManager();

    public static CombatManager get() {
        return INSTANCE;
    }

    // player UUID -> ticks remaining in combat
    private final Map<UUID, Integer> playersInCombat = new ConcurrentHashMap<>();
    private final Set<UUID> combatLoggers = ConcurrentHashMap.newKeySet();

    private CombatManager() {
        MinecraftForge.EVENT_BUS.register(this);
    }

    public boolean isInCombat(UUID uuid) {
        return playersInCombat.containsKey(uuid);
    }

    public int getRemainingTicks(UUID uuid) {
        return playersInCombat.getOrDefault(uuid, 0);
    }

    public void tagPlayer(ServerPlayer player) {
        playersInCombat.put(player.getUUID(), getDurationTicks());
        // No longer sends a direct packet, state is synced in onServerTick
    }

    private int getDurationTicks() {
        return org.lupz.doomsdayessentials.config.EssentialsConfig.COMBAT_DURATION_SECONDS.get() * 20;
    }

    public void clearCombat(UUID uuid) {
        playersInCombat.remove(uuid);
    }

    public void addCombatLogger(UUID uuid) {
        combatLoggers.add(uuid);
    }

    public boolean isCombatLogger(UUID uuid) {
        return combatLoggers.contains(uuid);
    }

    public void removeCombatLogger(UUID uuid) {
        combatLoggers.remove(uuid);
    }

    public Map<UUID, Integer> getPlayersInCombat() {
        return playersInCombat;
    }

    public int getDefaultDurationTicks() { return getDurationTicks(); }

    // ---------------------------------------------------------------------
    // Forge callbacks
    // ---------------------------------------------------------------------

    @SubscribeEvent
    public void onPlayerAttack(LivingAttackEvent event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer attacker)) return;
        if (!(event.getEntity() instanceof ServerPlayer victim)) return;

        // Don't tag players in creative or spectator mode
        if (attacker.isCreative() || attacker.isSpectator() || victim.isCreative() || victim.isSpectator()) {
            return;
        }

        // Any player-vs-player damage counts
        tagPlayer(attacker);
        tagPlayer(victim);
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        var server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;

        for (UUID uuid : playersInCombat.keySet()) {
            ServerPlayer player = server.getPlayerList().getPlayer(uuid);
            boolean inDangerZone = false;
            if (player != null) {
                var area = AreaManager.get().getAreaAt(player.serverLevel(), player.blockPosition());
                if (area != null && area.getType() == AreaType.DANGER) {
                    inDangerZone = true;
                }
            }

            if (inDangerZone) {
                // Forcefully reset the combat timer to the max value each tick.
                playersInCombat.put(uuid, getDurationTicks());
            } else {
                // Otherwise, tick down normally.
                playersInCombat.computeIfPresent(uuid, (k, v) -> v - 1);
            }
        }

        playersInCombat.entrySet().removeIf(e -> e.getValue() <= 0);

        // Broadcast the updated combat state to all players
        PacketHandler.CHANNEL.send(net.minecraftforge.network.PacketDistributor.ALL.noArg(), new SyncCombatStatePacket(playersInCombat));
    }
} 