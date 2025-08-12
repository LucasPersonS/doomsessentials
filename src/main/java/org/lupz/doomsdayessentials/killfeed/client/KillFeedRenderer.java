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

import java.awt.*;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Mod.EventBusSubscriber(modid = EssentialsMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class KillFeedRenderer {
    private static final long FADE_DURATION_MS = 500L;

    // Cache for resolved gun icon textures: key is namespace:path weapon id, value is RL or null if not found
    private static final Map<String, ResourceLocation> GUN_ICON_CACHE = new ConcurrentHashMap<>();

    @SubscribeEvent
    public static void onRenderGuiOverlay(RenderGuiOverlayEvent.Post event) {
        // Render once per frame anchored to the final overlay to avoid duplicate draws
        if (!event.getOverlay().id().equals(VanillaGuiOverlay.PLAYER_HEALTH.id())) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui) return;

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

            String gunIdRaw = entry.weaponName();
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
            int gunWidth = showGunIcon ? iconSize + padding : 0;

            boolean showMobIcon = isMobKill;
            int mobWidth = showMobIcon ? iconSize + padding : 0;

            int totalWidth = textWidth + gunWidth + mobWidth + (showHead ? headWidth : 0);
            int x = screenWidth - totalWidth - 2 - padding * 2;
            int bgColor = new Color(0, 0, 0, (int) (alpha * 150)).getRGB();

            guiGraphics.fill(x, y, x + totalWidth + padding * 2, y + ySize, bgColor);

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
                guiGraphics.blit(headInfo.getSkinLocation(), currentX, y + (ySize - headSize) / 2, headSize, headSize, 8, 8, 8, 8, 64, 64);
                guiGraphics.blit(headInfo.getSkinLocation(), currentX, y + (ySize - headSize) / 2, headSize, headSize, 40, 8, 8, 8, 64, 64);
                RenderSystem.disableBlend();
                RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
            }
            currentX += (showHead ? headWidth : 0);

            // Draw gun icon if applicable with caching
            if (showGunIcon) {
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
                    guiGraphics.blit(cached, currentX, y + (ySize - iconSize) / 2, 0, 0, iconSize, iconSize, iconSize, iconSize);
                    RenderSystem.disableBlend();
                    RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
                } else {
                    // fallback render the gun item itself
                    Item gunItem = ForgeRegistries.ITEMS.getValue(ResourceLocation.parse(gunNamespace+":"+gunPath));
                    ItemStack stack = gunItem != null ? new ItemStack(gunItem) : new ItemStack(Items.IRON_SWORD);
                    guiGraphics.renderItem(stack, currentX, y + (ySize - iconSize) / 2);
                }

                currentX += gunWidth;
            } else if (showMobIcon) {
                ItemStack mobIconStack = ItemStack.EMPTY;
                try {
                    ResourceLocation entRl = ResourceLocation.parse(entry.entityTypeId());
                    ResourceLocation eggRl = ResourceLocation.fromNamespaceAndPath(entRl.getNamespace(), entRl.getPath() + "_spawn_egg");
                    Item eggItem = ForgeRegistries.ITEMS.getValue(eggRl);
                    if (eggItem != null && eggItem != net.minecraft.world.item.Items.AIR) {
                        mobIconStack = new ItemStack(eggItem);
                    }
                } catch (Exception ignored) {}

                if (!mobIconStack.isEmpty()) {
                    guiGraphics.renderItem(mobIconStack, currentX, y + (ySize - iconSize) / 2);
                }
                currentX += mobWidth;
            }

            int textY = y + (ySize - font.lineHeight) / 2 + 1;
            guiGraphics.drawString(font, killMessage, currentX, textY, Color.WHITE.getRGB() | (alphaInt << 24), false);

            y += ySize + 2;
        }
    }
} 