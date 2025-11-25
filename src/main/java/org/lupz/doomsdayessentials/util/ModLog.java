package org.lupz.doomsdayessentials.util;

public final class ModLog {
    private ModLog() {}

    public static void debug(String msg) {
        if (!org.lupz.doomsdayessentials.config.EssentialsConfig.DEBUG_LOGS_ENABLED.get()) return;
        org.lupz.doomsdayessentials.EssentialsMod.LOGGER.debug(msg);
    }

    public static void debug(String msg, Object a1) {
        if (!org.lupz.doomsdayessentials.config.EssentialsConfig.DEBUG_LOGS_ENABLED.get()) return;
        org.lupz.doomsdayessentials.EssentialsMod.LOGGER.debug(msg, a1);
    }

    public static void debug(String msg, Object a1, Object a2) {
        if (!org.lupz.doomsdayessentials.config.EssentialsConfig.DEBUG_LOGS_ENABLED.get()) return;
        org.lupz.doomsdayessentials.EssentialsMod.LOGGER.debug(msg, a1, a2);
    }

    public static void debug(String msg, Object a1, Object a2, Object a3) {
        if (!org.lupz.doomsdayessentials.config.EssentialsConfig.DEBUG_LOGS_ENABLED.get()) return;
        org.lupz.doomsdayessentials.EssentialsMod.LOGGER.debug(msg, a1, a2, a3);
    }

    public static void debug(String msg, Object... args) {
        if (!org.lupz.doomsdayessentials.config.EssentialsConfig.DEBUG_LOGS_ENABLED.get()) return;
        org.lupz.doomsdayessentials.EssentialsMod.LOGGER.debug(msg, args);
    }
}
