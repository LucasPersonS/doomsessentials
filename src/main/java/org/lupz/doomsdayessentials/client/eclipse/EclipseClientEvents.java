package org.lupz.doomsdayessentials.client.eclipse;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec2;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lupz.doomsdayessentials.EssentialsMod;

@Mod.EventBusSubscriber(modid = EssentialsMod.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class EclipseClientEvents {

    private EclipseClientEvents() {}

    @SubscribeEvent
    public static void onRenderFog(ViewportEvent.RenderFog event) {
        if (!EclipseClientState.isActive()) return;
        event.setCanceled(false);
        event.setNearPlaneDistance(EclipseClientState.getFogNear());
        event.setFarPlaneDistance(EclipseClientState.getFogFar());
    }

    @SubscribeEvent
    public static void onComputeFogColor(ViewportEvent.ComputeFogColor event) {
        if (!EclipseClientState.isActive()) return;
        float a = 0.7f;
        event.setRed(event.getRed() * a);
        event.setGreen(event.getGreen() * a);
        event.setBlue(event.getBlue() * a);
    }

    @SubscribeEvent
    public static void onRenderGuiOverlay(RenderGuiOverlayEvent.Pre event) {
        if (!EclipseClientState.isActive()) return;
        var id = event.getOverlay().id();
        // Cancel hotbar e outros, deixando apenas corações (PLAYER_HEALTH)
        if (id.equals(VanillaGuiOverlay.HOTBAR.id()) ||
            id.equals(VanillaGuiOverlay.ARMOR_LEVEL.id()) ||
            id.equals(VanillaGuiOverlay.FOOD_LEVEL.id()) ||
            id.equals(VanillaGuiOverlay.EXPERIENCE_BAR.id())) {
            event.setCanceled(true);
        }

        // Desenhar overlay uma vez por frame, ancorado em PLAYER_HEALTH (antes dos corações renderizarem)
        if (!id.equals(VanillaGuiOverlay.PLAYER_HEALTH.id())) return;
        float alpha = EclipseClientState.getOverlayAlpha();
        if (alpha <= 0.01f) return;
        int width = event.getWindow().getGuiScaledWidth();
        int height = event.getWindow().getGuiScaledHeight();

        RenderSystem.disableDepthTest();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionShader);
        RenderSystem.setShaderColor(0.0F, 0.0F, 0.0F, alpha);

        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder builder = tesselator.getBuilder();
        builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION);
        builder.vertex(0, 0, -90).endVertex();
        builder.vertex(0, height, -90).endVertex();
        builder.vertex(width, height, -90).endVertex();
        builder.vertex(width, 0, -90).endVertex();
        tesselator.end();

        RenderSystem.disableBlend();
        RenderSystem.enableDepthTest();
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (!EclipseClientState.isActive()) return;
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_SKY) return;

        var mc = Minecraft.getInstance();
        var level = mc.level;
        if (level == null) return;

        float celestial = level.getTimeOfDay(event.getPartialTick());
        float size = 12.0f; // cover sun disk

        var pose = event.getPoseStack();
        var buf = mc.renderBuffers().bufferSource();
        pose.pushPose();
        // Match vanilla celestial transforms: rotate around Y then around X by celestial angle
        pose.mulPose(Axis.YP.rotationDegrees(-90.0F));
        pose.mulPose(Axis.XP.rotationDegrees(celestial * 360.0F));

        var vc = buf.getBuffer(RenderType.solid());
        int light = 0x00F000F0; // fullbright
        var last = pose.last();
        var m = last.pose();
        var n = last.normal();
        float y = 100.0f; // sky plane height used by vanilla
        vc.vertex(m, -size, y, -size).color(0,0,0,255).uv(0,0).overlayCoords(net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY).uv2(light).normal(n,0,-1,0).endVertex();
        vc.vertex(m,  size, y, -size).color(0,0,0,255).uv(1,0).overlayCoords(net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY).uv2(light).normal(n,0,-1,0).endVertex();
        vc.vertex(m,  size, y,  size).color(0,0,0,255).uv(1,1).overlayCoords(net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY).uv2(light).normal(n,0,-1,0).endVertex();
        vc.vertex(m, -size, y,  size).color(0,0,0,255).uv(0,1).overlayCoords(net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY).uv2(light).normal(n,0,-1,0).endVertex();

        pose.popPose();
        buf.endBatch();
    }
} 