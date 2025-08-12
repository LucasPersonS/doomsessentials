package org.lupz.doomsdayessentials.client.frequency;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import org.lupz.doomsdayessentials.EssentialsMod;
import org.lupz.doomsdayessentials.frequency.capability.FrequencyCapabilityProvider;
import org.lupz.doomsdayessentials.sound.ModSounds;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public final class FrequencyClientEffects {
    private FrequencyClientEffects() {}

    // Dynamic symbol list from resources
    private static final List<ResourceLocation> SYMBOLS = new ArrayList<>();
    private static boolean SYMBOLS_LOADED = false;

    private static void ensureSymbolsLoaded(Minecraft mc) {
        if (SYMBOLS_LOADED) return;
        SYMBOLS_LOADED = true;
        try {
            ResourceManager rm = mc.getResourceManager();
            // list all PNGs under textures/misc for our namespace
            Map<ResourceLocation, net.minecraft.server.packs.resources.Resource> found = rm.listResources("textures/misc", rl -> rl.getNamespace().equals(EssentialsMod.MOD_ID) && rl.getPath().endsWith(".png"));
            Set<String> excludeNames = Set.of(
                    "area_contested", "area_dominating", "area_dominated",
                    "frequencia_overlay"
            );
            for (ResourceLocation rl : found.keySet()) {
                String path = rl.getPath(); // e.g., textures/misc/xyz.png
                String base = path.substring(path.lastIndexOf('/') + 1).toLowerCase(Locale.ROOT); // xyz.png
                String name = base.endsWith(".png") ? base.substring(0, base.length() - 4) : base;
                if (excludeNames.contains(name)) continue;
                SYMBOLS.add(rl);
            }
        } catch (Exception ignored) {}
    }

    private static final List<Symbol> ACTIVE = new ArrayList<>();
    private static float spawnAccumulator = 0f;

    // Ambient sound cooldowns
    private static long nextAmbientMs = 0L;

    // Shader management: 0=none, 1=deconverge, 2=spider
    private static int shaderState = 0;

    private static class Symbol {
        final ResourceLocation texture;
        final float x;
        final float y;
        final float scale;
        final float rotationDeg;
        final long spawnMs;
        final long lifeMs;

        Symbol(ResourceLocation texture, float x, float y, float scale, float rotationDeg, long lifeMs) {
            this.texture = texture;
            this.x = x;
            this.y = y;
            this.scale = scale;
            this.rotationDeg = rotationDeg;
            this.spawnMs = System.currentTimeMillis();
            this.lifeMs = lifeMs;
        }

        float ageMs() { return (float)(System.currentTimeMillis() - spawnMs); }
        float lifeFrac() { return Math.min(1f, ageMs() / (float) lifeMs); }
        boolean expired() { return ageMs() > lifeMs; }
        float alpha() {
            float f = lifeFrac();
            float a = (float)(Math.sin(f * Math.PI));
            a *= 0.7f + 0.3f * ThreadLocalRandom.current().nextFloat();
            return Math.max(0f, Math.min(1f, a));
        }
    }

    public static void tick(Minecraft mc) {
        if (mc.player == null) return;
        ensureSymbolsLoaded(mc);
        mc.player.getCapability(FrequencyCapabilityProvider.FREQUENCY_CAPABILITY).ifPresent(cap -> {
            int level = cap.getLevel();

            // Apply post shaders by thresholds (one at a time): spider (>=80%) > deconverge (>=10%) > none
            applyShaderForLevel(mc, level);

            // Camera shake from 50%+, scaling up strongly by level
            if (level >= 50) {
                double norm = (level - 50) / 50.0; // 0..1
                double intensity = 1.5 + norm * 3.5; // 1.5 .. 5.0
                double yawShake = (mc.player.getRandom().nextDouble() - 0.5) * intensity;
                double pitchShake = (mc.player.getRandom().nextDouble() - 0.5) * intensity;
                mc.player.setYRot(mc.player.getYRot() + (float) yawShake);
                mc.player.setXRot(mc.player.getXRot() + (float) pitchShake);
            }

            // Spawn symbols – scales with level (honor imagesEnabled)
            boolean imagesEnabled = cap.isImagesEnabled();
            if (imagesEnabled && !SYMBOLS.isEmpty()) {
                float spawnsPerSecond = (level / 100f) * 8.0f;
                float spawnsPerTick = spawnsPerSecond / 20.0f;
                spawnAccumulator += spawnsPerTick;
                int maxActive = 24;
                while (spawnAccumulator >= 1f && ACTIVE.size() < maxActive) {
                    spawnAccumulator -= 1f;
                    spawnRandomSymbol(mc);
                    if (ThreadLocalRandom.current().nextFloat() < 0.2f && ACTIVE.size() < maxActive) {
                        spawnRandomSymbol(mc);
                    }
                }
            } else if (!imagesEnabled) {
                ACTIVE.clear();
                spawnAccumulator = 0f;
            }

            // Ambient sounds from 10%+ (honor soundsEnabled)
            if (cap.isSoundsEnabled() && level >= 10) {
                long now = System.currentTimeMillis();
                if (now >= nextAmbientMs) {
                    // Expanded pool; choose weighted by level
                    float r = ThreadLocalRandom.current().nextFloat();
                    if (level >= 80 && r < 0.4f) {
                        play(ModSounds.INSIDE_FREQUENCY.get(), level);
                    } else if (r < 0.65f) {
                        play(ModSounds.WHISPERING.get(), level);
                    } else if (r < 0.8f) {
                        play(ModSounds.ESPALHE.get(), level);
                    } else if (r < 0.9f) {
                        play(ModSounds.FREQUENCIA1.get(), level);
                    } else {
                        play(ModSounds.FREQUENCIA2.get(), level);
                    }
                    long base;
                    if (level >= 50) {
                        base = 4000L - (long)((level - 50) / 50.0 * 2000L); // 4s down to ~2s
                    } else {
                        base = 9000L - (long)((level / 100.0) * 4000L); // 9s down to ~5s
                    }
                    long jitter = ThreadLocalRandom.current().nextLong(500L, 1500L);
                    nextAmbientMs = now + Math.max(1000L, base - jitter);
                }
            } else if (!cap.isSoundsEnabled()) {
                nextAmbientMs = 0L;
            }

            // Cull expired symbols
            Iterator<Symbol> it = ACTIVE.iterator();
            while (it.hasNext()) {
                if (it.next().expired()) it.remove();
            }
        });
    }

    private static void applyShaderForLevel(Minecraft mc, int level) {
        try {
            if (level >= 80) {
                if (shaderState != 2) {
                    mc.gameRenderer.loadEffect(ResourceLocation.parse("minecraft:shaders/post/spider.json"));
                    shaderState = 2;
                }
            } else if (level >= 10) {
                if (shaderState != 1) {
                    mc.gameRenderer.loadEffect(ResourceLocation.parse("minecraft:shaders/post/deconverge.json"));
                    shaderState = 1;
                }
            } else {
                if (shaderState != 0) {
                    mc.gameRenderer.shutdownEffect();
                    shaderState = 0;
                }
            }
        } catch (Exception ignored) {
        }
    }

    private static void play(net.minecraft.sounds.SoundEvent se, int level) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        float volume = Math.max(0.2f, Math.min(1.0f, level / 50.0f));
        float pitch;
        if (level <= 60) {
            pitch = 1.0f;
        } else {
            float t = (level - 60) / 40.0f; // 0..1
            pitch = 1.0f - 0.3f * t; // 1.0 -> 0.7
        }
        mc.getSoundManager().play(SimpleSoundInstance.forLocalAmbience(se, volume, pitch));
    }

    private static void spawnRandomSymbol(Minecraft mc) {
        if (SYMBOLS.isEmpty()) return;
        int w = mc.getWindow().getGuiScaledWidth();
        int h = mc.getWindow().getGuiScaledHeight();
        var rnd = ThreadLocalRandom.current();
        ResourceLocation tex = SYMBOLS.get(rnd.nextInt(SYMBOLS.size()));
        float x = rnd.nextFloat() * w;
        float y = rnd.nextFloat() * h;
        float scale = 0.6f + rnd.nextFloat() * 1.4f; // 0.6 .. 2.0
        float rot = -25f + rnd.nextFloat() * 50f; // -25..25 deg
        long life = 350 + rnd.nextInt(900); // 0.35s .. 1.25s
        ACTIVE.add(new Symbol(tex, x, y, scale, rot, life));
    }

    public static void render(GuiGraphics gfx, int width, int height) {
        var pose = gfx.pose();
        for (Symbol s : ACTIVE) {
            float alpha = s.alpha();
            RenderSystem.enableBlend();
            RenderSystem.setShaderColor(1f, 1f, 1f, alpha);
            pose.pushPose();
            pose.translate(s.x, s.y, 0);
            pose.mulPose(Axis.ZP.rotationDegrees(s.rotationDeg));
            pose.scale(s.scale, s.scale, 1f);
            gfx.blit(s.texture, -32, -32, 0, 0, 64, 64, 64, 64);
            pose.popPose();
            RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
            RenderSystem.disableBlend();
        }
    }
} 