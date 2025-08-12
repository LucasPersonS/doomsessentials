package org.lupz.doomsdayessentials.client.model;

import net.minecraft.resources.ResourceLocation;
import org.lupz.doomsdayessentials.EssentialsMod;
import org.lupz.doomsdayessentials.event.eclipse.market.NightMarketEntity;
import software.bernie.geckolib.model.GeoModel;

public class NightMarketModel extends GeoModel<NightMarketEntity> {
    private static ResourceLocation rl(String path){return ResourceLocation.fromNamespaceAndPath(EssentialsMod.MOD_ID, path);}    
    @Override
    public ResourceLocation getModelResource(NightMarketEntity animatable){return rl("geo/black_market.geo.json");}
    @Override
    public ResourceLocation getTextureResource(NightMarketEntity animatable){return rl("textures/entity/black_market.png");}
    @Override
    public ResourceLocation getAnimationResource(NightMarketEntity animatable){return rl("animations/black_market.animation.json");}
} 