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

	public static final RegistryObject<SoundEvent> ABDUCTION_THUNDER = register("abduction_thunder");
	public static final RegistryObject<SoundEvent> ABDUCTION_BEAM = register("abduction_beam");
	public static final RegistryObject<SoundEvent> VANISH = register("vanish");
	public static final RegistryObject<SoundEvent> HELLO = register("hello");

	// New frequency ambience
	public static final RegistryObject<SoundEvent> ESPALHE = register("espalhe");
	public static final RegistryObject<SoundEvent> WHISPERING = register("whispering");
	public static final RegistryObject<SoundEvent> WHIND = register("whind");
	public static final RegistryObject<SoundEvent> INSIDE_FREQUENCY = register("inside_frequency");

	// Sentry fixed shoot sound
	public static final RegistryObject<SoundEvent> SENTRY_SHOOT_SCAR_L_3P = register("scar_l_shoot_3p");

	// Kill notification
	public static final RegistryObject<SoundEvent> KILL_NOTIFY = register("kill_notify");
	// AK47 specific kill sound
	public static final RegistryObject<SoundEvent> KILL_AK47 = register("kill_ak47");
	// Kuronami kill sounds
	public static final RegistryObject<SoundEvent> KURONAMIKS = register("kuronamiks");
	public static final RegistryObject<SoundEvent> KURONAMI2 = register("kuronami2");
	public static final RegistryObject<SoundEvent> KURONAMI3 = register("kuronami3");
	public static final RegistryObject<SoundEvent> KURONAMI4 = register("kuronami4");
	public static final RegistryObject<SoundEvent> KURONAMI5 = register("kuronami5");

	// Incremental kill streak sounds (cycle 1..5)
	public static final RegistryObject<SoundEvent> KILL_STREAK_1 = register("kill_streak_1");
	public static final RegistryObject<SoundEvent> KILL_STREAK_2 = register("kill_streak_2");
	public static final RegistryObject<SoundEvent> KILL_STREAK_3 = register("kill_streak_3");
	public static final RegistryObject<SoundEvent> KILL_STREAK_4 = register("kill_streak_4");
	public static final RegistryObject<SoundEvent> KILL_STREAK_5 = register("kill_streak_5");

	// Lootbox sounds
	public static final RegistryObject<SoundEvent> LOOTBOX_SPIN = register("lootbox_spin");
	public static final RegistryObject<SoundEvent> LOOTBOX_COMPLETE = register("lootbox_complete");

	private static RegistryObject<SoundEvent> register(String name) {
		// 16 block hearing range
		return SOUND_EVENTS.register(name, () -> SoundEvent.createFixedRangeEvent(ResourceLocation.fromNamespaceAndPath(EssentialsMod.MOD_ID, name), 16f));
	}

	public static void register(IEventBus eventBus) {
		SOUND_EVENTS.register(eventBus);
	}
} 