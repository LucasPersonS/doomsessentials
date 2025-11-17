package org.lupz.doomsdayessentials.client.model;

import net.minecraft.resources.ResourceLocation;
import org.lupz.doomsdayessentials.EssentialsMod;
import org.lupz.doomsdayessentials.item.NightMarketBlockItem;
import software.bernie.geckolib.model.GeoModel;

public class NightMarketItemModel extends GeoModel<NightMarketBlockItem> {
    @Override
    public ResourceLocation getModelResource(NightMarketBlockItem animatable) {
        return ResourceLocation.fromNamespaceAndPath(EssentialsMod.MOD_ID, "geo/night_market.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(NightMarketBlockItem animatable) {
        return ResourceLocation.fromNamespaceAndPath(EssentialsMod.MOD_ID, "textures/block/night_market.png");
    }

    @Override
    public ResourceLocation getAnimationResource(NightMarketBlockItem animatable) {
        return ResourceLocation.fromNamespaceAndPath(EssentialsMod.MOD_ID, "animations/black_market.animation.json");
    }
}
