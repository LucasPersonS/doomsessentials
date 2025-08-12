package org.lupz.doomsdayessentials.client.model;

import net.minecraft.resources.ResourceLocation;
import org.lupz.doomsdayessentials.EssentialsMod;

public final class NightMarketModelResources {
    private static ResourceLocation rl(String path){return ResourceLocation.fromNamespaceAndPath(EssentialsMod.MOD_ID, path);}    
    public static final ResourceLocation MODEL = rl("geo/black_market.geo.json");
    public static final ResourceLocation TEXTURE = rl("textures/entity/black_market.png");
    public static final ResourceLocation ANIM = rl("animations/black_market.animation.json");
} 