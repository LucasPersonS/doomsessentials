package org.lupz.doomsdayessentials.client.renderer;

import org.lupz.doomsdayessentials.block.HuntingBoardBlockEntity;
import org.lupz.doomsdayessentials.client.model.HuntingBoardModel;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

public class HuntingBoardBlockRenderer extends GeoBlockRenderer<HuntingBoardBlockEntity> {
	public HuntingBoardBlockRenderer() {
		super(new HuntingBoardModel());
	}

} 