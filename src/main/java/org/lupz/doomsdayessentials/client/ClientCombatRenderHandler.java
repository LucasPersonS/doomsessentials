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

    private static boolean wasInDangerZone = false;
    private static boolean wasInSafeArea = false;

    private ClientCombatRenderHandler() {}

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

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }

        ManagedArea currentArea = ClientCombatState.getPlayerArea(mc.player);
        boolean inDanger = currentArea != null && currentArea.getType() == AreaType.DANGER;
        boolean inSafe = currentArea != null && currentArea.getType() == AreaType.SAFE;
        boolean inCombat = ClientCombatState.isPlayerInCombat(mc.player.getUUID());
        long combatEndTime = ClientCombatState.getCombatEndTime(mc.player.getUUID());

        GuiGraphics guiGraphics = event.getGuiGraphics();
        PoseStack poseStack = guiGraphics.pose();
        poseStack.pushPose();

        int screenWidth = event.getWindow().getGuiScaledWidth();
        int screenHeight = event.getWindow().getGuiScaledHeight();
        Font font = mc.font;

        // Render Combat Status & Timer
        if (inCombat || inDanger) {
            long remainingSeconds = (combatEndTime - System.currentTimeMillis()) / 1000;
            Component combatText = Component.literal("ᴇᴍ ᴄᴏᴍʙᴀᴛᴇ").withStyle(ChatFormatting.BOLD, ChatFormatting.RED);

            if (remainingSeconds > 0 && !inDanger) {
                // Only show timer when not in a danger zone
                 Component timerText = Component.literal(" (").withStyle(ChatFormatting.RED)
                     .append(Component.literal(String.valueOf(remainingSeconds)).withStyle(ChatFormatting.YELLOW))
                     .append(Component.literal("s)").withStyle(ChatFormatting.RED));
                combatText = combatText.copy().append(timerText);
            }

            int textWidth = font.width(combatText);
            int x = (screenWidth - textWidth) / 2;
            int y = screenHeight - 59; // Position above the hotbar.
            guiGraphics.drawString(font, combatText, x, y, 0xFFFFFF, true);

            // Render combat icon
            RenderSystem.setShaderTexture(0, COMBAT_ICON);
            int iconX = x - 20;
            int iconY = y - 2;
            guiGraphics.blit(COMBAT_ICON, iconX, iconY, 0, 0, 16, 16, 16, 16);
        } else if (inSafe) {
            Component safeText = Component.literal("ᴠᴏᴄê ᴇѕᴛá ᴇᴍ ᴜᴍᴀ ᴢᴏɴᴀ ѕᴇɢᴜʀᴀ").withStyle(ChatFormatting.BOLD, ChatFormatting.GREEN);
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

        poseStack.popPose();
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
        guiGraphics.blit(DANGER_ZONE_TEXTURE, 10, screenHeight - 64 - 5, 0, 0, 64, 64, 64, 64);
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
        
        // Particle spawning logic can be added here if desired, based on the example.
    }
    
    private void playSound(SoundEvent sound) {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(sound, 1.0F));
    }
} 