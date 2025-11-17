package org.lupz.doomsdayessentials.client.model;

import net.minecraft.resources.ResourceLocation;
import org.lupz.doomsdayessentials.EssentialsMod;
import org.lupz.doomsdayessentials.entity.AirdropEntity;
import software.bernie.geckolib.model.GeoModel;

public class AirdropModel extends GeoModel<AirdropEntity> {
    @Override
    public ResourceLocation getModelResource(AirdropEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(EssentialsMod.MOD_ID, "geo/airdrop.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(AirdropEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(EssentialsMod.MOD_ID, "textures/block/airdrop.png");
    }

    @Override
    public ResourceLocation getAnimationResource(AirdropEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(EssentialsMod.MOD_ID, "animations/airdrop.animation.json");
    }
}
