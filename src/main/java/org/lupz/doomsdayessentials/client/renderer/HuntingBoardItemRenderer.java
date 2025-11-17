package org.lupz.doomsdayessentials.client.renderer;

import org.lupz.doomsdayessentials.client.model.HuntingBoardItemModel;
import org.lupz.doomsdayessentials.item.HuntingBoardBlockItem;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public class HuntingBoardItemRenderer extends GeoItemRenderer<HuntingBoardBlockItem> {
    public HuntingBoardItemRenderer() {
        super(new HuntingBoardItemModel());
    }
}
