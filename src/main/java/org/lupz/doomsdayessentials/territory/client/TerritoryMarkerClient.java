package org.lupz.doomsdayessentials.territory.client;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lupz.doomsdayessentials.EssentialsMod;
import org.lupz.doomsdayessentials.network.packet.s2c.TerritoryMarkerPacket;

import java.util.HashMap;
import java.util.Map;

@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(modid = EssentialsMod.MOD_ID, value = Dist.CLIENT)
public class TerritoryMarkerClient {
    private static final ResourceLocation CONTESTED_TEX = ResourceLocation.fromNamespaceAndPath(EssentialsMod.MOD_ID, "textures/misc/area_contested.png");
    private static final ResourceLocation DOMINATING_TEX = ResourceLocation.fromNamespaceAndPath(EssentialsMod.MOD_ID, "textures/misc/area_dominating.png");
    private static final ResourceLocation DOMINATED_TEX  = ResourceLocation.fromNamespaceAndPath(EssentialsMod.MOD_ID, "textures/misc/area_dominated.png");
    private static final ResourceLocation CLOSED_TEX  = ResourceLocation.fromNamespaceAndPath(EssentialsMod.MOD_ID, "textures/gui/closed_zone.png");

    private static final Map<String, Marker> MARKERS = new HashMap<>();

    public record Marker(double x, double y, double z, byte status) {}

    public static void handlePacket(TerritoryMarkerPacket pkt) {
        if (pkt.status() == 0) {
            MARKERS.remove(pkt.areaName());
        } else {
            MARKERS.put(pkt.areaName(), new Marker(pkt.cx(), pkt.cy(), pkt.cz(), pkt.status()));
        }
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent e) {
        if (e.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) return;
        Minecraft mc = Minecraft.getInstance();
        PoseStack pose = e.getPoseStack();
        MultiBufferSource.BufferSource buf = mc.renderBuffers().bufferSource();
        if (MARKERS.isEmpty()) return;
        for (Map.Entry<String, Marker> entry : MARKERS.entrySet()) {
            drawBillboard(pose, buf, entry.getValue(), entry.getKey());
        }
        buf.endBatch();
    }

    private static void drawBillboard(PoseStack stack, MultiBufferSource buf, Marker marker, String areaName) {
        Minecraft mc = Minecraft.getInstance();
        double camX = mc.gameRenderer.getMainCamera().getPosition().x;
        double camY = mc.gameRenderer.getMainCamera().getPosition().y;
        double camZ = mc.gameRenderer.getMainCamera().getPosition().z;

        stack.pushPose();
        stack.translate(marker.x - camX, marker.y - camY, marker.z - camZ);
        // rotate to face camera
        stack.mulPose(mc.gameRenderer.getMainCamera().rotation());
        float scale = marker.status==4?5f:1f;
        stack.scale(scale, scale, scale);

        ResourceLocation tex;
        if (marker.status == 1) tex = CONTESTED_TEX;
        else if (marker.status == 2) tex = DOMINATING_TEX;
        else if (marker.status == 4) tex = CLOSED_TEX;
        else tex = DOMINATED_TEX;
        RenderType rt = RenderType.entityCutoutNoCull(tex);
        VertexConsumer vc = buf.getBuffer(rt);

        float halfWidth = 1f; // width: 1.0
        float halfHeight = 0.5f; // height: 0.25 (for 4:1 aspect ratio)
        int light = 0x00F000F0; // full bright
        // Fix orientation: swap both U and V for each vertex
        vc.vertex(stack.last().pose(), -halfWidth, -halfHeight, 0)
          .color(255,255,255,255)
          .uv(1,1)
          .overlayCoords(net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY)
          .uv2(light)
          .normal(stack.last().normal(),0,1,0)
          .endVertex();
        vc.vertex(stack.last().pose(), halfWidth, -halfHeight, 0)
          .color(255,255,255,255)
          .uv(0,1)
          .overlayCoords(net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY)
          .uv2(light)
          .normal(stack.last().normal(),0,1,0)
          .endVertex();
        vc.vertex(stack.last().pose(), halfWidth, halfHeight, 0)
          .color(255,255,255,255)
          .uv(0,0)
          .overlayCoords(net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY)
          .uv2(light)
          .normal(stack.last().normal(),0,1,0)
          .endVertex();
        vc.vertex(stack.last().pose(), -halfWidth, halfHeight, 0)
          .color(255,255,255,255)
          .uv(1,0)
          .overlayCoords(net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY)
          .uv2(light)
          .normal(stack.last().normal(),0,1,0)
          .endVertex();
        stack.popPose();

        // Removed floating text rendering to keep only PNG billboard
    }
} 