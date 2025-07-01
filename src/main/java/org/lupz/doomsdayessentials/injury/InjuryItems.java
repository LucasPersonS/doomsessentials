package org.lupz.doomsdayessentials.injury;

import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.lupz.doomsdayessentials.EssentialsMod;

public class InjuryItems {
   public static final DeferredRegister<Item> ITEMS;
   public static final RegistryObject<Item> MEDIC_KIT;

   public InjuryItems() {
   }

   public static void register(IEventBus eventBus) {
      ITEMS.register(eventBus);
   }

   static {
      ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, EssentialsMod.MOD_ID);
      MEDIC_KIT = ITEMS.register("medic_kit", () -> {
         return new MedicKitItem((new Item.Properties()).stacksTo(16));
      });
   }
} 