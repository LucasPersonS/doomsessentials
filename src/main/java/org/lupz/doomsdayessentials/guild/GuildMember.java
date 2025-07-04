package org.lupz.doomsdayessentials.guild;

import java.util.UUID;

/**
 * Represents a single player in a guild – keeps track of their rank and when
 * they joined (for leave-cooldowns).
 */
public class GuildMember {

    public enum Rank {
        LEADER,
        OFFICER,
        MEMBER
    }

    private final UUID playerUUID;
    private Rank rank;
    private final long joinTimestamp;

    public GuildMember(UUID playerUUID, Rank rank) {
        this.playerUUID = playerUUID;
        this.rank = rank;
        this.joinTimestamp = System.currentTimeMillis();
    }

    // ---------------------------------------------------------------------
    // Accessors
    // ---------------------------------------------------------------------

    public UUID getPlayerUUID() {
        return playerUUID;
    }

    public Rank getRank() {
        return rank;
    }

    public void setRank(Rank rank) {
        this.rank = rank;
    }

    public long getJoinTimestamp() {
        return joinTimestamp;
    }

    // ---------------------------------------------------------------------
    // Helper methods
    // ---------------------------------------------------------------------

    public boolean canLeave() {
        long cooldownMs = GuildConfig.GUILD_LEAVE_COOLDOWN_HOURS.get() * 60L * 60L * 1000L;
        return System.currentTimeMillis() - joinTimestamp >= cooldownMs;
    }

    public long getTimeUntilCanLeave() {
        long cooldownMs = GuildConfig.GUILD_LEAVE_COOLDOWN_HOURS.get() * 60L * 60L * 1000L;
        long timePassed = System.currentTimeMillis() - joinTimestamp;
        return Math.max(0L, cooldownMs - timePassed);
    }
} 