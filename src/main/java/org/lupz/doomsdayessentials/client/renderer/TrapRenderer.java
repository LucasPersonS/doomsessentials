package org.lupz.doomsdayessentials.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import org.lupz.doomsdayessentials.EssentialsMod;
import org.lupz.doomsdayessentials.client.model.TrapModel;
import org.lupz.doomsdayessentials.entity.TrapEntity;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class TrapRenderer extends GeoEntityRenderer<TrapEntity> {
    
    public TrapRenderer(EntityRendererProvider.Context context) {
        super(context, new TrapModel());
    }
    
    @Override
    public ResourceLocation getTextureLocation(TrapEntity entity) {
        return ResourceLocation.fromNamespaceAndPath(EssentialsMod.MOD_ID, "textures/block/beartrap.png");
    }
    
    @Override
    public void render(TrapEntity entity, float entityYaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight) {
        poseStack.pushPose();
        poseStack.scale(0.5f, 0.5f, 0.5f); // Scale down the trap
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
        poseStack.popPose();
    }
}
