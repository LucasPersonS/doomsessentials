package org.lupz.doomsdayessentials.client.model;

import net.minecraft.resources.ResourceLocation;
import org.lupz.doomsdayessentials.blockentity.EscavadeiraControllerBlockEntity;
import software.bernie.geckolib.model.GeoModel;

public class EscavadeiraModel extends GeoModel<EscavadeiraControllerBlockEntity> {
	@Override
	public ResourceLocation getModelResource(EscavadeiraControllerBlockEntity animatable) {
		return ResourceLocation.fromNamespaceAndPath("doomsdayessentials", "geo/petroleira.geo.json");
	}

	@Override
	public ResourceLocation getTextureResource(EscavadeiraControllerBlockEntity animatable) {
		return ResourceLocation.fromNamespaceAndPath("doomsdayessentials", "textures/block/petroleira.png");
	}

	@Override
	public ResourceLocation getAnimationResource(EscavadeiraControllerBlockEntity animatable) {
		return ResourceLocation.fromNamespaceAndPath("doomsdayessentials", "animations/dummy.animation.json");
	}
} 