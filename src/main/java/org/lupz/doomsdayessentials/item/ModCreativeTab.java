package org.lupz.doomsdayessentials.item;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.lupz.doomsdayessentials.EssentialsMod;
import org.lupz.doomsdayessentials.injury.InjuryItems;

/**
 * Registers the Doomsday Essentials creative mode tab and populates it with every item from this mod.
 */
public final class ModCreativeTab {
    private ModCreativeTab() {}

    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, EssentialsMod.MOD_ID);

    public static final RegistryObject<CreativeModeTab> ESSENTIALS_TAB = TABS.register("doomsday_essentials", () ->
            CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup." + EssentialsMod.MOD_ID))
                    .icon(() -> new ItemStack(InjuryItems.MEDIC_KIT.get()))
                    .displayItems((params, output) -> {
                        for (Item item : ForgeRegistries.ITEMS.getValues()) {
                            if (ForgeRegistries.ITEMS.getKey(item).getNamespace().equals(EssentialsMod.MOD_ID)) {
                                output.accept(item);
                            }
                        }
                    })
                    .build());

    public static void register(IEventBus bus) {
        TABS.register(bus);
    }
} 