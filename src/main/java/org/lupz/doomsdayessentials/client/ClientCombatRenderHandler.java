package org.lupz.doomsdayessentials.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.event.RenderNameTagEvent;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;
import org.lupz.doomsdayessentials.EssentialsMod;
import org.lupz.doomsdayessentials.combat.AreaType;
import org.lupz.doomsdayessentials.combat.ManagedArea;
import org.lupz.doomsdayessentials.sound.ModSounds;

@Mod.EventBusSubscriber(modid = EssentialsMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class ClientCombatRenderHandler {

    private static ClientCombatRenderHandler instance;

    public static void init() {
        if (instance == null) {
            instance = new ClientCombatRenderHandler();
            MinecraftForge.EVENT_BUS.register(instance);
        }
    }

    private static final ResourceLocation COMBAT_ICON = ResourceLocation.fromNamespaceAndPath(EssentialsMod.MOD_ID, "textures/gui/combat_icon.png");
    private static final ResourceLocation DANGER_ZONE_TEXTURE = ResourceLocation.fromNamespaceAndPath(EssentialsMod.MOD_ID, "textures/gui/danger_zone.png");
    private static final ResourceLocation SAFE_ZONE_TEXTURE = ResourceLocation.fromNamespaceAndPath(EssentialsMod.MOD_ID, "textures/gui/safe_zone.png");
    private static final ResourceLocation IN_COMBAT_ICON = ResourceLocation.fromNamespaceAndPath(EssentialsMod.MOD_ID, "textures/gui/incombat.png");
    private static final ResourceLocation FREQUENCY_OVERLAY = ResourceLocation.fromNamespaceAndPath(EssentialsMod.MOD_ID, "textures/misc/frequencia_overlay.png");

    private static boolean wasInDangerZone = false;
    private static boolean wasInSafeArea = false;
    private static boolean wasInFrequencyZone = false;

    private ClientCombatRenderHandler() {}

    // ---------------------------------------------------------------------
    // Line-of-sight helper
    // ---------------------------------------------------------------------

    /**
     * Returns true if the local (client) player can directly see the target entity without any blocks obstructing.
     */
    private static boolean hasLineOfSight(Entity target) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return true; // no info → assume visible

        Vec3 start = mc.player.getEyePosition(1.0f);
        Vec3 end = target.getEyePosition(1.0f);

        var context = new net.minecraft.world.level.ClipContext(start, end, net.minecraft.world.level.ClipContext.Block.COLLIDER, net.minecraft.world.level.ClipContext.Fluid.NONE, mc.player);
        HitResult result = mc.level.clip(context);

        // Visible if nothing hit or the hit is the target itself
        return result.getType() == HitResult.Type.MISS ||
               (result instanceof BlockHitResult bhr && bhr.getType() == HitResult.Type.MISS);
    }

    @SubscribeEvent
    public void onRenderNameplate(RenderNameTagEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        // Standard player checks
        if (player == Minecraft.getInstance().player && !Minecraft.getInstance().options.getCameraType().isFirstPerson()) {
            // allow rendering own tag in third person for debugging
        } else if (player == Minecraft.getInstance().player) {
            return; // don't render own tag in first person
        }

        if (player.isCrouching()) return; // Hide tag if crouching
        
        // Hide if behind wall
        if (!hasLineOfSight(player)) {
            event.setResult(RenderNameTagEvent.Result.DENY); // cancel vanilla
            return;
        }
        
        // Determine player's status
        ManagedArea area = ClientCombatState.getPlayerArea(player);
        boolean inDanger = area != null && area.getType() == AreaType.DANGER;
        boolean inCombat = ClientCombatState.isPlayerInCombat(player.getUUID());

        if (inDanger || inCombat) {
            event.setResult(RenderNameTagEvent.Result.DENY); // Cancel original tag
            Component redName = player.getName().copy().withStyle(ChatFormatting.RED);
            renderCustomNameTag(event, redName, player);
        }
    }

    private void renderCustomNameTag(RenderNameTagEvent event, Component name, Player player) {
        PoseStack poseStack = event.getPoseStack();
        poseStack.pushPose();
        poseStack.translate(0.0, player.getEyeHeight() + 0.75F, 0.0);
        poseStack.mulPose(Minecraft.getInstance().getEntityRenderDispatcher().cameraOrientation());
        poseStack.scale(-0.025F, -0.025F, 0.025F);
        
        Font font = Minecraft.getInstance().font;
        float x = -font.width(name) / 2.0f;
        
        font.drawInBatch(name, x, 0f, 0xFFFFFF, false, poseStack.last().pose(), event.getMultiBufferSource(), Font.DisplayMode.SEE_THROUGH, 0, event.getPackedLight());
        poseStack.popPose();
    }

    @SubscribeEvent
    public void onRenderGuiOverlay(RenderGuiOverlayEvent.Pre event) {
        if (event.getOverlay() != VanillaGuiOverlay.PLAYER_HEALTH.type()) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        GuiGraphics guiGraphics = event.getGuiGraphics();
        if (minecraft.player == null || minecraft.options.hideGui) {
            return;
        }

        Player player = minecraft.player;
        ManagedArea currentArea = ClientCombatState.getPlayerArea(player);

        boolean inCombat = ClientCombatState.isPlayerInCombat(player.getUUID());
        boolean inSafe = currentArea != null && currentArea.getType() == AreaType.SAFE;
        boolean inDanger = currentArea != null && currentArea.getType() == AreaType.DANGER;
        long combatEndTime = ClientCombatState.getCombatEndTime(player.getUUID());
        int remainingSeconds = (int) Math.max(0, (combatEndTime - System.currentTimeMillis()) / 1000);

        int screenWidth = minecraft.getWindow().getGuiScaledWidth();
        int screenHeight = minecraft.getWindow().getGuiScaledHeight();
        Font font = minecraft.font;

        if (inCombat && !inSafe) {
            int iconSrcW = 95;
            int iconSrcH = 22;

            int guiScaleSetting = minecraft.options.guiScale().get();
            float scaleFactor = 1.0F;
            if (guiScaleSetting >= 4 || (guiScaleSetting == 0 && screenWidth < 500)) {
                scaleFactor = 0.7F;
            } else if (guiScaleSetting == 3 || (guiScaleSetting == 0 && screenWidth < 700)) {
                scaleFactor = 0.8F;
            }

            int iconW = Math.round(iconSrcW * scaleFactor);
            int iconH = Math.round(iconSrcH * scaleFactor);
            int xIcon = (screenWidth - iconW) / 2;
            int yIcon = screenHeight - iconH - 50;

            RenderSystem.setShaderTexture(0, IN_COMBAT_ICON);
            RenderSystem.enableBlend();

            PoseStack poseStack = guiGraphics.pose();
            poseStack.pushPose();
            poseStack.translate(xIcon, yIcon, 0);
            poseStack.scale(scaleFactor, scaleFactor, 1.0f);
            guiGraphics.blit(IN_COMBAT_ICON, 0, 0, 0, 0, iconSrcW, iconSrcH, iconSrcW, iconSrcH);
            poseStack.popPose();

            RenderSystem.disableBlend();

            // Draw countdown when applicable – hide if in danger zone or permanent combat mode
            boolean alwaysActive = ClientCombatState.isPlayerAlwaysActive(player.getUUID());
            if (remainingSeconds > 0 && !inDanger && !alwaysActive) {
                Component timer = Component.literal(String.valueOf(remainingSeconds)).withStyle(ChatFormatting.YELLOW);
                int w = font.width(timer);
                guiGraphics.drawString(font, timer, (screenWidth - w) / 2, yIcon + iconH + 4, 0xFFFFFF, true);
            }
        } else if (inSafe) {
            Component safeText = Component.literal("ᴠᴏᴄê ᴇsᴛá ᴇᴍ ᴜᴍᴀ ᴢᴏɴᴀ ѕᴇɢᴜʀᴀ").withStyle(ChatFormatting.BOLD, ChatFormatting.GREEN);
            int textWidth = font.width(safeText);
            int x = (screenWidth - textWidth) / 2;
            int y = screenHeight - 59; // Position above the hotbar.
            guiGraphics.drawString(font, safeText, x, y, 0xFFFFFF, true);
        }

        // Render Area Status Icon (Danger/Safe)
        if (currentArea != null) {
            if (currentArea.getType() == AreaType.DANGER) {
                renderDangerZoneOverlay(guiGraphics);
            } else if (currentArea.getType() == AreaType.SAFE) {
                renderSafeAreaOverlay(guiGraphics);
            }
        }

        // Full-screen red overlay when inside a Frequency zone (if player is not immune)
        boolean inFrequency = currentArea != null && currentArea.getType() == AreaType.FREQUENCY;
        if (inFrequency && !minecraft.player.hasEffect(org.lupz.doomsdayessentials.effect.ModEffects.FREQUENCY.get())) {
            RenderSystem.disableDepthTest();
            RenderSystem.depthMask(false);
            guiGraphics.fill(0, 0, screenWidth, screenHeight, 0x33FF0000); // Semi-transparent red
        }
    }

    private void renderSafeAreaOverlay(GuiGraphics guiGraphics) {
        RenderSystem.enableBlend();
        int screenHeight = guiGraphics.guiHeight();
        guiGraphics.blit(SAFE_ZONE_TEXTURE, 10, screenHeight - 64 - 5, 0, 0, 64, 64, 64, 64);
        RenderSystem.disableBlend();
    }

    private void renderDangerZoneOverlay(GuiGraphics guiGraphics) {
        RenderSystem.enableBlend();
        int screenHeight = guiGraphics.guiHeight();

        PoseStack stack = guiGraphics.pose();
        stack.pushPose();
        stack.translate(10, screenHeight - 96 - 5, 0);
        stack.scale(1.5f, 1.5f, 1f);
        RenderSystem.setShaderTexture(0, DANGER_ZONE_TEXTURE);
        guiGraphics.blit(DANGER_ZONE_TEXTURE, 0, 0, 0, 0, 64, 64, 64, 64);
        stack.popPose();
        RenderSystem.disableBlend();
    }

    @SubscribeEvent
    public void onRenderPlayerPost(RenderPlayerEvent.Post event) {
        Player player = event.getEntity();
        if (player.isCrouching()) return; // Hide icon if crouching

        ManagedArea area = ClientCombatState.getPlayerArea(player);
        boolean inDanger = area != null && area.getType() == AreaType.DANGER;
        boolean inCombat = ClientCombatState.isPlayerInCombat(player.getUUID());

        if (inCombat || inDanger) {
            renderCombatIcon(event.getPoseStack(), player, event.getPackedLight());
        }
    }

    private void renderCombatIcon(PoseStack poseStack, Player player, int packedLight) {
        // Hide if behind wall
        if (!hasLineOfSight(player)) return;

        poseStack.pushPose();
        // Adjust position to be above the head
        poseStack.translate(0.0D, player.getBbHeight() + 0.5D, 0.0D);
        poseStack.mulPose(Minecraft.getInstance().getEntityRenderDispatcher().cameraOrientation());
        poseStack.scale(-0.025f, -0.025f, -0.025f);

        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, COMBAT_ICON);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        Tesselator tesselator = Tesselator.getInstance();
        var builder = tesselator.getBuilder();
        builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        
        float size = 8.0f;
        builder.vertex(poseStack.last().pose(), -size, -size, 0).uv(0, 0).endVertex();
        builder.vertex(poseStack.last().pose(), -size, size, 0).uv(0, 1).endVertex();
        builder.vertex(poseStack.last().pose(), size, size, 0).uv(1, 1).endVertex();
        builder.vertex(poseStack.last().pose(), size, -size, 0).uv(1, 0).endVertex();
        
        tesselator.end();
        RenderSystem.disableBlend();
        poseStack.popPose();
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;

        boolean nowInDanger = ClientCombatState.isInDangerArea();
        if (nowInDanger && !wasInDangerZone) {
            playSound(ModSounds.DANGER_ZONE_ENTER.get());
        }
        wasInDangerZone = nowInDanger;

        boolean nowInSafe = ClientCombatState.isInSafeArea();
        if (nowInSafe && !wasInSafeArea) {
            playSound(ModSounds.SAFE_ZONE_ENTER.get());
        }
        wasInSafeArea = nowInSafe;
        
        // Frequency zone entry sound
        ManagedArea curArea = ClientCombatState.getPlayerArea(mc.player);
        boolean nowInFreq = curArea != null && curArea.getType() == AreaType.FREQUENCY;
        if (nowInFreq && !wasInFrequencyZone) {
            playSound(org.lupz.doomsdayessentials.sound.ModSounds.FREQUENCIA1.get());
        }
        wasInFrequencyZone = nowInFreq;

        // Particle spawning logic can be added here if desired.
    }
    
    private void playSound(SoundEvent sound) {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(sound, 1.0F));
    }
} 