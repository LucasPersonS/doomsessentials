package org.lupz.doomsdayessentials.territory;

/**
 * Configuration holder for KOFH territory event system. In the future this may be
 * wired to a Forge config file; for now it centralizes constants.
 */
public final class TerritoryEventConfig {
    private TerritoryEventConfig() {}

    // Maximum number of concurrent capture events allowed server-wide
    public static final int MAX_CONCURRENT_EVENTS = 2;

    // Interval in seconds for periodic progress broadcast messages (HUD packets are sent every second)
    public static final int BROADCAST_PROGRESS_INTERVAL_SECONDS = 30;
}
