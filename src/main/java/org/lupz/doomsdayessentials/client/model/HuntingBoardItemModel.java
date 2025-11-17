package org.lupz.doomsdayessentials.client.model;

import net.minecraft.resources.ResourceLocation;
import org.lupz.doomsdayessentials.EssentialsMod;
import org.lupz.doomsdayessentials.item.HuntingBoardBlockItem;
import software.bernie.geckolib.model.GeoModel;

public class HuntingBoardItemModel extends GeoModel<HuntingBoardBlockItem> {
    @Override
    public ResourceLocation getModelResource(HuntingBoardBlockItem animatable) {
        return ResourceLocation.fromNamespaceAndPath(EssentialsMod.MOD_ID, "geo/hunting_board.json");
    }

    @Override
    public ResourceLocation getTextureResource(HuntingBoardBlockItem animatable) {
        return ResourceLocation.fromNamespaceAndPath(EssentialsMod.MOD_ID, "textures/block/huntingmodel.png");
    }

    @Override
    public ResourceLocation getAnimationResource(HuntingBoardBlockItem animatable) {
        return null;
    }
}
