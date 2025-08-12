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

    public static void register(IEventBus bus){
        ENTITIES.register(bus);
        bus.addListener(ModEntities::onAttributeCreate);
    }

    private static void onAttributeCreate(EntityAttributeCreationEvent e){
        e.put(FACELESS.get(), FacelessEntity.createAttributes().build());
    }
} 