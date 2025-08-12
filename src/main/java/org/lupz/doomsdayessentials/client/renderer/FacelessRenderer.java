package org.lupz.doomsdayessentials.client.renderer;

import org.lupz.doomsdayessentials.client.model.FacelessModel;
import org.lupz.doomsdayessentials.entity.FacelessEntity;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

public class FacelessRenderer extends GeoEntityRenderer<FacelessEntity> {
    public FacelessRenderer(EntityRendererProvider.Context ctx){
        super(ctx, new FacelessModel());
        this.shadowRadius = 0.4f;
    }
} 