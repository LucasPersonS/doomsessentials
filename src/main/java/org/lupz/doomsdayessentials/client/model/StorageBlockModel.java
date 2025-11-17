package org.lupz.doomsdayessentials.client.model;

import net.minecraft.resources.ResourceLocation;
import org.lupz.doomsdayessentials.EssentialsMod;
import org.lupz.doomsdayessentials.guild.block.StorageBlockEntity;
import software.bernie.geckolib.model.GeoModel;

public class StorageBlockModel extends GeoModel<StorageBlockEntity> {
    private static ResourceLocation rl(String path) {
        return ResourceLocation.fromNamespaceAndPath(EssentialsMod.MOD_ID, path);
    }

    @Override
    public ResourceLocation getModelResource(StorageBlockEntity animatable) {
        return rl("geo/storage.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(StorageBlockEntity animatable) {
        return rl("textures/block/storage.png");
    }

    @Override
    public ResourceLocation getAnimationResource(StorageBlockEntity animatable) {
        return rl("animations/storage.animation.json");
    }
}
