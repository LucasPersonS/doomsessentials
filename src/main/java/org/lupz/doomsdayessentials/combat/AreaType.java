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
 */
public enum AreaType implements StringRepresentable {
    DANGER,
    SAFE,
    FREQUENCY,
    NEUTRAL; // Yellow / Neutral zones – PvP off unless players are in combat

    public static final Codec<AreaType> CODEC = StringRepresentable.fromEnum(AreaType::values);

    public boolean isDanger() {
        return this == DANGER;
    }

    @Override
    public String getSerializedName() {
        return this.name().toLowerCase();
    }
} 