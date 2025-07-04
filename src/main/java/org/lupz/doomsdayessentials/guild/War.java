package org.lupz.doomsdayessentials.guild;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Runtime object representing an active war between two guilds.
 */
public class War {
    private final String attackingGuildName;
    private final String defendingGuildName;
    private final long startTime;
    private final long durationMillis;

    private final Map<UUID, Integer> playerDeaths = new HashMap<>();
    private final Set<UUID> lockedOutPlayers = new HashSet<>();

    public War(String attackingGuildName, String defendingGuildName) {
        this.attackingGuildName = attackingGuildName;
        this.defendingGuildName = defendingGuildName;
        this.startTime = System.currentTimeMillis();
        this.durationMillis = GuildConfig.WAR_DURATION_MINUTES.get() * 60L * 1000L;
    }

    public String getAttackingGuildName() {
        return attackingGuildName;
    }

    public String getDefendingGuildName() {
        return defendingGuildName;
    }

    public boolean isWarOver() {
        return System.currentTimeMillis() > startTime + durationMillis;
    }

    public int recordPlayerDeath(UUID uuid) {
        int deaths = playerDeaths.getOrDefault(uuid, 0) + 1;
        playerDeaths.put(uuid, deaths);
        if (deaths >= GuildConfig.WAR_MAX_DEATHS.get()) {
            lockedOutPlayers.add(uuid);
        }
        return deaths;
    }

    public boolean isPlayerLockedOut(UUID uuid) {
        return lockedOutPlayers.contains(uuid);
    }
} 