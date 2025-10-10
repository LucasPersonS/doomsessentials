package org.lupz.doomsdayessentials.client.renderer;

import org.lupz.doomsdayessentials.client.model.DummyModel;
import org.lupz.doomsdayessentials.entity.DummyEntity;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

public class DummyRenderer extends GeoEntityRenderer<DummyEntity> {
	public DummyRenderer(EntityRendererProvider.Context ctx){
		super(ctx, new DummyModel());
		this.shadowRadius = 0.2f;
	}
} 