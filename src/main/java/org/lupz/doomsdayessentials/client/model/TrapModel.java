package org.lupz.doomsdayessentials.client.model;

import net.minecraft.resources.ResourceLocation;
import org.lupz.doomsdayessentials.EssentialsMod;
import org.lupz.doomsdayessentials.entity.TrapEntity;
import software.bernie.geckolib.model.GeoModel;

public class TrapModel extends GeoModel<TrapEntity> {
    
    @Override
    public ResourceLocation getModelResource(TrapEntity object) {
        return ResourceLocation.fromNamespaceAndPath(EssentialsMod.MOD_ID, "geo/trap.geo.json");
    }
    
    @Override
    public ResourceLocation getTextureResource(TrapEntity object) {
        return ResourceLocation.fromNamespaceAndPath(EssentialsMod.MOD_ID, "textures/block/beartrap.png");
    }
    
    @Override
    public ResourceLocation getAnimationResource(TrapEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(EssentialsMod.MOD_ID, "animations/trap.animation.json");
    }
}
