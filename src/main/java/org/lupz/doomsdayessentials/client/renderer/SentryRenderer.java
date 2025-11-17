package org.lupz.doomsdayessentials.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.world.item.ItemDisplayContext;
import org.lupz.doomsdayessentials.client.model.SentryModel;
import org.lupz.doomsdayessentials.entity.SentryEntity;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class SentryRenderer extends GeoEntityRenderer<SentryEntity> {
	public SentryRenderer(EntityRendererProvider.Context ctx) {
		super(ctx, new SentryModel());
		this.shadowRadius = 0.2f;
	}

	@Override
	public void renderRecursively(PoseStack poseStack, SentryEntity animatable, GeoBone bone,
								  RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer,
								  boolean isReRender, float partialTick, int packedLight, int packedOverlay,
								  float red, float green, float blue, float alpha) {
		// Render weapon at the boca_arma bone (weapon barrel/mouth mounting point - red area)
		if (bone.getName().equals("boca_arma") && !animatable.getMountedGun().isEmpty()) {
			poseStack.pushPose();
			
			// Position and orient the weapon to fit in the red vertical slot
			poseStack.translate(0, 1.2, -0.9);  // Elevate Y higher and move forward
			poseStack.mulPose(Axis.XP.rotationDegrees(-90));  // Rotate -90 degrees on X-axis
			poseStack.mulPose(Axis.YP.rotationDegrees(90));  // Rotate 90 degrees on Y-axis
			poseStack.mulPose(Axis.ZP.rotationDegrees(90));  // Flip 90 degrees on Z-axis
			poseStack.scale(0.35f, 0.35f, 0.35f);  // Scale to fit in slot
			
			// Render the mounted gun as an item
			Minecraft.getInstance().getItemRenderer().renderStatic(
				animatable.getMountedGun(),
				ItemDisplayContext.FIXED,
				packedLight,
				OverlayTexture.NO_OVERLAY,
				poseStack,
				bufferSource,
				animatable.level(),
				animatable.getId()
			);
			
			poseStack.popPose();
		}
		
		// Continue with normal bone rendering
		super.renderRecursively(poseStack, animatable, bone, renderType, bufferSource, buffer,
				isReRender, partialTick, packedLight, packedOverlay, red, green, blue, alpha);
	}
}