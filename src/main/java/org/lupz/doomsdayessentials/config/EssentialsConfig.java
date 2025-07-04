package org.lupz.doomsdayessentials.config;

import net.minecraftforge.common.ForgeConfigSpec;

public class EssentialsConfig {
    public static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec SPEC;

    // Combat System
    public static final ForgeConfigSpec.IntValue COMBAT_DURATION_SECONDS;
    public static final ForgeConfigSpec.ConfigValue<java.util.List<? extends String>> ALLOWED_COMBAT_COMMANDS;
    public static final ForgeConfigSpec.IntValue COMBAT_LOG_TIMER;
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
    public static final ForgeConfigSpec.IntValue MEDIC_HEAL_COOLDOWN;
    public static final ForgeConfigSpec.DoubleValue MEDIC_HEAL_RADIUS;
    public static final ForgeConfigSpec.DoubleValue MEDIC_HEAL_AMOUNT;

    // Professions.medico
    public static final ForgeConfigSpec.BooleanValue MEDICO_ENABLED;
    public static final ForgeConfigSpec.IntValue MEDICO_MAX_COUNT;
    public static final ForgeConfigSpec.BooleanValue MEDICO_ANNOUNCE_JOIN;
    public static final ForgeConfigSpec.BooleanValue MEDICO_AUTO_RECEIVE_KIT;
    public static final ForgeConfigSpec.IntValue MEDICO_BED_COOLDOWN_SECONDS;
    public static final ForgeConfigSpec.IntValue MEDICO_GLOBAL_COOLDOWN_MINUTES;

    // Professions.combatente
    public static final ForgeConfigSpec.BooleanValue COMBATENTE_ENABLED;
    public static final ForgeConfigSpec.IntValue COMBATENTE_MAX_COUNT;

    // Professions.rastreador
    public static final ForgeConfigSpec.BooleanValue RASTREADOR_ENABLED;
    public static final ForgeConfigSpec.IntValue RASTREADOR_MAX_COUNT;
    public static final ForgeConfigSpec.IntValue RASTREADOR_SCAN_COOLDOWN_MINUTES;
    public static final ForgeConfigSpec.DoubleValue RASTREADOR_SCAN_RADIUS;
    public static final ForgeConfigSpec.IntValue RASTREADOR_SCAN_DURATION_SECONDS;
    public static final ForgeConfigSpec.DoubleValue TRACKER_COMPASS_RADIUS;
    public static final ForgeConfigSpec.IntValue TRACKER_COMPASS_DURATION;
    public static final ForgeConfigSpec.IntValue TRACKER_COMPASS_COOLDOWN;

    // Shop Items
    public static final ForgeConfigSpec.ConfigValue<java.util.List<? extends String>> SHOP_ITEMS;

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
                .defineInRange("healingBedTimeMinutes", 1, 1, 120);
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
        MEDIC_HEAL_COOLDOWN = BUILDER.comment("The cooldown in seconds for the Medic's heal ability.")
                .defineInRange("medicHealCooldown", 60, 10, 300);
        MEDIC_HEAL_RADIUS = BUILDER.comment("The radius in blocks for the Medic's area of effect heal.")
                .defineInRange("medicHealRadius", 5.0, 1.0, 20.0);
        MEDIC_HEAL_AMOUNT = BUILDER.comment("The amount of health (in half hearts) the Medic's heal ability restores.")
                .defineInRange("medicHealAmount", 8.0, 1.0, 40.0);

