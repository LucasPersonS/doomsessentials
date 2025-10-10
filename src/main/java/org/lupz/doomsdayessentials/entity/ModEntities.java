package org.lupz.doomsdayessentials.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.lupz.doomsdayessentials.EssentialsMod;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;

public final class ModEntities {
    private ModEntities(){}
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, EssentialsMod.MOD_ID);

    public static final RegistryObject<EntityType<FacelessEntity>> FACELESS = ENTITIES.register("faceless",
            () -> EntityType.Builder.<FacelessEntity>of(FacelessEntity::new, MobCategory.MONSTER)
                    .sized(0.6f, 1.9f)
                    .clientTrackingRange(64)
                    .build("faceless"));

    public static final RegistryObject<EntityType<org.lupz.doomsdayessentials.entity.SentryEntity>> SENTRY = ENTITIES.register("sentry",
            () -> EntityType.Builder.<org.lupz.doomsdayessentials.entity.SentryEntity>of(org.lupz.doomsdayessentials.entity.SentryEntity::new, MobCategory.MONSTER)
                    .sized(0.6f, 0.6f)
                    .clientTrackingRange(64)
                    .build("sentry"));

	public static final RegistryObject<EntityType<DummyEntity>> DUMMY = ENTITIES.register("dummy",
			() -> EntityType.Builder.<DummyEntity>of(DummyEntity::new, MobCategory.MISC)
					.sized(0.6f, 1.8f)
					.clientTrackingRange(48)
					.build("dummy"));

    public static void register(IEventBus bus){
        ENTITIES.register(bus);
        bus.addListener(ModEntities::onAttributeCreate);
    }

    private static void onAttributeCreate(EntityAttributeCreationEvent e){
        e.put(FACELESS.get(), FacelessEntity.createAttributes().build());
        e.put(SENTRY.get(), org.lupz.doomsdayessentials.entity.SentryEntity.createAttributes().build());
		e.put(DUMMY.get(), DummyEntity.createAttributes().build());
    }
} 