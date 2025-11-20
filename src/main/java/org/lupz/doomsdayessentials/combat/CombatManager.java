package org.lupz.doomsdayessentials.combat;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.lupz.doomsdayessentials.network.PacketHandler;
import org.lupz.doomsdayessentials.network.packet.s2c.SyncCombatStatePacket;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Set;

/**
 * Tracks combat-tag state for each player. A player is considered "in combat"
 * for a certain
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

    // Players that opted-in to permanent combat mode via /combat activate
    private final Set<UUID> alwaysActive = ConcurrentHashMap.newKeySet();

    // Throttle broadcast to clients: send at most every 5 ticks (4 Hz)
    private int syncTickCounter = 0;

    private final Set<UUID> wantedPlayers = ConcurrentHashMap.newKeySet();
    private final Map<UUID, Long> wantedUntil = new ConcurrentHashMap<>();
    private static final java.io.File WANTED_FILE = new java.io.File("wanted_players.json");
    private static final com.google.gson.Gson GSON = new com.google.gson.GsonBuilder().setPrettyPrinting().create();

    private CombatManager() {
        MinecraftForge.EVENT_BUS.register(this);
    }

    public boolean isInCombat(UUID uuid) {
        return alwaysActive.contains(uuid) || playersInCombat.containsKey(uuid);
    }

    public boolean isAlwaysActive(UUID uuid) {
        return alwaysActive.contains(uuid);
    }

    public void setAlwaysActive(UUID uuid, boolean active) {
        if (active) {
            alwaysActive.add(uuid);
            // Store -1 sentinel so client knows it's permanent and hides countdown
            playersInCombat.put(uuid, -1);
        } else {
            alwaysActive.remove(uuid);
            playersInCombat.remove(uuid);
        }
    }

    public int getRemainingTicks(UUID uuid) {
        return playersInCombat.getOrDefault(uuid, 0);
    }

    public void tagPlayer(ServerPlayer player) {
        if (alwaysActive.contains(player.getUUID())) {
            playersInCombat.put(player.getUUID(), -1);
        } else {
            playersInCombat.put(player.getUUID(), getDurationTicks());
        }
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

    public int getDefaultDurationTicks() {
        return getDurationTicks();
    }

    // ---------------------------------------------------------------------
    // Wanted System
    // ---------------------------------------------------------------------

    public void addWanted(UUID uuid) {
        addWanted(uuid, 30 * 60);
    }

    public void addWanted(UUID uuid, int durationSeconds) {
        long until = System.currentTimeMillis() + durationSeconds * 1000L;
        wantedPlayers.add(uuid);
        wantedUntil.put(uuid, until);
        saveWantedList();
        syncWantedState();
    }

    public void removeWanted(UUID uuid) {
        boolean changed = wantedPlayers.remove(uuid) | (wantedUntil.remove(uuid) != null);
        if (changed) {
            saveWantedList();
            syncWantedState();
        }
    }

    public boolean isWanted(UUID uuid) {
        return wantedPlayers.contains(uuid);
    }

    public Set<UUID> getWantedPlayers() {
        return wantedPlayers;
    }

    public int getWantedRemainingSeconds(UUID uuid) {
        Long until = wantedUntil.get(uuid);
        if (until == null) return 0;
        long now = System.currentTimeMillis();
        long ms = Math.max(0, until - now);
        return (int) (ms / 1000L);
    }

    private void syncWantedState() {
        PacketHandler.CHANNEL.send(net.minecraftforge.network.PacketDistributor.ALL.noArg(),
                new SyncCombatStatePacket(playersInCombat, wantedPlayers));
    }

    private void saveWantedList() {
        try (java.io.Writer writer = new java.io.FileWriter(WANTED_FILE)) {
            java.util.Map<String, Object> root = new java.util.HashMap<>();
            java.util.List<String> ids = wantedPlayers.stream().map(java.util.UUID::toString).toList();
            java.util.Map<String, Long> untils = new java.util.HashMap<>();
            wantedUntil.forEach((k,v) -> untils.put(k.toString(), v));
            root.put("players", ids);
            root.put("until", untils);
            GSON.toJson(root, writer);
        } catch (java.io.IOException e) {
            e.printStackTrace();
        }
    }

    public void loadWantedList() {
        if (!WANTED_FILE.exists()) return;
        try (java.io.Reader reader = new java.io.FileReader(WANTED_FILE)) {
            com.google.gson.JsonObject obj = com.google.gson.JsonParser.parseReader(reader).getAsJsonObject();
            java.util.List<String> ids = new java.util.ArrayList<>();
            if (obj.has("players") && obj.get("players").isJsonArray()) {
                obj.get("players").getAsJsonArray().forEach(e -> ids.add(e.getAsString()));
            }
            java.util.Map<String, Long> untils = new java.util.HashMap<>();
            if (obj.has("until") && obj.get("until").isJsonObject()) {
                for (var entry : obj.get("until").getAsJsonObject().entrySet()) {
                    untils.put(entry.getKey(), entry.getValue().getAsLong());
                }
            }
            for (String s : ids) {
                try {
                    java.util.UUID id = java.util.UUID.fromString(s);
                    wantedPlayers.add(id);
                    Long u = untils.get(s);
                    if (u != null) wantedUntil.put(id, u);
                } catch (Throwable ignored) {}
            }
        } catch (java.io.IOException e) {
            e.printStackTrace();
        }
    }

    // ---------------------------------------------------------------------
    // Forge callbacks
    // ---------------------------------------------------------------------

    @SubscribeEvent
    public void onPlayerAttack(LivingAttackEvent event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer attacker))
            return;
        if (!(event.getEntity() instanceof ServerPlayer victim))
            return;

        // Don't tag players in creative or spectator mode
        if (attacker.isCreative() || attacker.isSpectator() || victim.isCreative() || victim.isSpectator()) {
            return;
        }

        // Check if either player is in an ARENA zone
        var attackerArea = AreaManager.get().getAreaAt(attacker.serverLevel(), attacker.blockPosition());
        var victimArea = AreaManager.get().getAreaAt(victim.serverLevel(), victim.blockPosition());
        if ((attackerArea != null && attackerArea.getType() == AreaType.ARENA) ||
                (victimArea != null && victimArea.getType() == AreaType.ARENA)) {
            return;
        }

        // Any player-vs-player damage counts
        tagPlayer(attacker);
        tagPlayer(victim);
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END)
            return;

        var server = ServerLifecycleHooks.getCurrentServer();
        if (server == null)
            return;

        for (UUID uuid : playersInCombat.keySet()) {
            ServerPlayer player = server.getPlayerList().getPlayer(uuid);
            boolean inDangerZone = false;
            if (player != null) {
                var area = AreaManager.get().getAreaAt(player.serverLevel(), player.blockPosition());
                if (area != null && area.getType() == AreaType.DANGER) {
                    inDangerZone = true;
                }
            }

            if (alwaysActive.contains(uuid)) {
                // Maintain sentinel -1
                playersInCombat.put(uuid, -1);
                continue;
            }

            if (inDangerZone) {
                // Forcefully reset the combat timer to the max value each tick.
                playersInCombat.put(uuid, getDurationTicks());
            } else {
                // Otherwise, tick down normally.
                playersInCombat.computeIfPresent(uuid, (k, v) -> v - 1);
            }
        }

        playersInCombat.entrySet().removeIf(e -> !alwaysActive.contains(e.getKey()) && e.getValue() <= 0);

        // Throttle broadcast: send only once every 5 ticks (~4 times per second)
        syncTickCounter++;
        if (syncTickCounter % 5 != 0)
            return;

        // Broadcast the updated combat state to all players
        PacketHandler.CHANNEL.send(net.minecraftforge.network.PacketDistributor.ALL.noArg(),
                new SyncCombatStatePacket(playersInCombat, wantedPlayers));
    }

    @SubscribeEvent
    public void onServerTickWantedCleanup(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        long now = System.currentTimeMillis();
        wantedPlayers.removeIf(uuid -> {
            Long until = wantedUntil.get(uuid);
            if (until != null && now >= until) {
                wantedUntil.remove(uuid);
                return true;
            }
            return false;
        });
    }
}
