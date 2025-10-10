package org.lupz.doomsdayessentials.client.model;

import net.minecraft.resources.ResourceLocation;
import org.lupz.doomsdayessentials.EssentialsMod;
import org.lupz.doomsdayessentials.entity.DummyEntity;
import software.bernie.geckolib.model.GeoModel;

public class DummyModel extends GeoModel<DummyEntity> {
	private static ResourceLocation rl(String path){return ResourceLocation.fromNamespaceAndPath(EssentialsMod.MOD_ID, path);}    
	@Override
	public ResourceLocation getModelResource(DummyEntity animatable){return rl("geo/dummy.geo.json");}
	@Override
	public ResourceLocation getTextureResource(DummyEntity animatable){return rl("textures/entity/dummy.png");}
	@Override
	public ResourceLocation getAnimationResource(DummyEntity animatable){return rl("animations/dummy.animation.json");}
} 