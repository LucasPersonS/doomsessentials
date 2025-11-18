package org.lupz.doomsdayessentials.guild;

import net.minecraft.world.item.ItemStack;
import org.lupz.doomsdayessentials.EssentialsMod;

public final class StorageDiagnostics {
    private StorageDiagnostics() {}
    private static boolean debugEnabled() {
        try {
            return org.lupz.doomsdayessentials.guild.GuildConfig.SPEC != null && org.lupz.doomsdayessentials.guild.GuildConfig.STORAGE_DEBUG_ENABLED.get();
        } catch (Throwable ignored) { return false; }
    }
    public static void logOp(String op, String guild, int page, int slot, ItemStack stack, int amount) {
        try {
            String id = stack == null || stack.isEmpty() ? "" : java.util.Objects.toString(net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(stack.getItem()));
            EssentialsMod.LOGGER.info("StorageOp: op=" + op + ", guild=" + guild + ", page=" + page + ", slot=" + slot + ", item=" + id + ", amount=" + amount);
        } catch (Throwable ignored) {}
    }
    public static void logTrace(String stage, String guild, int page, int slot, ItemStack stack) {
        if (!debugEnabled()) return;
        try {
            String id = stack == null || stack.isEmpty() ? "" : java.util.Objects.toString(net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(stack.getItem()));
            int count = stack == null || stack.isEmpty() ? 0 : stack.getCount();
            int ext = 0;
            if (stack != null && stack.hasTag() && stack.getTag().contains("gd_ext_count")) ext = stack.getTag().getInt("gd_ext_count");
            EssentialsMod.LOGGER.info("StorageTrace: stage=" + stage + ", guild=" + guild + ", page=" + page + ", slot=" + slot + ", item=" + id + ", count=" + count + ", ext=" + ext);
        } catch (Throwable ignored) {}
    }
    public static void logError(String ctx, Throwable t) {
        EssentialsMod.LOGGER.error("StorageOp error: " + ctx, t);
    }
}
