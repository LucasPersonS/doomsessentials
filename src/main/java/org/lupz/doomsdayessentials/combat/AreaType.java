package org.lupz.doomsdayessentials.combat;

/**
 * Represents the two kinds of combat areas that can exist on the server.
 *
 * DANGER – PvP is enabled, extra HUD warnings are shown and players can be put into combat state.
 * SAFE   – PvP is disabled, players are protected from combat damage.
 */
public enum AreaType {
    DANGER,
    SAFE,
    FREQUENCY;

    public boolean isDanger() {
        return this == DANGER;
    }
} 