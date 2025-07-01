package org.lupz.doomsdayessentials.config;

import net.minecraftforge.common.ForgeConfigSpec;

public class EssentialsConfig {
    public static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec SPEC;

    // Combat System
    public static final ForgeConfigSpec.IntValue COMBAT_DURATION_SECONDS;
    public static final ForgeConfigSpec.ConfigValue<java.util.List<? extends String>> ALLOWED_COMBAT_COMMANDS;

    // Injury System
    public static final ForgeConfigSpec.BooleanValue INJURY_SYSTEM_ENABLED;
    public static final ForgeConfigSpec.IntValue MAX_INJURY_LEVEL;
    public static final ForgeConfigSpec.IntValue DEATHS_PER_INJURY_LEVEL;
    public static final ForgeConfigSpec.BooleanValue INJURY_PVP_ONLY;
    public static final ForgeConfigSpec.IntValue HEALING_BED_TIME_MINUTES;
    public static final ForgeConfigSpec.IntValue MEDICO_HEAL_AMOUNT;

    // Injury Effects
    public static final ForgeConfigSpec.BooleanValue ENABLE_SLOWNESS_EFFECT;
    public static final ForgeConfigSpec.BooleanValue ENABLE_MINING_FATIGUE_EFFECT;
    public static final ForgeConfigSpec.BooleanValue ENABLE_BLINDNESS_EFFECT;
    public static final ForgeConfigSpec.BooleanValue ENABLE_NAUSEA_EFFECT;
    public static final ForgeConfigSpec.BooleanValue ENABLE_WITHER_EFFECT;

    static {
        BUILDER.push("combat");
        COMBAT_DURATION_SECONDS = BUILDER
                .comment("Duration (in seconds) that a player remains in combat after PvP or leaving a danger zone.")
                .defineInRange("combatDurationSeconds", 15, 1, 600);
        ALLOWED_COMBAT_COMMANDS = BUILDER
                .comment("Commands that are allowed to be used while in combat. (e.g., 'msg', 'r')")
                .defineList("allowedCombatCommands", java.util.List.of("msg", "r"), s -> s instanceof String);
        BUILDER.pop();

        BUILDER.push("injury");
        INJURY_SYSTEM_ENABLED = BUILDER.comment("Enable or disable the injury system.")
                .define("injurySystemEnabled", true);
        MAX_INJURY_LEVEL = BUILDER.comment("Maximum injury level a player can reach.")
                .defineInRange("maxInjuryLevel", 5, 1, 100);
        DEATHS_PER_INJURY_LEVEL = BUILDER.comment("How many deaths are required to increase the injury level by one.")
                .defineInRange("deathsPerInjuryLevel", 3, 1, 100);
        INJURY_PVP_ONLY = BUILDER.comment("If true, only deaths from PvP will count towards injuries.")
                .define("injuryPvpOnly", false);
        HEALING_BED_TIME_MINUTES = BUILDER.comment("Time in minutes a player must sleep to heal one level of injury.")
                .defineInRange("healingBedTimeMinutes", 10, 1, 120);
        MEDICO_HEAL_AMOUNT = BUILDER.comment("How many injury levels a medic can heal with a medic kit.")
                .defineInRange("medicoHealAmount", 1, 1, 5);

        BUILDER.push("effects");
        ENABLE_SLOWNESS_EFFECT = BUILDER.define("enableSlowness", true);
        ENABLE_MINING_FATIGUE_EFFECT = BUILDER.define("enableMiningFatigue", true);
        ENABLE_BLINDNESS_EFFECT = BUILDER.define("enableBlindness", true);
        ENABLE_NAUSEA_EFFECT = BUILDER.define("enableNausea", true);
        ENABLE_WITHER_EFFECT = BUILDER.define("enableWither", true);
        BUILDER.pop();

        BUILDER.pop();
        SPEC = BUILDER.build();
    }
} 