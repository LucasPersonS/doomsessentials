package org.lupz.doomsdayessentials.client.renderer;

import org.lupz.doomsdayessentials.client.model.StorageBlockModel;
import org.lupz.doomsdayessentials.guild.block.StorageBlockEntity;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

public class StorageBlockRenderer extends GeoBlockRenderer<StorageBlockEntity> {
    public StorageBlockRenderer() {
        super(new StorageBlockModel());
    }
}
