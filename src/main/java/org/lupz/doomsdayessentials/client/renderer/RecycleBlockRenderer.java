package org.lupz.doomsdayessentials.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.lupz.doomsdayessentials.EssentialsMod;
import org.lupz.doomsdayessentials.block.RecycleBlockEntity;
import org.lupz.doomsdayessentials.client.model.RecycleModel2;
import com.mojang.math.Axis;

/**
 * Renders the {@link RecycleBlockEntity} using the baked {@link RecycleModel2}.
 */
public class RecycleBlockRenderer implements BlockEntityRenderer<RecycleBlockEntity> {

    public static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(EssentialsMod.MOD_ID, "textures/block/recycle_model.png");
    private final RecycleModel2<net.minecraft.world.entity.Entity> model;
    private final Font font;

    public RecycleBlockRenderer(BlockEntityRendererProvider.Context ctx) {
        this.model = new RecycleModel2<>(ctx.bakeLayer(RecycleModel2.LAYER_LOCATION));
        this.font = ctx.getFont();
    }

    @Override
    public void render(RecycleBlockEntity blockEntity, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay) {
        poseStack.pushPose();
        poseStack.translate(0.5D, 1.5D, 0.5D);
        poseStack.mulPose(Axis.XP.rotationDegrees(180)); // Flip model

        VertexConsumer vc = buffer.getBuffer(RenderType.entityCutout(TEXTURE));
        model.renderToBuffer(poseStack, vc, packedLight, packedOverlay, 1f, 1f, 1f, 1f);

        poseStack.popPose();

        // Render floating text
        Component text = Component.literal("Recicle seus itens aqui!!");
        poseStack.pushPose();

        float time = 0;
        if (blockEntity.hasLevel()) {
            time = blockEntity.getLevel().getGameTime() + partialTicks;
        }

        // Move text 2 blocks up
        float yOffset = 3.7F + (float) Math.sin(time / 20.0F) * 0.1F;
        poseStack.translate(0.5D, yOffset, 0.5D);

        poseStack.mulPose(Minecraft.getInstance().getEntityRenderDispatcher().cameraOrientation());

        float scale = 0.025f;
        poseStack.scale(-scale, -scale, scale);

        float textWidth = -this.font.width(text) / 2.0f;

        this.font.drawInBatch(text, textWidth, 0, 0x33FF33, false, poseStack.last().pose(), buffer, Font.DisplayMode.NORMAL, 0, packedLight);
        poseStack.popPose();
    }
} 