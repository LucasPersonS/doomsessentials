package org.lupz.doomsdayessentials.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import org.lupz.doomsdayessentials.EssentialsMod;

/**
 * Auto-generated Blockbench model. Rendered via RecycleBlockRenderer.
 */
public class RecycleModel2<T extends Entity> extends EntityModel<T> {

    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(EssentialsMod.MOD_ID, "recyclemodel2"), "main");

    private final ModelPart bb_main;

    public RecycleModel2(ModelPart root) {
        this.bb_main = root.getChild("bb_main");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();

        PartDefinition bb_main = partdefinition.addOrReplaceChild("bb_main", CubeListBuilder.create().texOffs(0, 0).addBox(-23.0F, -29.0F, -12.0F, 45.0F, 18.0F, 23.0F, new CubeDeformation(0.0F))
                .texOffs(96, 69).addBox(-23.0F, -11.0F, 8.0F, 3.0F, 11.0F, 3.0F, new CubeDeformation(0.0F))
                .texOffs(96, 69).addBox(19.0F, -11.0F, 8.0F, 3.0F, 11.0F, 3.0F, new CubeDeformation(0.0F))
                .texOffs(96, 69).addBox(19.0F, -11.0F, -11.75F, 3.0F, 11.0F, 3.0F, new CubeDeformation(0.0F))
                .texOffs(96, 69).addBox(-23.0F, -11.0F, -11.75F, 3.0F, 11.0F, 3.0F, new CubeDeformation(0.0F))
                .texOffs(0, 100).addBox(-13.0F, -20.0F, 11.0F, 3.0F, 3.0F, 3.0F, new CubeDeformation(0.0F))
                .texOffs(12, 100).addBox(-12.0F, -17.0F, 14.0F, 1.0F, 12.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 24.0F, 0.0F));

        bb_main.addOrReplaceChild("cube_r1", CubeListBuilder.create().texOffs(0, 54).addBox(-23.0F, -7.5F, -11.0F, 45.0F, 12.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -37.25F, -3.5F, 0.3927F, 0.0F, 0.0F));
        bb_main.addOrReplaceChild("cube_r2", CubeListBuilder.create().texOffs(45, 66).addBox(-1.0F, -11.0F, -12.0F, 1.0F, 11.0F, 23.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-22.0F, -29.25F, 0.0F, 0.0F, 0.0F, -0.3927F));
        bb_main.addOrReplaceChild("cube_r3", CubeListBuilder.create().texOffs(2, 65).addBox(0.0F, -12.0F, -12.0F, 1.0F, 12.0F, 23.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(21.0F, -29.25F, 0.0F, 0.0F, 0.0F, 0.3927F));
        bb_main.addOrReplaceChild("cube_r4", CubeListBuilder.create().texOffs(1, 41).addBox(-23.0F, -7.5F, 10.0F, 45.0F, 12.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -37.25F, 2.5F, -0.3927F, 0.0F, 0.0F));

        return LayerDefinition.create(meshdefinition, 256, 256);
    }

    @Override
    public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        // Static model
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
        bb_main.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
    }
} 