package org.lupz.doomsdayessentials.client.renderer;

import org.lupz.doomsdayessentials.client.model.EscavadeiraItemModel;
import org.lupz.doomsdayessentials.item.EscavadeiraBlockItem;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public class EscavadeiraItemRenderer extends GeoItemRenderer<EscavadeiraBlockItem> {
    public EscavadeiraItemRenderer() {
        super(new EscavadeiraItemModel());
    }
}
