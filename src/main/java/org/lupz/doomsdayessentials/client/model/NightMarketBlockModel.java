package org.lupz.doomsdayessentials.client.model;

import net.minecraft.resources.ResourceLocation;
import org.lupz.doomsdayessentials.EssentialsMod;
import org.lupz.doomsdayessentials.event.eclipse.market.NightMarketBlockEntity;
import software.bernie.geckolib.model.GeoModel;

public class NightMarketBlockModel extends GeoModel<NightMarketBlockEntity> {
    private static ResourceLocation rl(String path){ return ResourceLocation.fromNamespaceAndPath(EssentialsMod.MOD_ID, path); }

    @Override
    public ResourceLocation getModelResource(NightMarketBlockEntity animatable) {
        return rl("geo/night_market.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(NightMarketBlockEntity animatable) {
        return rl("textures/block/night_market.png");
    }

    @Override
    public ResourceLocation getAnimationResource(NightMarketBlockEntity animatable) {
        // If you add animations later, point here. For now a non-existent file is fine.
        return rl("animations/black_market.animation.json");
    }
} 