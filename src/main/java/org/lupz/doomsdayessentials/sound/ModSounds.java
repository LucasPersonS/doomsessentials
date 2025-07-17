package org.lupz.doomsdayessentials.sound;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.lupz.doomsdayessentials.EssentialsMod;

public class ModSounds {

    public static final DeferredRegister<SoundEvent> SOUND_EVENTS = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, EssentialsMod.MOD_ID);

    public static final RegistryObject<SoundEvent> DANGER_ZONE_ENTER = register("danger_zone_enter");
    public static final RegistryObject<SoundEvent> SAFE_ZONE_ENTER = register("safe_zone_enter");
    public static final RegistryObject<SoundEvent> FREQUENCIA1 = register("frequencia1");
    public static final RegistryObject<SoundEvent> FREQUENCIA2 = register("frequencia2");
    public static final RegistryObject<SoundEvent> RECYCLER_LOOP = register("recycler_loop");

    private static RegistryObject<SoundEvent> register(String name) {
        // 16 block hearing range
        return SOUND_EVENTS.register(name, () -> SoundEvent.createFixedRangeEvent(ResourceLocation.fromNamespaceAndPath(EssentialsMod.MOD_ID, name), 16f));
    }

    public static void register(IEventBus eventBus) {
        SOUND_EVENTS.register(eventBus);
    }
} 