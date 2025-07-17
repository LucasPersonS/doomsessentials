package org.lupz.doomsdayessentials.professions.shop;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.common.Mod;
import org.lupz.doomsdayessentials.EssentialsMod;

@Mod.EventBusSubscriber(modid = EssentialsMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class EngineerConfig {
    public static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec SPEC;

    public static final ForgeConfigSpec.BooleanValue ENGENHEIRO_ENABLED;
    public static final ForgeConfigSpec.IntValue ENGENHEIRO_MAX_COUNT;
    public static final ForgeConfigSpec.IntValue ENGENHEIRO_BARRIER_COOLDOWN_SECONDS;

    static {
        BUILDER.push("engenheiro");
        ENGENHEIRO_ENABLED = BUILDER.comment("Enable or disable the engineer profession.")
                .define("engenheiroEnabled", true);
        ENGENHEIRO_MAX_COUNT = BUILDER.comment("Maximum number of simultaneous engineers allowed on the server (0 = unlimited).")
                .defineInRange("engenheiroMaxCount", 5, 0, 1000);
        ENGENHEIRO_BARRIER_COOLDOWN_SECONDS = BUILDER.comment("Cooldown in seconds for the /engineer barrier ability.")
                .defineInRange("engenheiroBarrierCooldownSeconds", 60, 1, 3600);
        BUILDER.pop();
        SPEC = BUILDER.build();
    }
} 