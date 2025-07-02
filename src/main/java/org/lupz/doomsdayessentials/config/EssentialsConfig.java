package org.lupz.doomsdayessentials.config;

import net.minecraftforge.common.ForgeConfigSpec;

public class EssentialsConfig {
    public static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec SPEC;

    // Combat System
    public static final ForgeConfigSpec.IntValue COMBAT_DURATION_SECONDS;
    public static final ForgeConfigSpec.ConfigValue<java.util.List<? extends String>> ALLOWED_COMBAT_COMMANDS;
    public static final ForgeConfigSpec.IntValue COMBAT_LOG_TIMER;
    public static final ForgeConfigSpec.DoubleValue HEAL_RANGE;
    public static final ForgeConfigSpec.DoubleValue HEAL_AMOUNT;
    public static final ForgeConfigSpec.IntValue LOCAL_CHAT_RADIUS;

    // Injury System
    public static final ForgeConfigSpec.BooleanValue INJURY_SYSTEM_ENABLED;
    public static final ForgeConfigSpec.IntValue MAX_INJURY_LEVEL;
    public static final ForgeConfigSpec.IntValue DEATHS_PER_INJURY_LEVEL;
    public static final ForgeConfigSpec.BooleanValue INJURY_PVP_ONLY;
    public static final ForgeConfigSpec.IntValue HEALING_BED_TIME_MINUTES;
    public static final ForgeConfigSpec.IntValue MEDICO_HEAL_AMOUNT;
    public static final ForgeConfigSpec.DoubleValue DOWNED_HEALTH_POOL;
    public static final ForgeConfigSpec.IntValue DOWNED_BLEED_OUT_SECONDS;
    public static final ForgeConfigSpec.BooleanValue RESET_INJURY_ON_DEATH;

    // Injury Effects
    public static final ForgeConfigSpec.BooleanValue ENABLE_SLOWNESS_EFFECT;
    public static final ForgeConfigSpec.BooleanValue ENABLE_MINING_FATIGUE_EFFECT;
    public static final ForgeConfigSpec.BooleanValue ENABLE_BLINDNESS_EFFECT;
    public static final ForgeConfigSpec.BooleanValue ENABLE_NAUSEA_EFFECT;
    public static final ForgeConfigSpec.BooleanValue ENABLE_WITHER_EFFECT;

    // Professions
    public static final ForgeConfigSpec.IntValue MEDIC_KIT_COOLDOWN;
    public static final ForgeConfigSpec.DoubleValue MEDIC_KIT_RADIUS;
    public static final ForgeConfigSpec.DoubleValue MEDIC_KIT_HEAL_AMOUNT;

    // Professions.tracker
    public static final ForgeConfigSpec.DoubleValue TRACKER_COMPASS_RADIUS;
    public static final ForgeConfigSpec.IntValue TRACKER_COMPASS_DURATION;
    public static final ForgeConfigSpec.IntValue TRACKER_COMPASS_COOLDOWN;

