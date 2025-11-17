package org.lupz.doomsdayessentials.client.model;

import net.minecraft.resources.ResourceLocation;
import org.lupz.doomsdayessentials.EssentialsMod;
import org.lupz.doomsdayessentials.item.StorageBlockItem;
import software.bernie.geckolib.model.GeoModel;

public class StorageItemModel extends GeoModel<StorageBlockItem> {
    @Override
    public ResourceLocation getModelResource(StorageBlockItem animatable) {
        return ResourceLocation.fromNamespaceAndPath(EssentialsMod.MOD_ID, "geo/storage.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(StorageBlockItem animatable) {
        return ResourceLocation.fromNamespaceAndPath(EssentialsMod.MOD_ID, "textures/block/storage.png");
    }

    @Override
    public ResourceLocation getAnimationResource(StorageBlockItem animatable) {
        return ResourceLocation.fromNamespaceAndPath(EssentialsMod.MOD_ID, "animations/storage.animation.json");
    }
}
