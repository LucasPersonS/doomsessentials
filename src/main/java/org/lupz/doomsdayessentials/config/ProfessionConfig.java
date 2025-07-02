package org.lupz.doomsdayessentials.config;

import net.minecraftforge.common.ForgeConfigSpec;

/**
 * Holds all configuration options related to the profession system.
 */
public final class ProfessionConfig {

    private ProfessionConfig() {}

    public static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec SPEC;

    // Medico
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

    // Combatente
    public static final ForgeConfigSpec.BooleanValue COMBATENTE_ENABLED;
    public static final ForgeConfigSpec.IntValue COMBATENTE_MAX_COUNT;

    // Rastreador
    public static final ForgeConfigSpec.BooleanValue RASTREADOR_ENABLED;
    public static final ForgeConfigSpec.IntValue RASTREADOR_MAX_COUNT;
    public static final ForgeConfigSpec.IntValue RASTREADOR_SCAN_COOLDOWN_MINUTES;
    public static final ForgeConfigSpec.DoubleValue RASTREADOR_SCAN_RADIUS;

    static {
        BUILDER.comment("Configurações para as profissões").push("professions");

        BUILDER.comment("Configurações para a profissão de Médico").push("medico");
        MEDICO_ENABLED = BUILDER.comment("Habilita ou desabilita a profissão de médico.")
                .define("enabled", true);
        MEDICO_MAX_COUNT = BUILDER.comment("Número máximo de médicos simultâneos permitidos no servidor (0 = ilimitado).")
                .defineInRange("maxCount", 5, 0, 1000);
        MEDICO_ANNOUNCE_JOIN = BUILDER.comment("Se verdadeiro, anuncia a todos os jogadores quando alguém se torna médico.")
                .define("announceJoin", true);
        MEDICO_AUTO_RECEIVE_KIT = BUILDER.comment("Se verdadeiro, entrega um kit médico quando o jogador se torna médico.")
                .define("autoReceiveKit", true);
        MEDICO_HEAL_RANGE = BUILDER.comment("Distância máxima (em blocos) em que um médico pode curar outro jogador ferido.")
                .defineInRange("healRange", 6.0, 1.0, 32.0);
        MEDICO_HEAL_COOLDOWN_SECONDS = BUILDER.comment("Cooldown em segundos para a habilidade pessoal de cura /medico heal.")
                .defineInRange("healCooldownSeconds", 30, 1, 3600);
        MEDICO_BED_COOLDOWN_SECONDS = BUILDER.comment("Cooldown em segundos para a habilidade /medico bed.")
                .defineInRange("bedCooldownSeconds", 60, 1, 3600);
        MEDICO_GLOBAL_COOLDOWN_MINUTES = BUILDER.comment("Cooldown global em minutos antes que a habilidade de cura do médico possa ser usada novamente após qualquer cura.")
                .defineInRange("globalCooldownMinutes", 5, 1, 1440);
        MEDICO_KIT_RANGE = BUILDER.comment("Distância máxima em blocos para a habilidade de cura do Kit Médico.")
                .defineInRange("kitRange", 6.0, 1.0, 32.0);
        MEDICO_KIT_HEAL_HEARTS = BUILDER.comment("Número de corações restaurados por um Kit Médico.")
                .defineInRange("kitHealHearts", 4, 1, 20);
        BUILDER.pop();

        BUILDER.comment("Configurações para a profissão de Combatente").push("combatente");
        COMBATENTE_ENABLED = BUILDER.comment("Habilita ou desabilita a profissão de combatente.")
                .define("enabled", true);
        COMBATENTE_MAX_COUNT = BUILDER.comment("Número máximo de combatentes simultâneos permitidos no servidor (0 = ilimitado).")
                .defineInRange("maxCount", 10, 0, 1000);
        BUILDER.pop();

        BUILDER.comment("Configurações para a profissão de Rastreador").push("rastreador");
        RASTREADOR_ENABLED = BUILDER.comment("Habilita ou desabilita a profissão de rastreador.")
                .define("enabled", true);
        RASTREADOR_MAX_COUNT = BUILDER.comment("Número máximo de rastreadores simultâneos permitidos no servidor (0 = ilimitado).")
                .defineInRange("maxCount", 5, 0, 1000);
        RASTREADOR_SCAN_COOLDOWN_MINUTES = BUILDER.comment("Cooldown (em minutos) para a habilidade de escaneamento do rastreador.")
                .defineInRange("scanCooldownMinutes", 60, 1, 1440);
        RASTREADOR_SCAN_RADIUS = BUILDER.comment("Raio (em blocos) da habilidade de escaneamento que aplica o efeito de brilho.")
                .defineInRange("scanRadius", 30.0, 5.0, 128.0);
        BUILDER.pop();

        BUILDER.pop();
        SPEC = BUILDER.build();
    }
} 