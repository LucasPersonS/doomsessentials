package org.lupz.doomsdayessentials.airdrop;

import net.minecraftforge.common.ForgeConfigSpec;

public final class AirdropConfig {
    public static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec SPEC;

    public static final ForgeConfigSpec.DoubleValue FALL_SPEED;
    public static final ForgeConfigSpec.IntValue SPAWN_HEIGHT;

    static {
        BUILDER.push("airdrop");
        FALL_SPEED = BUILDER.comment("Fall speed per tick (0.005 - 0.08). Lower is slower; chicken-like is around 0.03")
                .defineInRange("fall_speed", 0.03, 0.005, 0.08);
        SPAWN_HEIGHT = BUILDER.comment("Default spawn Y height for /airdrop call command")
                .defineInRange("spawn_height", 200, 70, 384);
        BUILDER.pop();
        SPEC = BUILDER.build();
    }

    private AirdropConfig() {}
}
