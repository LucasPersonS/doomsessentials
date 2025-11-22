package org.lupz.doomsdayessentials.event;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.monster.ZombifiedPiglin;
import net.minecraft.world.entity.monster.Drowned;
import net.minecraft.world.entity.monster.Husk;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingEquipmentChangeEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = org.lupz.doomsdayessentials.EssentialsMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ZombieArmorEvents {
    private ZombieArmorEvents(){}

    private static void strip(LivingEntity e){
        e.setItemSlot(EquipmentSlot.HEAD, net.minecraft.world.item.ItemStack.EMPTY);
        e.setItemSlot(EquipmentSlot.CHEST, net.minecraft.world.item.ItemStack.EMPTY);
        e.setItemSlot(EquipmentSlot.LEGS, net.minecraft.world.item.ItemStack.EMPTY);
        e.setItemSlot(EquipmentSlot.FEET, net.minecraft.world.item.ItemStack.EMPTY);
    }

    @SubscribeEvent
    public static void onJoin(EntityJoinLevelEvent event){
        if (!org.lupz.doomsdayessentials.config.EssentialsConfig.ZOMBIES_STRIP_ARMOR_AGGRESSIVE.get()) return;
        var e = event.getEntity();
        if (e instanceof Zombie || e instanceof ZombifiedPiglin || e instanceof Drowned || e instanceof Husk){
            strip((LivingEntity)e);
        }
    }

    @SubscribeEvent
    public static void onEquip(LivingEquipmentChangeEvent event){
        if (!org.lupz.doomsdayessentials.config.EssentialsConfig.ZOMBIES_STRIP_ARMOR_AGGRESSIVE.get()) return;
        var e = event.getEntity();
        if (e instanceof Zombie || e instanceof ZombifiedPiglin || e instanceof Drowned || e instanceof Husk){
            if (event.getTo() != null && !event.getTo().isEmpty()){
                strip(e);
            }
        }
    }
}

