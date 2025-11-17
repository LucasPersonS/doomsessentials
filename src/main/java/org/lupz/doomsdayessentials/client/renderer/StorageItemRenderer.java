package org.lupz.doomsdayessentials.client.renderer;

import org.lupz.doomsdayessentials.client.model.StorageItemModel;
import org.lupz.doomsdayessentials.item.StorageBlockItem;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public class StorageItemRenderer extends GeoItemRenderer<StorageBlockItem> {
    public StorageItemRenderer() {
        super(new StorageItemModel());
    }
}
