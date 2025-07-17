package org.lupz.doomsdayessentials.item;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.crafting.Ingredient;
import org.lupz.doomsdayessentials.EssentialsMod;

/**
 * Armor materials used by Doomsday Essentials. Currently only includes a cosmetic crown material
 * that provides no protection and cannot be damaged.
 */
public enum ModArmorMaterials implements ArmorMaterial {
    CROWN("crown", SoundEvents.ARMOR_EQUIP_GOLD);

    private final String name;
    private final SoundEvent equipSound;

    ModArmorMaterials(String name, SoundEvent equipSound) {
        this.name = name;
        this.equipSound = equipSound;
    }

    @Override
    public int getDurabilityForType(ArmorItem.Type slot) {
        // Cosmetic item – no durability / damage
        return 0;
    }

    @Override
    public int getDefenseForType(ArmorItem.Type slot) {
        return 0;
    }

    @Override
    public int getEnchantmentValue() {
        return 0;
    }

    @Override
    public SoundEvent getEquipSound() {
        return equipSound;
    }

    @Override
    public Ingredient getRepairIngredient() {
        // Cosmetic – not repairable.
        return Ingredient.EMPTY;
    }

    @Override
    public String getName() {
        // The game expects the full name to be <modid>:<material_name>
        return EssentialsMod.MOD_ID + ":" + name;
    }

    @Override
    public float getToughness() {
        return 0;
    }

    @Override
    public float getKnockbackResistance() {
        return 0;
    }
} 