        BUILDER.comment("Configurações para a profissão de Médico").push("medico");
        MEDICO_ENABLED = BUILDER.comment("Habilita ou desabilita a profissão de médico.")
                .define("medicoEnabled", true);
        MEDICO_MAX_COUNT = BUILDER.comment("Número máximo de médicos simultâneos permitidos no servidor (0 = ilimitado).")
                .defineInRange("medicoMaxCount", 5, 0, 1000);
        MEDICO_ANNOUNCE_JOIN = BUILDER.comment("Se verdadeiro, anuncia a todos os jogadores quando alguém se torna médico.")
                .define("medicoAnnounceJoin", true);
        MEDICO_AUTO_RECEIVE_KIT = BUILDER.comment("Se verdadeiro, entrega um kit médico quando o jogador se torna médico.")
                .define("medicoAutoReceiveKit", true);
        MEDICO_BED_COOLDOWN_SECONDS = BUILDER.comment("Cooldown em segundos para a habilidade /medico bed.")
                .defineInRange("medicoBedCooldownSeconds", 60, 1, 3600);
        MEDICO_GLOBAL_COOLDOWN_MINUTES = BUILDER.comment("Cooldown global em minutos antes que a habilidade de cura do médico possa ser usada novamente após qualquer cura.")
                .defineInRange("medicoGlobalCooldownMinutes", 5, 1, 1440);
        BUILDER.pop();

        BUILDER.comment("Configurações para a profissão de Combatente").push("combatente");
        COMBATENTE_ENABLED = BUILDER.comment("Habilita ou desabilita a profissão de combatente.")
                .define("combatenteEnabled", true);
        COMBATENTE_MAX_COUNT = BUILDER.comment("Número máximo de combatentes simultâneos permitidos no servidor (0 = ilimitado).")
                .defineInRange("combatenteMaxCount", 10, 0, 1000);
        BUILDER.pop();

        BUILDER.comment("Configurações para a profissão de Rastreador").push("rastreador");
        RASTREADOR_ENABLED = BUILDER.comment("Habilita ou desabilita a profissão de rastreador.")
                .define("rastreadorEnabled", true);
        RASTREADOR_MAX_COUNT = BUILDER.comment("Número máximo de rastreadores simultâneos permitidos no servidor (0 = ilimitado).")
                .defineInRange("rastreadorMaxCount", 5, 0, 1000);
        RASTREADOR_SCAN_COOLDOWN_MINUTES = BUILDER.comment("Cooldown (em minutos) para a habilidade de escaneamento do rastreador.")
                .defineInRange("rastreadorScanCooldownMinutes", 60, 1, 1440);
        RASTREADOR_SCAN_RADIUS = BUILDER.comment("Raio (em blocos) da habilidade de escaneamento que aplica o efeito de brilho.")
                .defineInRange("rastreadorScanRadius", 30.0, 5.0, 128.0);
        RASTREADOR_SCAN_DURATION_SECONDS = BUILDER.comment("Duração (em segundos) do efeito de brilho da habilidade de escaneamento.")
                .defineInRange("rastreadorScanDurationSeconds", 10, 5, 60);
        TRACKER_COMPASS_RADIUS = BUILDER.comment("The radius in blocks for the Tracking Compass's reveal effect.")
                .defineInRange("trackerCompassRadius", 50.0, 10.0, 200.0);
        TRACKER_COMPASS_DURATION = BUILDER.comment("The duration in seconds for the Tracking Compass's glowing effect.")
                .defineInRange("trackerCompassDuration", 10, 5, 60);
        TRACKER_COMPASS_COOLDOWN = BUILDER.comment("The cooldown in seconds for the Tracking Compass.")
                .defineInRange("trackerCompassCooldown", 30, 10, 300);
        BUILDER.pop();

        BUILDER.comment("Itens disponíveis na loja de profissões no formato outputId, costId, costAmount").push("shop");
        SHOP_ITEMS = BUILDER.defineList("items", java.util.List.of(
                "doomsdayessentials:medic_kit,apocalypsenow:scrapmetal,24",
                "doomsdayessentials:tracking_compass,apocalypsenow:scrap_metal,20",
                "doomsdayessentials:medical_bed,apocalypsenow:scrap_metal,100"
        ), o -> o instanceof String);
        BUILDER.pop();

        BUILDER.pop();

        BUILDER.push("Chat");
        LOCAL_CHAT_RADIUS = BUILDER.comment("The radius in blocks for local chat.")
                .defineInRange("localChatRadius", 16, 1, 256);
        BUILDER.pop();

        SPEC = BUILDER.build();
    }
} 