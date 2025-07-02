package org.lupz.doomsdayessentials.combat;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;

/**
 * Represents the two kinds of combat areas that can exist on the server.
 *
 * DANGER – PvP is enabled, extra HUD warnings are shown and players can be put into combat state.
 * SAFE   – PvP is disabled, players are protected from combat damage.
 */
public enum AreaType implements StringRepresentable {
    DANGER,
    SAFE,
    FREQUENCY;

    public static final Codec<AreaType> CODEC = StringRepresentable.fromEnum(AreaType::values);

    public boolean isDanger() {
        return this == DANGER;
    }

    @Override
    public String getSerializedName() {
        return this.name().toLowerCase();
    }
} 