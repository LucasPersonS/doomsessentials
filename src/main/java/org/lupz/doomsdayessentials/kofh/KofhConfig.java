package org.lupz.doomsdayessentials.kofh;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.common.Mod;
import org.lupz.doomsdayessentials.EssentialsMod;

/**
 * Forge config for the King of the Hill (KOFH) system.
 * Allows server admins to tune gameplay and rewards.
 */
@Mod.EventBusSubscriber(modid = EssentialsMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class KofhConfig {
    public static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec SPEC;

    // Gameplay core
    public static final ForgeConfigSpec.IntValue TIMER_DURATION_SECONDS;
    public static final ForgeConfigSpec.DoubleValue POINTS_PER_SECOND;
    public static final ForgeConfigSpec.DoubleValue MULTIPLIER_MAX;
    public static final ForgeConfigSpec.IntValue MULTIPLIER_STEP_SECONDS;
    public static final ForgeConfigSpec.DoubleValue MULTIPLIER_STEP_AMOUNT;

    // Broadcast/packets
    public static final ForgeConfigSpec.IntValue HUD_UPDATE_INTERVAL_TICKS;

    // Zone list: predefined area names used for dynamic spawning/rotation
    public static final ForgeConfigSpec.ConfigValue<java.util.List<? extends String>> ZONE_NAMES;

    // Rewards
    public static final ForgeConfigSpec.ConfigValue<String> REWARD_RESOURCE_ID;
    public static final ForgeConfigSpec.IntValue REWARD_AMOUNT_WINNER;

    static {
        BUILDER.push("kofh");
        TIMER_DURATION_SECONDS = BUILDER.comment("Default KOFH round duration in seconds.")
                .defineInRange("timerDurationSeconds", 900, 30, 36000);
        POINTS_PER_SECOND = BUILDER.comment("Base points earned per second while holding the hill (before multiplier).")
                .defineInRange("pointsPerSecond", 1.0, 0.1, 100.0);
        MULTIPLIER_MAX = BUILDER.comment("Maximum uncontested hold multiplier.")
                .defineInRange("multiplierMax", 3.0, 1.0, 20.0);
        MULTIPLIER_STEP_SECONDS = BUILDER.comment("Number of consecutive uncontested seconds required to increase the multiplier by stepAmount.")
                .defineInRange("multiplierStepSeconds", 30, 1, 600);
        MULTIPLIER_STEP_AMOUNT = BUILDER.comment("Amount added to the multiplier each time stepSeconds threshold is reached.")
                .defineInRange("multiplierStepAmount", 0.25, 0.01, 5.0);

        HUD_UPDATE_INTERVAL_TICKS = BUILDER.comment("How often to send HUD scoreboard updates to clients (in ticks).")
                .defineInRange("hudUpdateIntervalTicks", 20, 1, 200);

        ZONE_NAMES = BUILDER.comment("Predefined ManagedArea names considered for KOFH objectives (rotation pool).")
                .defineList("zoneNames", java.util.Arrays.asList("HillA", "HillB", "HillC"), s -> s instanceof String);

        REWARD_RESOURCE_ID = BUILDER.comment("Guild resource ID to grant to the winning organization at round end.")
                .define("rewardResourceId", "scrapmetal");
        REWARD_AMOUNT_WINNER = BUILDER.comment("Amount of reward resource granted to the winning guild.")
                .defineInRange("rewardAmountWinner", 250, 1, 1000000);
        BUILDER.pop();
        SPEC = BUILDER.build();
    }

    private KofhConfig() {}
}

