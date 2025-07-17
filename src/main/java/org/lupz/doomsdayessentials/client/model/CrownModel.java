package org.lupz.doomsdayessentials.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import org.lupz.doomsdayessentials.EssentialsMod;

/**
 * Very simple 3-D crown that sits on the player's head.  Built at runtime so the user doesn't
 * have to paste the long Blockbench export.  If they export their own Java model, they can swap
 * this out without touching any of the registration code.
 */
public class CrownModel<T extends LivingEntity> extends HumanoidModel<T> {

    public static final ModelLayerLocation LAYER_LOCATION =
            new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(EssentialsMod.MOD_ID, "crown"), "main");

    public CrownModel(ModelPart root) {
        // Base humanoid model setup
        super(root);
    }

    /**
     * Builds a very small ring + four spikes.  Replace with a full export if desired.
     */
    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = HumanoidModel.createMesh(CubeDeformation.NONE, 0.0F);
        PartDefinition root = mesh.getRoot();

        PartDefinition head = root.getChild("head");

        // Auto-generated from Blockbench coroa.json (pixel-perfect) – coordinates converted:
        record Box(float fx, float fy, float fz, float tx, float ty, float tz) {}
        Box[] boxes = new Box[]{
                new Box(3.5f,0,3.75f,12.5f,5,12.75f),
                new Box(3.25f,0,3.75f,3.5f,2,12.75f),
                new Box(12.5f,0,3.75f,12.75f,2,12.75f),
                new Box(3.25f,0,12.7f,12.65f,2,12.95f),
                new Box(3.25f,0,3.55f,12.65f,2,3.8f),
                new Box(3.4f,2,3.7f,3.4f,4,5.7f),
                new Box(12.5f,2,3.7f,12.5f,4,5.7f),
                new Box(3.4f,2,10.8f,3.4f,4,12.8f),
                new Box(12.6f,2,10.8f,12.6f,4,12.8f),
                new Box(10.7f,2,12.8f,12.7f,4,12.8f),
                new Box(10.7f,2,3.6f,12.7f,4,3.6f),
                new Box(3.4f,2,12.8f,5.4f,4,12.8f),
                new Box(3.4f,2,3.7f,5.4f,4,3.7f),
                new Box(3.4f,1.6f,7.2f,3.5f,4.9f,9.3f),
                new Box(12.5f,1.6f,7.2f,12.6f,4.9f,9.3f),
                new Box(6.8f,1.6f,12.8f,8.9f,4.9f,12.9f),
                new Box(6.8f,1.6f,3.6f,8.9f,4.9f,3.7f),
                new Box(6.8f,4.6f,7.3f,8.8f,7.4f,9.3f)
        };

        for (int i = 0; i < boxes.length; i++) {
            Box b = boxes[i];
            float x = b.fx - 8.0f;
            float y = b.fy - 8.0f; // convert block Y to model Y (head top is -8)
            float z = b.fz - 8.0f;
            float dx = b.tx - b.fx;
            float dy = b.ty - b.fy;
            float dz = b.tz - b.fz;
            head.addOrReplaceChild("bb"+i, CubeListBuilder.create()
                            .texOffs(0,0)
                            .addBox(x, y, z, dx, dy, dz, CubeDeformation.NONE), PartPose.ZERO);
        }

        return LayerDefinition.create(mesh, 32, 32);
    }

    // The base HumanoidModel already defines an appropriate RenderType; custom override removed for 1.20.1 compatibility.

    // No extra animation; fallback to HumanoidModel's default behaviour
} 