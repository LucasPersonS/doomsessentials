package org.lupz.doomsdayessentials.guild;

import net.minecraftforge.common.ForgeConfigSpec;

/**
 * Configuration values for the guild (/organizacao) system.
 *
 * They are kept in a separate file (<modid>-guilds.toml) so server owners can
 * tweak guild mechanics without touching the rest of the Essentials config.
 */
public class GuildConfig {
    public static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec SPEC;

    // ---------------------------------------------------------------------
    // General guild settings
    // ---------------------------------------------------------------------
    public static final ForgeConfigSpec.IntValue MAX_GUILD_MEMBERS;
    public static final ForgeConfigSpec.IntValue MAX_ALLIANCES;
    public static final ForgeConfigSpec.LongValue GUILD_LEAVE_COOLDOWN_HOURS;
    public static final ForgeConfigSpec.IntValue TERRITORY_RADIUS_CHUNKS;

    // ---------------------------------------------------------------------
    // Totem settings
    // ---------------------------------------------------------------------
    public static final ForgeConfigSpec.IntValue TOTEM_PLACE_MIN_Y;
    public static final ForgeConfigSpec.IntValue TOTEM_PLACE_MAX_Y;
    public static final ForgeConfigSpec.IntValue TOTEM_CLEARING_RADIUS;

    // ---------------------------------------------------------------------
    // War settings
    // ---------------------------------------------------------------------
    public static final ForgeConfigSpec.LongValue WAR_DURATION_MINUTES;
    public static final ForgeConfigSpec.LongValue WAR_COOLDOWN_HOURS;
    public static final ForgeConfigSpec.IntValue WAR_MAX_DEATHS;
    public static final ForgeConfigSpec.IntValue WAR_MIN_ONLINE_DEFENDERS;
    public static final ForgeConfigSpec.IntValue WAR_MIN_ONLINE_ATTACKERS;
    public static final ForgeConfigSpec.BooleanValue INCLUDE_ALLIES_IN_WAR;
    public static final ForgeConfigSpec.IntValue PLUNDER_ITEM_COUNT;

    // ---------------------------------------------------------------------
    // Reinforced block settings
    // ---------------------------------------------------------------------
    public static final ForgeConfigSpec.DoubleValue REINFORCED_STONE_DURABILITY;
    public static final ForgeConfigSpec.DoubleValue PRIMAL_STEEL_DURABILITY;
    public static final ForgeConfigSpec.DoubleValue MOLTEN_STEEL_DURABILITY;
    public static final ForgeConfigSpec.DoubleValue GENERAL_EXPLOSION_DAMAGE;
    public static final ForgeConfigSpec.DoubleValue TACS_EXPLOSIVE_DAMAGE;

    static {
        // Guild section ----------------------------------------------------
        BUILDER.comment("Guild (organizacao) settings").push("guild");
        MAX_GUILD_MEMBERS = BUILDER.comment("Maximum number of members allowed in a single guild (0 = unlimited).")
                .defineInRange("maxGuildMembers", 10, 0, 1000);
        GUILD_LEAVE_COOLDOWN_HOURS = BUILDER.comment("Cooldown (in hours) before a new member can leave a guild.")
                .defineInRange("guildLeaveCooldownHours", 24L, 0L, 720L);
        TERRITORY_RADIUS_CHUNKS = BUILDER.comment("Radius (in chunks) defining a guild's protected territory around the totem. 1 = 3x3 chunks.")
                .defineInRange("territoryRadiusChunks", 1, 0, 10);

        MAX_ALLIANCES = BUILDER.comment("Maximum number of simultaneous alliances a guild can have (0 = unlimited).")
                .defineInRange("maxAlliances", 2, 0, 100);
        BUILDER.pop();

        // Totem section ----------------------------------------------------
        BUILDER.comment("Totem placement settings").push("totem");
        TOTEM_PLACE_MIN_Y = BUILDER.comment("Minimum Y-level where the totem can be placed.")
                .defineInRange("totemPlaceMinY", -20, -64, 320);
        TOTEM_PLACE_MAX_Y = BUILDER.comment("Maximum Y-level where the totem can be placed.")
                .defineInRange("totemPlaceMaxY", 80, -64, 320);
        TOTEM_CLEARING_RADIUS = BUILDER.comment("Radius (in blocks) of area cleared around a newly placed totem.")
                .defineInRange("totemClearingRadius", 3, 0, 16);
        BUILDER.pop();

        // War section ------------------------------------------------------
        BUILDER.comment("War / invasion settings").push("war");
        WAR_DURATION_MINUTES = BUILDER.comment("Duration (in minutes) of an active war.")
                .defineInRange("warDurationMinutes", 90L, 1L, 1440L);
        WAR_COOLDOWN_HOURS = BUILDER.comment("Cooldown (in hours) before a new war can be started in the same territory (also used for post-victory shield & failure pair-ban).")
                .defineInRange("warCooldownHours", 168L, 0L, 1680L);
        WAR_MAX_DEATHS = BUILDER.comment("Number of deaths allowed for a player during war before being locked out.")
                .defineInRange("warMaxDeaths", 2, 1, 100);
        WAR_MIN_ONLINE_DEFENDERS = BUILDER.comment("Minimum online defenders required to start a war.")
                .defineInRange("warMinOnlineDefenders", 5, 1, 50);
        WAR_MIN_ONLINE_ATTACKERS = BUILDER.comment("Minimum online attackers required to start a war.")
                .defineInRange("warMinOnlineAttackers", 5, 1, 50);
        INCLUDE_ALLIES_IN_WAR = BUILDER.comment("If true, all allies of the two main guilds will also enter war automatically.")
                .define("includeAlliesInWar", true);
        PLUNDER_ITEM_COUNT = BUILDER.comment("Number of random items to plunder from the defender on victory.")
                .defineInRange("plunderItemCount", 200, 1, 100000);
        BUILDER.pop();

        // Reinforced block section ---------------------------------------
        BUILDER.comment("Reinforced block durability & explosion damage").push("reinforcedBlocks");
        REINFORCED_STONE_DURABILITY = BUILDER.defineInRange("reinforcedStoneDurability", 10.0, 0.1, Double.MAX_VALUE);
        PRIMAL_STEEL_DURABILITY = BUILDER.defineInRange("primalSteelDurability", 25.0, 0.1, Double.MAX_VALUE);
        MOLTEN_STEEL_DURABILITY = BUILDER.defineInRange("moltenSteelDurability", 50.0, 0.1, Double.MAX_VALUE);
        GENERAL_EXPLOSION_DAMAGE = BUILDER.defineInRange("generalExplosionDamage", 1.0, 0.1, Double.MAX_VALUE);
        TACS_EXPLOSIVE_DAMAGE = BUILDER.defineInRange("tacsExplosiveDamage", 5.0, 0.1, Double.MAX_VALUE);
        BUILDER.pop();

        SPEC = BUILDER.build();
    }
} 