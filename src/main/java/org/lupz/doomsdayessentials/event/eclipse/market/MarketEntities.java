package org.lupz.doomsdayessentials.event.eclipse.market;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.lupz.doomsdayessentials.EssentialsMod;

public final class MarketEntities {
    private MarketEntities(){}
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, EssentialsMod.MOD_ID);

    public static final RegistryObject<EntityType<NightMarketEntity>> NIGHT_MARKET = ENTITIES.register("night_market",
            () -> EntityType.Builder.<NightMarketEntity>of(NightMarketEntity::new, MobCategory.MISC)
                    .sized(0.6f, 1.8f)
                    .clientTrackingRange(48)
                    .build("night_market"));

    public static void register(IEventBus bus){ ENTITIES.register(bus); }
} 