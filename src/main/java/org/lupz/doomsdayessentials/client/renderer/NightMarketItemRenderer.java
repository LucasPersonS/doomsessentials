package org.lupz.doomsdayessentials.client.renderer;

import org.lupz.doomsdayessentials.client.model.NightMarketItemModel;
import org.lupz.doomsdayessentials.item.NightMarketBlockItem;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public class NightMarketItemRenderer extends GeoItemRenderer<NightMarketBlockItem> {
    public NightMarketItemRenderer() {
        super(new NightMarketItemModel());
    }
}
