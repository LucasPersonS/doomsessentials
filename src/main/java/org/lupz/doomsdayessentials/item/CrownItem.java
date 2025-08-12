package org.lupz.doomsdayessentials.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import java.util.function.Consumer;

import java.util.List;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.util.GeckoLibUtil;
import org.jetbrains.annotations.NotNull;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import software.bernie.geckolib.animatable.GeoItem;
import org.lupz.doomsdayessentials.client.renderer.CrownArmorRenderer;
import net.minecraft.world.item.Item;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import software.bernie.geckolib.core.animation.AnimatableManager.ControllerRegistrar;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import org.lupz.doomsdayessentials.client.renderer.CrownItemRenderer;

/**
 * Simple cosmetic crown that occupies the helmet slot.
 */
public class CrownItem extends ArmorItem implements GeoItem {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public CrownItem(Item.Properties props) {
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

    /* ----------------- Geckolib ----------------- */
    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            private CrownArmorRenderer renderer;
            @Override
            public @NotNull HumanoidModel<?> getHumanoidArmorModel(LivingEntity entity, ItemStack stack, EquipmentSlot slot, HumanoidModel<?> original) {
                if (renderer == null) renderer = new CrownArmorRenderer();
                renderer.prepForRender(entity, stack, slot, original);
                return renderer;
            }
            private CrownItemRenderer itemRenderer;
            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if(itemRenderer==null) itemRenderer = new CrownItemRenderer();
                return itemRenderer;
            }
        });
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    public void registerControllers(ControllerRegistrar registrar) {
        // No animations for static cosmetic armour.
    }
} 