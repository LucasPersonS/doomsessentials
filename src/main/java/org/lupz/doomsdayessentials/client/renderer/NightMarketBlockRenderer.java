package org.lupz.doomsdayessentials.client.renderer;

import org.lupz.doomsdayessentials.client.model.NightMarketBlockModel;
import org.lupz.doomsdayessentials.event.eclipse.market.NightMarketBlockEntity;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

public class NightMarketBlockRenderer extends GeoBlockRenderer<NightMarketBlockEntity> {
    public NightMarketBlockRenderer() {
        super(new NightMarketBlockModel());
    }
} 