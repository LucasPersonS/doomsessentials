package org.lupz.doomsdayessentials.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.world.item.ItemDisplayContext;
import org.lupz.doomsdayessentials.client.model.SentryModel;
import org.lupz.doomsdayessentials.entity.SentryEntity;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class SentryRenderer extends GeoEntityRenderer<SentryEntity> {
	public SentryRenderer(EntityRendererProvider.Context ctx) {
		super(ctx, new SentryModel());
		this.shadowRadius = 0.2f;
	}

	@Override
	public void render(SentryEntity entity, float yaw, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
		poseStack.pushPose();
		poseStack.mulPose(Axis.YP.rotationDegrees(entity.getYRot()));
		super.render(entity, yaw, partialTick, poseStack, buffer, packedLight);
		if (!entity.getMountedGun().isEmpty()) {
			poseStack.pushPose();
			poseStack.translate(0.0, 0.85 + 0.35, 0.0);
			poseStack.scale(0.35f, 0.35f, 0.35f);
			Minecraft.getInstance().getItemRenderer().renderStatic(
				entity.getMountedGun(),
				ItemDisplayContext.FIXED,
				packedLight,
				OverlayTexture.NO_OVERLAY,
				poseStack,
				buffer,
				entity.level(),
				entity.getId()
			);
			poseStack.popPose();
		}
		poseStack.popPose();
	}
} 