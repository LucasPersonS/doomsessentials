package org.lupz.doomsdayessentials.client.model;

import net.minecraft.resources.ResourceLocation;
import org.lupz.doomsdayessentials.EssentialsMod;
import org.lupz.doomsdayessentials.event.eclipse.market.NightMarketBlockEntity;
import software.bernie.geckolib.model.GeoModel;

public class NightMarketBlockModel extends GeoModel<NightMarketBlockEntity> {
    private static ResourceLocation rl(String path){ return ResourceLocation.fromNamespaceAndPath(EssentialsMod.MOD_ID, path); }

    @Override
    public ResourceLocation getModelResource(NightMarketBlockEntity animatable) {
        // Matches existing file at assets/doomsdayessentials/geo/black_market.geo.json
        return rl("geo/black_market.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(NightMarketBlockEntity animatable) {
        // Provide your texture here; placeholder path
        return rl("textures/entity/black_market.png");
    }

    @Override
    public ResourceLocation getAnimationResource(NightMarketBlockEntity animatable) {
        // If you add animations later, point here. For now a non-existent file is fine.
        return rl("animations/black_market.animation.json");
    }
} 