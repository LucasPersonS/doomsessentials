package org.lupz.doomsdayessentials.client.model;

import net.minecraft.resources.ResourceLocation;
import org.lupz.doomsdayessentials.EssentialsMod;
import org.lupz.doomsdayessentials.item.CrownItem;
import software.bernie.geckolib.model.GeoModel;

/**
 * GeckoLib model for the crown armor item. Uses the Bedrock-exported crown.geo.json.
 */
public class CrownGeoModel extends GeoModel<CrownItem> {
    private static final ResourceLocation MODEL = ResourceLocation.fromNamespaceAndPath(EssentialsMod.MOD_ID, "geo/crown.geo.json");
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(EssentialsMod.MOD_ID, "textures/models/armor/crown_layer_1.png");
    // No animations yet – use empty file.
    private static final ResourceLocation ANIM = ResourceLocation.fromNamespaceAndPath(EssentialsMod.MOD_ID, "animations/empty.animation.json");

    @Override
    public ResourceLocation getModelResource(CrownItem animatable) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(CrownItem animatable) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(CrownItem animatable) {
        return ANIM;
    }
} 