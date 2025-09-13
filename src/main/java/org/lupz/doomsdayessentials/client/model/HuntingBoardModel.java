package org.lupz.doomsdayessentials.client.model;

import net.minecraft.resources.ResourceLocation;
import org.lupz.doomsdayessentials.EssentialsMod;
import software.bernie.geckolib.model.GeoModel;

public class HuntingBoardModel extends GeoModel<org.lupz.doomsdayessentials.block.HuntingBoardBlockEntity> {
	@Override
	public ResourceLocation getModelResource(org.lupz.doomsdayessentials.block.HuntingBoardBlockEntity animatable) {
		return ResourceLocation.fromNamespaceAndPath(EssentialsMod.MOD_ID, "geo/hunting_board.json");
	}
	@Override
	public ResourceLocation getTextureResource(org.lupz.doomsdayessentials.block.HuntingBoardBlockEntity animatable) {
		return ResourceLocation.fromNamespaceAndPath(EssentialsMod.MOD_ID, "textures/block/huntingmodel.png");
	}
	@Override
	public ResourceLocation getAnimationResource(org.lupz.doomsdayessentials.block.HuntingBoardBlockEntity animatable) {
		return null;
	}
} 