package org.lupz.doomsdayessentials.client.model;

import net.minecraft.resources.ResourceLocation;
import org.lupz.doomsdayessentials.item.EscavadeiraBlockItem;
import software.bernie.geckolib.model.GeoModel;

public class EscavadeiraItemModel extends GeoModel<EscavadeiraBlockItem> {
    @Override
    public ResourceLocation getModelResource(EscavadeiraBlockItem animatable) {
        return ResourceLocation.fromNamespaceAndPath("doomsdayessentials", "geo/petroleira.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(EscavadeiraBlockItem animatable) {
        return ResourceLocation.fromNamespaceAndPath("doomsdayessentials", "textures/block/petroleira.png");
    }

    @Override
    public ResourceLocation getAnimationResource(EscavadeiraBlockItem animatable) {
        return ResourceLocation.fromNamespaceAndPath("doomsdayessentials", "animations/turn_on.animation.json");
    }
}
