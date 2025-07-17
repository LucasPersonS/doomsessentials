package org.lupz.doomsdayessentials.client.particle;

import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.lupz.doomsdayessentials.EssentialsMod;

public class ModParticles {
    public static final DeferredRegister<ParticleType<?>> PARTICLE_TYPES =
            DeferredRegister.create(ForgeRegistries.PARTICLE_TYPES, EssentialsMod.MOD_ID);

    public static final RegistryObject<SimpleParticleType> FREQUENCY_PARTICLE =
            PARTICLE_TYPES.register("frequency", () -> new SimpleParticleType(true));

    public static final RegistryObject<SimpleParticleType> BLACK_SMOKE_PARTICLE =
            PARTICLE_TYPES.register("black_smoke", () -> new SimpleParticleType(true));


    public static void register(IEventBus eventBus) {
        PARTICLE_TYPES.register(eventBus);
    }
} 