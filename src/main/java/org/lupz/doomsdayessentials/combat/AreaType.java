package org.lupz.doomsdayessentials.combat;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;

/**
 * Represents the different kinds of combat areas that can exist on the server.
 *
 * DANGER   – PvP is always enabled, extra HUD warnings are shown and players can be put into combat state.
 * SAFE     – PvP is disabled, players are fully protected from combat damage.
 * NEUTRAL  – "Yellow" zone. PvP is disabled unless one or both players are currently in combat.
 * FREQUENCY – Special zone used by the Frequency effect mechanics.
 * RESOURCE – High-risk generator zone during a capture event. Becomes SAFE when conquistada.
 * ARENA    – Special zone for events. No combat tag, no injury on death, no item/XP loss.
 */
public enum AreaType implements StringRepresentable {
    /**
     * High-risk generator zone during a capture event. Becomes SAFE when conquistada.
     */
    RESOURCE,
    DANGER,
    SAFE,
    FREQUENCY,
    ARENA,
    NEUTRAL; // Yellow / Neutral zones – PvP off unless players are in combat

    public static final Codec<AreaType> CODEC = StringRepresentable.fromEnum(AreaType::values);

    public boolean isDanger() {
        return this == DANGER;
    }

    public boolean isArena() {
        return this == ARENA;
    }

    public boolean isResource() {
        return this == RESOURCE;
    }

    @Override
    public String getSerializedName() {
        return this.name().toLowerCase();
    }
} 