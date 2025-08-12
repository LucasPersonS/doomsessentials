package org.lupz.doomsdayessentials.client.model;

import net.minecraft.resources.ResourceLocation;
import org.lupz.doomsdayessentials.EssentialsMod;
import org.lupz.doomsdayessentials.entity.FacelessEntity;
import software.bernie.geckolib.model.GeoModel;

public class FacelessModel extends GeoModel<FacelessEntity> {
    private static ResourceLocation rl(String path){return ResourceLocation.fromNamespaceAndPath(EssentialsMod.MOD_ID, path);}    
    @Override
    public ResourceLocation getModelResource(FacelessEntity animatable){return rl("geo/faceless.geo.json");}
    @Override
    public ResourceLocation getTextureResource(FacelessEntity animatable){return rl("textures/entity/faceless.png");}
    @Override
    public ResourceLocation getAnimationResource(FacelessEntity animatable){return rl("animations/faceless.animation.json");}
} 