package org.lupz.doomsdayessentials.client;

import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lupz.doomsdayessentials.EssentialsMod;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;

@Mod.EventBusSubscriber(modid = EssentialsMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class MadnessClientEvents {

    private static boolean shaderApplied = false;

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        // ---------------------------------------------------------------------
        // Phase.START → invert controles antes da lógica de movimento do jogador
        // ---------------------------------------------------------------------
        if (event.phase == TickEvent.Phase.START) {
            if (MadnessClientState.isActive()) {
                Minecraft mc = Minecraft.getInstance();
                if (mc.player != null) {
                    mc.player.input.leftImpulse *= -1;
                    mc.player.input.forwardImpulse *= -1;
                }
            }
            return; // não processa o restante nesta fase
        }

        // ---------------------------------------------------------------------
        // Phase.END → shake da câmera, overlay e shader
        // ---------------------------------------------------------------------
        if (event.phase != TickEvent.Phase.END) return;

        Minecraft mc = Minecraft.getInstance();

        if (MadnessClientState.isActive()) {
            if (mc.level == null || mc.player == null) return;

            MadnessClientState.tick();

            float intensity = MadnessClientState.getShakeIntensity();
            if (intensity > 0) {
                double yawShake = (mc.player.getRandom().nextDouble() - 0.5) * intensity * 4.0;
                double pitchShake = (mc.player.getRandom().nextDouble() - 0.5) * intensity * 4.0;

                mc.player.setYRot(mc.player.getYRot() + (float) yawShake);
                mc.player.setXRot(mc.player.getXRot() + (float) pitchShake);
            }

            // Ativar shader, se necessário
            if (!shaderApplied) {
                ResourceLocation rl = ResourceLocation.parse("minecraft:shaders/post/wobble.json");
                mc.gameRenderer.loadEffect(rl);
                shaderApplied = true;
            }
        } else {
            // Efeito terminou – desligar shader se ainda ativo
            if (shaderApplied) {
                mc.gameRenderer.shutdownEffect();
                shaderApplied = false;
            }
        }
    }

    @SubscribeEvent
    public static void onRenderGuiOverlay(RenderGuiOverlayEvent.Pre event) {
        if (!MadnessClientState.isActive()) return;
        // Render this overlay only once per frame, anchored on PLAYER_HEALTH
        if (!event.getOverlay().id().equals(VanillaGuiOverlay.PLAYER_HEALTH.id())) return;
        Minecraft mc = Minecraft.getInstance();
        float alpha = Math.min(1.0f, MadnessClientState.getOverlayIntensity() * 0.6f);
        if (alpha <= 0) return;

        int width = event.getWindow().getGuiScaledWidth();
        int height = event.getWindow().getGuiScaledHeight();

        RenderSystem.disableDepthTest();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionShader);
        RenderSystem.setShaderColor(0.4F, 0.0F, 0.0F, alpha);

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

        // If later you want to add a distortion shader, you can re-enable the code below.
        // Currently we rely only on the semi-transparent overlay and camera shake,
        // because the vanilla color shader was tinting the whole screen de forma exagerada.
    }
} 