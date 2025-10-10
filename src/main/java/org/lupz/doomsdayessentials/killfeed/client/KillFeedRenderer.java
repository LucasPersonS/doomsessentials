package org.lupz.doomsdayessentials.killfeed.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lupz.doomsdayessentials.EssentialsMod;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import java.awt.*;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Set;

@Mod.EventBusSubscriber(modid = EssentialsMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class KillFeedRenderer {
	private static final long FADE_DURATION_MS = 500L;

	// Cache for resolved gun icon textures: key is namespace:path weapon id, value is RL or null if not found
	private static final Map<String, ResourceLocation> GUN_ICON_CACHE = new ConcurrentHashMap<>();
    // Cache for resolved TACZ slot textures from display JSON
    private static final Map<String, ResourceLocation> SLOT_TEXTURE_CACHE = new ConcurrentHashMap<>();
    private static final Set<String> SLOT_TEXTURE_MISS = ConcurrentHashMap.newKeySet();

    private static ResourceLocation resolveTaczSlotTexture(String weaponId) {
        if (weaponId == null || weaponId.isBlank() || !weaponId.contains(":")) return null;
        if (SLOT_TEXTURE_MISS.contains(weaponId)) return null;
        ResourceLocation cached = SLOT_TEXTURE_CACHE.get(weaponId);
        if (cached != null) return cached;

        try {
            String[] parts = weaponId.split(":", 2);
            String ns = parts[0];
            String path = parts[1];
            // TACZ display file convention: display/guns/<id>_display.json
            ResourceLocation displayJson = ResourceLocation.fromNamespaceAndPath(ns, "display/guns/" + path + "_display.json");
            ResourceManager rm = Minecraft.getInstance().getResourceManager();
            var resOpt = rm.getResource(displayJson);
            if (resOpt.isEmpty()) {
                SLOT_TEXTURE_MISS.add(weaponId);
                return null;
            }
            try (var is = resOpt.get().open();
                 var br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                JsonObject root = JsonParser.parseReader(br).getAsJsonObject();
                if (!root.has("slot")) { SLOT_TEXTURE_MISS.add(weaponId); return null; }
                String slotStr = root.get("slot").getAsString(); // e.g. doomsday:gun/slot/ak102
                ResourceLocation slotRl = ResourceLocation.parse(slotStr);
                // Convert to texture path: textures/<slotRl.path>.png
                ResourceLocation textureRl = ResourceLocation.fromNamespaceAndPath(slotRl.getNamespace(), "textures/" + slotRl.getPath() + ".png");
                // Verify it exists
                if (rm.getResource(textureRl).isPresent()) {
                    SLOT_TEXTURE_CACHE.put(weaponId, textureRl);
                    return textureRl;
                }
            }
        } catch (Exception ignored) {}
        SLOT_TEXTURE_MISS.add(weaponId);
        return null;
    }

	@SubscribeEvent
	public static void onRenderGuiOverlay(RenderGuiOverlayEvent.Post event) {
		// Render once per frame anchored to the final overlay to avoid duplicate draws
		if (!event.getOverlay().id().equals(VanillaGuiOverlay.PLAYER_HEALTH.id())) return;

		Minecraft mc = Minecraft.getInstance();
		if (mc.options.hideGui) return;

		// Killcard overlay (center)
		renderKillcard(event);

		List<KillFeedManager.KillFeedEntry> entries = KillFeedManager.getEntries();
		if (entries.isEmpty()) return;

		GuiGraphics guiGraphics = event.getGuiGraphics();
		int screenWidth = event.getWindow().getGuiScaledWidth();
		int y = 2;

		for (KillFeedManager.KillFeedEntry entry : entries) {
			long age = System.currentTimeMillis() - entry.creationTime();
			float alpha = 1.0f;
			if (age < FADE_DURATION_MS) {
				alpha = (float) age / FADE_DURATION_MS;
			} else if (age > KillFeedManager.ENTRY_DURATION_MS - FADE_DURATION_MS) {
				alpha = 1.0f - (float) (age - (KillFeedManager.ENTRY_DURATION_MS - FADE_DURATION_MS)) / FADE_DURATION_MS;
			}
			alpha = Math.max(0.0f, Math.min(1.0f, alpha));
			int alphaInt = (int) (alpha * 255);

			MutableComponent killMessage;
			boolean isPvpKill = entry.killerUUID() != null && !entry.killerUUID().equals(entry.victimUUID());
			boolean isMobKill = entry.killerUUID() == null && entry.killer() != null && !entry.killer().isBlank();

			if (isPvpKill) {
				killMessage = Component.literal(entry.killer()).withStyle(ChatFormatting.RED)
					.append(Component.literal(" ").withStyle(ChatFormatting.WHITE))
					.append(Component.literal(entry.weaponName()).withStyle(ChatFormatting.WHITE))
					.append(Component.literal(" ").withStyle(ChatFormatting.WHITE))
					.append(Component.literal(entry.victim()).withStyle(ChatFormatting.GREEN));
			} else if (isMobKill) {
				killMessage = Component.literal(entry.victim()).withStyle(ChatFormatting.RED)
					.append(Component.literal(" was slain by ").withStyle(ChatFormatting.WHITE))
					.append(Component.literal(entry.killer()).withStyle(ChatFormatting.GOLD));
			} else {
				if ("/kill".equals(entry.weaponName())) {
					killMessage = Component.literal(entry.victim()).withStyle(ChatFormatting.RED)
						.append(Component.literal(" SUICIDE").withStyle(ChatFormatting.RED));
				} else {
					killMessage = Component.literal(entry.victim()).withStyle(ChatFormatting.RED)
						.append(Component.literal(" " + entry.weaponName()).withStyle(ChatFormatting.WHITE));
				}
			}

			Font font = mc.font;
			int textWidth = font.width(killMessage);
			int padding = 4;
			int ySize = 12 + padding;
			int iconSize = 12;
			int headSize = 12;

			String gunIdRaw = entry.weaponId();
			boolean showGunIcon = false;
			String gunNamespace = "";
			String gunPath = "";
			if (gunIdRaw.contains(":")) {
				String[] parts = gunIdRaw.split(":", 2);
				gunNamespace = parts[0];
				gunPath = parts[1];
				showGunIcon = (gunNamespace.equals("tacz") || gunNamespace.equals("lradd"));
			}

			boolean showHead = true;
			int headWidth = headSize + padding;
			// Prefer TACZ slot texture if available
			ResourceLocation slotTexture = resolveTaczSlotTexture(gunIdRaw);
			boolean showSlot = slotTexture != null;
			int gunWidth = (showGunIcon || showSlot) ? iconSize + padding : 0;

			boolean showMobIcon = isMobKill;
			int mobWidth = showMobIcon ? iconSize + padding : 0;

			int totalWidth = textWidth + gunWidth + mobWidth + (showHead ? headWidth : 0);
			int x = screenWidth - totalWidth - 2 - padding * 2;
			int bgColor = new Color(0, 0, 0, (int) (alpha * 150)).getRGB();

			event.getGuiGraphics().fill(x, y, x + totalWidth + padding * 2, y + ySize, bgColor);

			int currentX = x + padding;

			// Draw head icon
			PlayerInfo headInfo;
			if (isPvpKill) {
				headInfo = mc.getConnection().getPlayerInfo(entry.killerUUID());
			} else {
				headInfo = mc.getConnection().getPlayerInfo(entry.victimUUID());
			}
			if (headInfo != null) {
				RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, alpha);
				RenderSystem.enableBlend();
				event.getGuiGraphics().blit(headInfo.getSkinLocation(), currentX, y + (ySize - headSize) / 2, headSize, headSize, 8, 8, 8, 8, 64, 64);
				event.getGuiGraphics().blit(headInfo.getSkinLocation(), currentX, y + (ySize - headSize) / 2, headSize, headSize, 40, 8, 8, 8, 64, 64);
				RenderSystem.disableBlend();
				RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
			}
			currentX += (showHead ? headWidth : 0);

			// Draw gun icon or TACZ slot if applicable with caching
			if (showSlot) {
				RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, alpha);
				RenderSystem.enableBlend();
				event.getGuiGraphics().blit(slotTexture, currentX, y + (ySize - iconSize) / 2, 0, 0, iconSize, iconSize, iconSize, iconSize);
				RenderSystem.disableBlend();
				RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
				currentX += gunWidth;
			} else if (showGunIcon) {
				String fullId = gunNamespace + ":" + gunPath;
				ResourceLocation cached = GUN_ICON_CACHE.get(fullId);
				if (cached == null && !GUN_ICON_CACHE.containsKey(fullId)) {
					// Resolve once and cache result (including negative cache as null)
					ResourceManager rm = mc.getResourceManager();
					ResourceLocation tryRl;
					// Try multiple common paths
					String[] candidatePaths = new String[] {
						"textures/item/guns/" + gunPath + ".png",
						"textures/item/" + gunPath + ".png",
						"textures/gun/hud/" + gunPath + ".png",
						"textures/gun/" + gunPath + ".png"
					};
					ResourceLocation found = null;
					for (String p : candidatePaths) {
						tryRl = ResourceLocation.fromNamespaceAndPath(gunNamespace, p);
						if (rm.getResource(tryRl).isPresent()) { found = tryRl; break; }
					}
					GUN_ICON_CACHE.put(fullId, found); // can be null meaning not found
					cached = found;
				}

				if (cached != null) {
					RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, alpha);
					RenderSystem.enableBlend();
					event.getGuiGraphics().blit(cached, currentX, y + (ySize - iconSize) / 2, 0, 0, iconSize, iconSize, iconSize, iconSize);
					RenderSystem.disableBlend();
					RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
					currentX += gunWidth;
				}
			}

			// Draw mob icon (generic) if applicable
			if (showMobIcon) {
				// Placeholder: draw a sword icon or mob head as needed; omitted for brevity
				currentX += mobWidth;
			}

			font.drawInBatch(killMessage, currentX, (float) (y + (ySize - 10) / 2), (alphaInt << 24) | 0xFFFFFF, false, event.getGuiGraphics().pose().last().pose(), event.getGuiGraphics().bufferSource(), Font.DisplayMode.NORMAL, 0, 15728880);
			event.getGuiGraphics().flush();

			y += ySize + 2;
		}
	}

	private static void renderKillcard(RenderGuiOverlayEvent.Post event) {
		KillFeedManager.KillCard card = KillFeedManager.getActiveKillcard();
		if (card == null) return;

		long age = System.currentTimeMillis() - card.creationTime();
		float alpha;
		long fade = 300L;
		if (age < fade) alpha = (float) age / fade; 
		else if (age > KillFeedManager.KILLCARD_DURATION_MS - fade) 
			alpha = 1.0f - (float) (age - (KillFeedManager.KILLCARD_DURATION_MS - fade)) / fade; 
		else alpha = 1.0f;
		alpha = Math.max(0f, Math.min(1f, alpha));

		Minecraft mc = Minecraft.getInstance();
		int sw = event.getWindow().getGuiScaledWidth();
		int sh = event.getWindow().getGuiScaledHeight();

		ResourceLocation texture = card.texture();

		// Check if texture exists and is valid before rendering
		try {
			var resourceManager = mc.getResourceManager();
			var resource = resourceManager.getResource(texture);
			if (resource.isEmpty()) {
				// Texture doesn't exist, skip rendering
				return;
			}
		} catch (Exception e) {
			// Texture is corrupted or invalid, skip rendering
			return;
		}

		// Smaller sizing with scale animation
		int baseWidth = Math.min(140, (int)(sw * 0.12));
		int baseHeight = baseWidth;
		
		// Scale animation - starts small and grows
		float scaleProgress = Math.min(1.0f, (float) age / 200f); // Scale up over 200ms
		float scale = 0.3f + (0.7f * scaleProgress); // Scale from 0.3 to 1.0
		
		int width = (int)(baseWidth * scale);
		int height = (int)(baseHeight * scale);
		int x = (sw - width) / 2;
		int marginBottom = 28; // just above hotbar
		int y = sh - height - marginBottom;

		try {
			RenderSystem.setShaderColor(1f, 1f, 1f, alpha);
			RenderSystem.enableBlend();
			// Apply simple animation: rotate slightly for streak >= 2
			var pose = event.getGuiGraphics().pose();
			pose.pushPose();
			// Translate to center for rotation
			pose.translate(x + width / 2.0f, y + height / 2.0f, 0);
			float extraScale = 1.0f;
			float rotationDeg = 0f;
			KillFeedManager.KillCard cardData = card;
			int streak = cardData.streakIndex();
			if (streak >= 2) {
				rotationDeg = (streak - 1) * 5f * (float)Math.sin(age / 150.0);
				extraScale = 1.0f + 0.02f * (streak - 1) * (float)Math.sin(age / 120.0);
			}
			pose.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(rotationDeg));
			pose.scale(extraScale, extraScale, 1.0f);
			// Draw centered
			event.getGuiGraphics().blit(texture, -width / 2, -height / 2, 0, 0, width, height, width, height);
			pose.popPose();
			
			RenderSystem.disableBlend();
			RenderSystem.setShaderColor(1,1,1,1);

			// Simple text without effects
			Font font = mc.font;
			String killerName = card.killer();
			String middle = " eliminated ";
			String victimName = card.victim();
			int killerW = font.width(killerName);
			int middleW = font.width(middle);
			int victimW = font.width(victimName);
			int totalW = killerW + middleW + victimW;
			int tx = x + (width - totalW) / 2;
			int ty = y + height - font.lineHeight - 8;
			int currentX = tx;
			// Killer (green)
			event.getGuiGraphics().drawString(font, killerName, currentX, ty, 0xFF00FF00, true);
			currentX += killerW;
			// 'eliminated' (yellow)
			event.getGuiGraphics().drawString(font, middle, currentX, ty, 0xFFFFFF00, true);
			currentX += middleW;
			// Victim (red)
			event.getGuiGraphics().drawString(font, victimName, currentX, ty, 0xFFFF0000, true);
			
		} catch (Exception e) {
			// If texture rendering fails, just skip it
			RenderSystem.disableBlend();
			RenderSystem.setShaderColor(1,1,1,1);
		}
	}
	
	private static void renderSparkles(RenderGuiOverlayEvent.Post event, int x, int y, int width, int height, long age, float alpha) {
		// Create sparkle effects around the killcard
		RenderSystem.setShaderColor(1f, 1f, 1f, alpha * 0.8f);
		
		int sparkleCount = 8;
		for (int i = 0; i < sparkleCount; i++) {
			float angle = (age / 100.0f + i * 45.0f) % 360.0f;
			float radius = 20 + (float) Math.sin(age / 200.0 + i) * 10;
			
			int sparkleX = x + width/2 + (int)(Math.cos(Math.toRadians(angle)) * radius);
			int sparkleY = y + height/2 + (int)(Math.sin(Math.toRadians(angle)) * radius);
			
			// Draw sparkle as a small cross
			event.getGuiGraphics().fill(sparkleX - 1, sparkleY, sparkleX + 1, sparkleY + 1, 0xFFFFFFFF);
			event.getGuiGraphics().fill(sparkleX, sparkleY - 1, sparkleX + 1, sparkleY + 1, 0xFFFFFFFF);
		}
	}
} 