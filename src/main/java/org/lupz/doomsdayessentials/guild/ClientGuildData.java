package org.lupz.doomsdayessentials.guild;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Stores client-side preferences for each player (currently only the "mapa"
 * overlay toggle).
 *
 * In a dedicated server environment this lives only in memory and resets on
 * restart; that is fine until we implement a proper client packet.
 */
public class ClientGuildData {
    private static final Set<UUID> PLAYERS_WITH_MAP_ON = new HashSet<>();

    public static boolean toggleMap(UUID uuid) {
        if (PLAYERS_WITH_MAP_ON.contains(uuid)) {
            PLAYERS_WITH_MAP_ON.remove(uuid);
            return false;
        } else {
            PLAYERS_WITH_MAP_ON.add(uuid);
            return true;
        }
    }

    public static boolean isMapOn(UUID uuid) {
        return PLAYERS_WITH_MAP_ON.contains(uuid);
    }
} 