    static {
        BUILDER.push("combat");
        COMBAT_DURATION_SECONDS = BUILDER
                .comment("Duration (in seconds) that a player remains in combat after PvP or leaving a danger zone.")
                .defineInRange("combatDurationSeconds", 15, 1, 600);
        ALLOWED_COMBAT_COMMANDS = BUILDER
                .comment("Commands that are allowed to be used while in combat. (e.g., 'msg', 'r')")
                .defineList("allowedCombatCommands", java.util.List.of("msg", "r"), s -> s instanceof String);
        COMBAT_LOG_TIMER = BUILDER.comment("The duration (in seconds) that combat logs are visible.")
                .defineInRange("combatLogTimer", 30, 1, 300);
        HEAL_RANGE = BUILDER.comment("The range in blocks a medic's special heal can reach.")
                .defineInRange("healRange", 5.0, 1.0, 20.0);
        HEAL_AMOUNT = BUILDER.comment("The amount of health a medic's special heal restores.")
                .defineInRange("healAmount", 4.0, 1.0, 20.0);
        BUILDER.pop();

        BUILDER.push("injury");
        INJURY_SYSTEM_ENABLED = BUILDER.comment("Enable or disable the injury system.")
                .define("injurySystemEnabled", true);
        MAX_INJURY_LEVEL = BUILDER.comment("Maximum injury level a player can reach.")
                .defineInRange("maxInjuryLevel", 5, 1, 100);
        DEATHS_PER_INJURY_LEVEL = BUILDER
                .comment("How many deaths are required to increase injury level by 1.")
                .defineInRange("deathsPerInjuryLevel", 5, 1, 100);
        INJURY_PVP_ONLY = BUILDER.comment("If true, only deaths from PvP will count towards injuries.")
                .define("injuryPvpOnly", false);
        HEALING_BED_TIME_MINUTES = BUILDER.comment("Time in minutes a player must sleep to heal one level of injury.")
                .defineInRange("healingBedTimeMinutes", 10, 1, 120);
        MEDICO_HEAL_AMOUNT = BUILDER.comment("How many injury levels a medic can heal with a medic kit.")
                .defineInRange("medicoHealAmount", 1, 1, 5);
        DOWNED_HEALTH_POOL = BUILDER
                .comment("How much damage a downed player can take before dying. Roughly 5 unarmed hits.")
                .defineInRange("downedHealthPool", 20.0, 1.0, 1000.0);
        DOWNED_BLEED_OUT_SECONDS = BUILDER
                .comment("How long (in seconds) a player can be downed before they bleed out and die.")
                .defineInRange("downedBleedOutSeconds", 180, 10, 600);
        RESET_INJURY_ON_DEATH = BUILDER.comment("If true, a player's injury level will be reset to 0 upon death.").define("resetInjuryOnDeath", false);
        BUILDER.pop();

        BUILDER.push("effects");
        ENABLE_SLOWNESS_EFFECT = BUILDER.define("enableSlowness", true);
        ENABLE_MINING_FATIGUE_EFFECT = BUILDER.define("enableMiningFatigue", true);
        ENABLE_BLINDNESS_EFFECT = BUILDER.define("enableBlindness", true);
        ENABLE_NAUSEA_EFFECT = BUILDER.define("enableNausea", true);
        ENABLE_WITHER_EFFECT = BUILDER.define("enableWither", true);
        BUILDER.pop();

        BUILDER.push("professions");
        MEDIC_KIT_COOLDOWN = BUILDER.comment("The cooldown in seconds for the Medic Kit.")
                .defineInRange("medicKitCooldown", 60, 10, 300);
        MEDIC_KIT_RADIUS = BUILDER.comment("The radius in blocks for the Medic Kit's area of effect heal.")
                .defineInRange("medicKitRadius", 5.0, 1.0, 20.0);
        MEDIC_KIT_HEAL_AMOUNT = BUILDER.comment("The amount of health (in half hearts) the Medic Kit heals.")
                .defineInRange("medicKitHealAmount", 8.0, 1.0, 40.0);
        BUILDER.pop();

        BUILDER.push("professions.tracker");
        TRACKER_COMPASS_RADIUS = BUILDER.comment("The radius in blocks for the Tracking Compass's reveal effect.")
                .defineInRange("trackerCompassRadius", 50.0, 10.0, 200.0);
        TRACKER_COMPASS_DURATION = BUILDER.comment("The duration in seconds for the Tracking Compass's glowing effect.")
                .defineInRange("trackerCompassDuration", 10, 5, 60);
        TRACKER_COMPASS_COOLDOWN = BUILDER.comment("The cooldown in seconds for the Tracking Compass.")
                .defineInRange("trackerCompassCooldown", 30, 10, 300);
        BUILDER.pop();

        BUILDER.push("Chat");
        LOCAL_CHAT_RADIUS = BUILDER.comment("The radius in blocks for local chat.")
                .defineInRange("localChatRadius", 16, 1, 256);
        BUILDER.pop();

        SPEC = BUILDER.build();
    }
} 