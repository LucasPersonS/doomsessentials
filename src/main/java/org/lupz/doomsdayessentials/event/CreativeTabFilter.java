package org.lupz.doomsdayessentials.event;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lupz.doomsdayessentials.EssentialsMod;

import java.util.ArrayList;
import java.util.List;

@Mod.EventBusSubscriber(modid = EssentialsMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class CreativeTabFilter {
    private CreativeTabFilter() {}

    @SubscribeEvent
    public static void onBuildCreativeContents(BuildCreativeModeTabContentsEvent event) {
        List<ItemStack> remove = new ArrayList<>();
        for (var entry : event.getEntries()) {
            ItemStack stack = entry.getKey();
            CompoundTag tag = stack.getTag();
            if (tag == null) continue;
            if (!tag.contains("GunId", net.minecraft.nbt.Tag.TAG_STRING)) continue;
            String gunId = tag.getString("GunId");
            if (gunId == null || gunId.isEmpty()) continue;
            String idLower = gunId.toLowerCase(java.util.Locale.ROOT);
            if (idLower.startsWith("doomsday:")) {
                boolean hide = false;
                if (idLower.contains("_kuronami") || idLower.equals("doomsday:kuronami") || idLower.startsWith("doomsday:kuronami_")) hide = true;
                if (idLower.contains("_ak_texas") || idLower.contains("_ak_kaltsit") || idLower.contains("_ak_laffey")) hide = true;
                if (idLower.contains("_m4_koei") || idLower.contains("_mk18_jianjiu") || idLower.contains("_sig556_shiroko") || idLower.contains("_type20_hibiki") || idLower.contains("_galilace_lesh") || idLower.contains("_awp_hm")) hide = true;
                if (hide) remove.add(stack);
            }
            
        }
        if (!remove.isEmpty()) {
            for (ItemStack s : remove) event.getEntries().remove(s);
        }
    }
}
