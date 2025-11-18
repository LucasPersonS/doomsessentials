package org.lupz.doomsdayessentials.guild;

import net.minecraft.server.MinecraftServer;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.lupz.doomsdayessentials.EssentialsMod;
import org.lupz.doomsdayessentials.guild.menu.GuildStorageMenu;

public final class StorageEnvValidator {
    private static long lastRunMs = 0L;
    private StorageEnvValidator() {}

    public static void validateServer() {
        long now = System.currentTimeMillis();
        if (lastRunMs != 0 && (now - lastRunMs) < 30_000) return;
        lastRunMs = now;
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            EssentialsMod.LOGGER.warn("StorageEnv: server handle not available");
            return;
        }
        boolean dedicated = server.isDedicatedServer();
        String motd = server.getMotd();
        int viewMax = GuildStorageMenu.Stacking.getContainerMaxStackSize();
        EssentialsMod.LOGGER.info("StorageEnv: dedicated=" + dedicated + ", production=" + FMLEnvironment.production + ", dist=" + net.minecraftforge.fml.loading.FMLEnvironment.dist.name() + ", motd=" + motd + ", viewMax=" + viewMax + ", debug=" + org.lupz.doomsdayessentials.guild.GuildConfig.STORAGE_DEBUG_ENABLED.get());
        try {
            if (viewMax < 32767) EssentialsMod.LOGGER.warn("StorageEnv: view max stack size unexpected=" + viewMax);
            GuildsManager gm = GuildsManager.get(server.overworld());
            for (String name : gm.getGuildNames()) {
                net.minecraft.core.NonNullList<net.minecraft.world.item.ItemStack> inv = gm.getOrCreateStorage(name);
                EssentialsMod.LOGGER.info("StorageEnv: guild=" + name + ", storageSize=" + inv.size());
                int sample = Math.min(10, inv.size());
                for (int i = 0; i < sample; i++) {
                    net.minecraft.world.item.ItemStack s = inv.get(i);
                    org.lupz.doomsdayessentials.guild.StorageDiagnostics.logTrace("env_sample", name, 0, i % 54, s);
                }
            }
        } catch (Throwable t) {
            EssentialsMod.LOGGER.error("StorageEnv: validation failed", t);
        }
    }
}
