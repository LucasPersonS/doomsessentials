package org.lupz.doomsdayessentials.rarity.client;

import com.mojang.datafixers.util.Either;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lupz.doomsdayessentials.EssentialsMod;
import org.lupz.doomsdayessentials.rarity.RarityManager;
import net.minecraft.client.Minecraft;
import com.mojang.blaze3d.platform.NativeImage;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Injects a small banner-like tooltip component for item rarity and registers its client renderer.
 */
@Mod.EventBusSubscriber(modid = EssentialsMod.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class RarityTooltipComponents {
    private RarityTooltipComponents() {}

    // Simple data component placed into the tooltip element list
    public record RarityComponent(RarityManager.RarityTier tier) implements TooltipComponent {}

    // Client-side renderer for the rarity component
    public static class RarityClientComponent implements ClientTooltipComponent {
        private static final Map<ResourceLocation, int[]> DIM_CACHE = new ConcurrentHashMap<>();
        private final ResourceLocation texture;
        private final int width;
        private final int height;
        public RarityClientComponent(RarityComponent data) {
            this.texture = RarityManager.getTexture(data.tier());
            int[] wh = getDims(texture);
            this.width = wh[0];
            this.height = wh[1];
        }
        private static int[] getDims(ResourceLocation tex) {
            if (tex == null) return new int[]{0, 0};
            int[] cached = DIM_CACHE.get(tex);
            if (cached != null) return cached;
            int[] wh = new int[]{0, 0};
            try {
                var rm = Minecraft.getInstance().getResourceManager();
                var opt = rm.getResource(tex);
                if (opt.isPresent()) {
                    try (var is = opt.get().open()) {
                        NativeImage img = NativeImage.read(is);
                        wh[0] = img.getWidth();
                        wh[1] = img.getHeight();
                        img.close();
                    }
                }
            } catch (Exception ignored) {}
            if (wh[0] <= 0 || wh[1] <= 0) {
                wh[0] = 64; wh[1] = 6; // fallback
            }
            DIM_CACHE.put(tex, wh);
            return wh;
        }
        @Override
        public int getHeight() { return height; }
        @Override
        public int getWidth(Font font) { return width; }
        @Override
        public void renderImage(Font font, int x, int y, GuiGraphics gg) {
            if (texture == null) return;
            // Draw 1:1 at native size to avoid stretching
            gg.blit(texture, x, y, 0, 0, width, height, width, height);
        }
    }

    // Inject our rarity component at the top of the tooltip elements
    @SubscribeEvent
    public static void onGatherComponents(RenderTooltipEvent.GatherComponents e) {
        var stack = e.getItemStack();
        if (stack == null || stack.isEmpty()) return;
        var tier = RarityManager.resolveRarity(stack);
        if (tier == null) return;
        e.getTooltipElements().add(0, Either.right(new RarityComponent(tier)));
    }
}
