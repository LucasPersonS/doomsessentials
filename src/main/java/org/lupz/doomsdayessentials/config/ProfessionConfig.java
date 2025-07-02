package org.lupz.doomsdayessentials.config;

import net.minecraftforge.common.ForgeConfigSpec;

/**
 * Holds all configuration options related to the profession system (currently only Médico).
 */
public final class ProfessionConfig {

    private ProfessionConfig() {}

    public static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec SPEC;

    // General
    public static final ForgeConfigSpec.BooleanValue MEDICO_ENABLED;
    public static final ForgeConfigSpec.IntValue MEDICO_MAX_COUNT;

    public static final ForgeConfigSpec.BooleanValue MEDICO_ANNOUNCE_JOIN;
    public static final ForgeConfigSpec.BooleanValue MEDICO_AUTO_RECEIVE_KIT;

    public static final ForgeConfigSpec.DoubleValue MEDICO_HEAL_RANGE;
    public static final ForgeConfigSpec.IntValue MEDICO_HEAL_COOLDOWN_SECONDS;
    public static final ForgeConfigSpec.IntValue MEDICO_GLOBAL_COOLDOWN_MINUTES;

    public static final ForgeConfigSpec.IntValue MEDICO_BED_COOLDOWN_SECONDS;

    public static final ForgeConfigSpec.DoubleValue MEDICO_KIT_RANGE;
    public static final ForgeConfigSpec.IntValue MEDICO_KIT_HEAL_HEARTS;

    public static final ForgeConfigSpec.BooleanValue COMBATENTE_ENABLED;
    public static final ForgeConfigSpec.IntValue COMBATENTE_MAX_COUNT;

    public static final ForgeConfigSpec.BooleanValue RASTREADOR_ENABLED;
    public static final ForgeConfigSpec.IntValue RASTREADOR_MAX_COUNT;
    public static final ForgeConfigSpec.IntValue RASTREADOR_SCAN_COOLDOWN_MINUTES;
    public static final ForgeConfigSpec.DoubleValue RASTREADOR_SCAN_RADIUS;

    static {
        BUILDER.push("medico");

        MEDICO_ENABLED = BUILDER.comment("Enable or disable the medico profession.")
                .define("enabled", true);

        MEDICO_MAX_COUNT = BUILDER.comment("Maximum number of simultaneous medicos allowed on the server (0 = unlimited).")
                .defineInRange("maxCount", 5, 0, 1000);

        MEDICO_ANNOUNCE_JOIN = BUILDER.comment("If true, announces to all players when someone becomes a medico.")
                .define("announceJoin", true);

        MEDICO_AUTO_RECEIVE_KIT = BUILDER.comment("If true, gives a medical kit item when the player becomes a medico.")
                .define("autoReceiveKit", true);

        MEDICO_HEAL_RANGE = BUILDER.comment("Maximum distance (in blocks) at which a medico can heal another injured player.")
                .defineInRange("healRange", 6.0, 1.0, 32.0);

        MEDICO_HEAL_COOLDOWN_SECONDS = BUILDER.comment("Cooldown in seconds for the personal /medico heal ability.")
                .defineInRange("healCooldownSeconds", 30, 1, 3600);

        MEDICO_BED_COOLDOWN_SECONDS = BUILDER.comment("Cooldown in seconds for the /medico bed ability.")
                .defineInRange("bedCooldownSeconds", 60, 1, 3600);

        MEDICO_GLOBAL_COOLDOWN_MINUTES = BUILDER.comment("Global cooldown in minutes before the medico heal ability can be used again after any heal.")
                .defineInRange("globalCooldownMinutes", 5, 1, 1440);

        MEDICO_KIT_RANGE = BUILDER.comment("Maximum block distance for the Medic Kit healing ability.")
                .defineInRange("kitRange", 6.0, 1.0, 32.0);

        MEDICO_KIT_HEAL_HEARTS = BUILDER.comment("Number of hearts restored by a Medic Kit.")
                .defineInRange("kitHealHearts", 4, 1, 20);

        BUILDER.pop();

        BUILDER.push("combatente");
        COMBATENTE_ENABLED = BUILDER.define("enabled", true);
        COMBATENTE_MAX_COUNT = BUILDER.comment("Maximum number of simultaneous combatentes allowed on the server (0 = unlimited).")
                .defineInRange("maxCount", 10, 0, 1000);
        BUILDER.pop();

        BUILDER.push("rastreador");
        RASTREADOR_ENABLED = BUILDER.define("enabled", true);
        RASTREADOR_MAX_COUNT = BUILDER.defineInRange("maxCount", 5, 0, 1000);
        RASTREADOR_SCAN_COOLDOWN_MINUTES = BUILDER.comment("Cooldown (in minutes) for the tracker scan ability.")
                .defineInRange("scanCooldownMinutes", 60, 1, 1440);

        RASTREADOR_SCAN_RADIUS = BUILDER.comment("Radius (in blocks) of the scan ability that applies the glowing effect.")
                .defineInRange("scanRadius", 30.0, 5.0, 128.0);

        BUILDER.pop();

        SPEC = BUILDER.build();
    }
} 