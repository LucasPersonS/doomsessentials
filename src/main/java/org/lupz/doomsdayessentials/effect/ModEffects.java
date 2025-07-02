package org.lupz.doomsdayessentials.effect;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;
import org.lupz.doomsdayessentials.EssentialsMod;

public final class ModEffects {

    public static final DeferredRegister<MobEffect> EFFECTS =
            DeferredRegister.create(Registries.MOB_EFFECT, EssentialsMod.MOD_ID);

    public static final RegistryObject<MobEffect> FREQUENCY = EFFECTS.register("frequencia", FrequencyEffect::new);

    private ModEffects() {}
} 