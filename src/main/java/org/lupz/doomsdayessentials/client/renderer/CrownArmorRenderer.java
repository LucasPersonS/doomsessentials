package org.lupz.doomsdayessentials.client.renderer;

import org.lupz.doomsdayessentials.client.model.CrownGeoModel;
import org.lupz.doomsdayessentials.item.CrownItem;
import software.bernie.geckolib.renderer.GeoArmorRenderer;

/**
 * GeckoLib armor renderer for the {@link CrownItem}.
 */
public class CrownArmorRenderer extends GeoArmorRenderer<CrownItem> {
    public CrownArmorRenderer() {
        super(new CrownGeoModel());
    }
} 