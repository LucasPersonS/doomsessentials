package org.lupz.doomsdayessentials.client.renderer;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import com.mojang.blaze3d.vertex.PoseStack;
import org.lupz.doomsdayessentials.client.model.AirdropModel;
import org.lupz.doomsdayessentials.entity.AirdropEntity;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class AirdropRenderer extends GeoEntityRenderer<AirdropEntity> {
    public AirdropRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new AirdropModel());
        this.shadowRadius = 0.8f;
    }

    @Override
    public void render(AirdropEntity entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        // Slight scale to ensure model fits expected block-sized crate
        poseStack.pushPose();
        poseStack.scale(1.0f, 1.0f, 1.0f);
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
        poseStack.popPose();
    }
}
