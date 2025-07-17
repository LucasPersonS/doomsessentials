package org.lupz.doomsdayessentials.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import java.util.function.Consumer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.resources.ResourceLocation;
import org.lupz.doomsdayessentials.client.model.CrownModel;

import java.util.List;

/**
 * Simple cosmetic crown that occupies the helmet slot.
 */
public class CrownItem extends ArmorItem {

    public CrownItem(Properties props) {
        super(ModArmorMaterials.CROWN, Type.HELMET, props);
    }

    @Override
    public boolean isRepairable(ItemStack stack) {
        return false;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.doomsdayessentials.crown.desc").withStyle(ChatFormatting.GRAY));
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            private final ResourceLocation TEX = ResourceLocation.fromNamespaceAndPath(org.lupz.doomsdayessentials.EssentialsMod.MOD_ID, "textures/models/armor/crown_layer_1.png");
            private CrownModel<LivingEntity> cachedModel;

            public HumanoidModel<?> getHumanoidArmorModel(LivingEntity living, net.minecraft.world.item.ItemStack stack, EquipmentSlot slot, HumanoidModel<?> defaultModel) {
                if (slot != EquipmentSlot.HEAD) return defaultModel;
                if (cachedModel == null) {
                    cachedModel = new CrownModel<>(Minecraft.getInstance().getEntityModels().bakeLayer(CrownModel.LAYER_LOCATION));
                }
                return cachedModel;
            }

            public ResourceLocation getArmorTexture(net.minecraft.world.item.ItemStack stack, EquipmentSlot slot, String type, LivingEntity entity) {
                return TEX;
            }
        });
    }
} 