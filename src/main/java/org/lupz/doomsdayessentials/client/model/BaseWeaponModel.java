package org.lupz.doomsdayessentials.client.model;

import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.model.GeoModel;

/**
 * Model for the base weapon (Browning M2) that can be rendered on entities/blocks
 */
public class BaseWeaponModel<T extends GeoAnimatable> extends GeoModel<T> {
    @Override
    public ResourceLocation getModelResource(T animatable) {
        return ResourceLocation.fromNamespaceAndPath("doomsdayessentials", "geo/baseweapon.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(T animatable) {
        return ResourceLocation.fromNamespaceAndPath("doomsdayessentials", "textures/entity/browning_m2.png");
    }

    @Override
    public ResourceLocation getAnimationResource(T animatable) {
        return ResourceLocation.fromNamespaceAndPath("doomsdayessentials", "animations/baseweapon.animation.json");
    }
}
