package org.lupz.doomsdayessentials.client.renderer;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import org.lupz.doomsdayessentials.client.model.NightMarketModel;
import org.lupz.doomsdayessentials.event.eclipse.market.NightMarketEntity;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class NightMarketRenderer extends GeoEntityRenderer<NightMarketEntity> {
    public NightMarketRenderer(EntityRendererProvider.Context ctx){
        super(ctx, new NightMarketModel());
        this.shadowRadius = 0.4f;
    }
} 