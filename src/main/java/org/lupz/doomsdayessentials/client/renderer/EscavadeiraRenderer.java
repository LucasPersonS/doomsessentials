package org.lupz.doomsdayessentials.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import org.lupz.doomsdayessentials.blockentity.EscavadeiraControllerBlockEntity;
import org.lupz.doomsdayessentials.client.model.EscavadeiraModel;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

public class EscavadeiraRenderer extends GeoBlockRenderer<EscavadeiraControllerBlockEntity> {
	public EscavadeiraRenderer(BlockEntityRendererProvider.Context ctx) {
		super(new EscavadeiraModel());
	}

	protected int getBlockLightLevel(EscavadeiraControllerBlockEntity animatable, BlockPos pos) {
		return 15;
	}
} 