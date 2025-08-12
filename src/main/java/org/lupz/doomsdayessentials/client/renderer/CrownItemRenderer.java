package org.lupz.doomsdayessentials.client.renderer;

import org.lupz.doomsdayessentials.client.model.CrownItemGeoModel;
import org.lupz.doomsdayessentials.item.CrownItem;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public class CrownItemRenderer extends GeoItemRenderer<CrownItem> {
    public CrownItemRenderer() {
        super(new CrownItemGeoModel());
    }
} 