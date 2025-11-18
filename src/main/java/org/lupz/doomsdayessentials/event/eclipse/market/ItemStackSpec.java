package org.lupz.doomsdayessentials.event.eclipse.market;

import com.google.gson.JsonObject;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.resources.ResourceLocation;

/**
 * Lightweight specification for an item stack in JSON config.
 * Supports namespaced item id, count and optional NBT payload.
 */
public class ItemStackSpec {
    public final ResourceLocation id;
    public final int count;
    public final CompoundTag nbt; // optional

    public ItemStackSpec(ResourceLocation id, int count, CompoundTag nbt){
        this.id = id;
        this.count = Math.max(1, count);
        this.nbt = nbt;
    }

    public static ItemStackSpec of(ResourceLocation id, int count){
        return new ItemStackSpec(id, count, null);
    }
}

