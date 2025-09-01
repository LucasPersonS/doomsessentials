package org.lupz.doomsdayessentials.client.model;

import net.minecraft.resources.ResourceLocation;
import org.lupz.doomsdayessentials.EssentialsMod;
import org.lupz.doomsdayessentials.entity.SentryEntity;
import software.bernie.geckolib.model.GeoModel;

public class SentryModel extends GeoModel<SentryEntity> {
	@Override
	public ResourceLocation getModelResource(SentryEntity animatable) {
		return ResourceLocation.fromNamespaceAndPath(EssentialsMod.MOD_ID, "geo/baseweapon.geo.json");
	}

	@Override
	public ResourceLocation getTextureResource(SentryEntity animatable) {
		return ResourceLocation.fromNamespaceAndPath(EssentialsMod.MOD_ID, "textures/block/baseweapon.png");
	}

	@Override
	public ResourceLocation getAnimationResource(SentryEntity animatable) {
		return null;
	}
} 