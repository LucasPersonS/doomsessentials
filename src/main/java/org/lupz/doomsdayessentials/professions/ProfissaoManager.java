package org.lupz.doomsdayessentials.professions;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.lupz.doomsdayessentials.EssentialsMod;
import org.lupz.doomsdayessentials.config.EssentialsConfig;
import org.lupz.doomsdayessentials.professions.shop.EngineerConfig;

import javax.annotation.Nullable;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Simple manager that keeps the profession each player chose.
 * <p>
 * Data is persisted to <code>config/doomsdayessentials/professions.json</code> so it survives restarts.
 * <p>
 * Currently we only have the profession id "medico", but the manager is generic enough to store any
 * string id in the future.
 */
public final class ProfissaoManager {

    private ProfissaoManager() {}

    // ---------------------------------------------------------------------
    // Persistent state
    // ---------------------------------------------------------------------
    private static final Map<UUID, String> playerProfessions = new ConcurrentHashMap<>();
    private static final File PROFESSIONS_FILE = new File("config/doomsdayessentials/professions.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    // ---------------------------------------------------------------------
    // CRUD helpers
    // ---------------------------------------------------------------------

    public static void setProfession(UUID playerUUID, String profession) {
        if (!canBecome(profession)) {
            net.minecraft.server.level.ServerPlayer player = ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayer(playerUUID);
            if (player != null) {
                player.sendSystemMessage(net.minecraft.network.chat.Component.literal("§cO limite para a profissão " + profession + " foi atingido."));
            }
            return;
        }
        playerProfessions.put(playerUUID, profession);
        saveProfessions();
    }

    public static String getProfession(UUID playerUUID) {
        return playerProfessions.get(playerUUID);
    }

    public static void removeProfession(UUID playerUUID) {
        playerProfessions.remove(playerUUID);
        saveProfessions();
    }

    public static boolean hasProfession(UUID playerUUID) {
        return playerProfessions.containsKey(playerUUID);
    }

    // ---------------------------------------------------------------------
    // Limit helpers
    // ---------------------------------------------------------------------

    /**
     * Generic limit checker for any registered profession.
     * Returns true if there is room for another player of this profession
     * or if the limit is set to 0 (unlimited).
     */
    public static boolean canBecome(String professionId) {
        String id = professionId == null ? "" : professionId.toLowerCase();
        int maxAllowed;
        switch (id) {
            case "medico" -> maxAllowed = EssentialsConfig.MEDICO_MAX_COUNT.get();
            case "combatente" -> maxAllowed = EssentialsConfig.COMBATENTE_MAX_COUNT.get();
            case "rastreador" -> maxAllowed = EssentialsConfig.RASTREADOR_MAX_COUNT.get();
            case "engenheiro" -> maxAllowed = EngineerConfig.ENGENHEIRO_MAX_COUNT.get();
            default -> {
                // No limit defined for unknown professions
                return true;
            }
        }

        if (maxAllowed == 0) return true; // Unlimited

        long currentCount = playerProfessions.values().stream()
                .filter(p -> id.equalsIgnoreCase(p))
                .count();
        EssentialsMod.LOGGER.info("Current {} count: {}, Max allowed: {}", id, currentCount, maxAllowed);
        return currentCount < maxAllowed;
    }

    public static boolean canBecomeMedico() {
        return canBecome("medico");
    }

    public static boolean canBecomeCombatente() {
        return canBecome("combatente");
    }

    public static boolean canBecomeRastreador() {
        return canBecome("rastreador");
    }

    // ---------------------------------------------------------------------
    // Persistence helpers
    // ---------------------------------------------------------------------

    public static void loadProfessions() {
        if (!PROFESSIONS_FILE.exists()) {
            return;
        }
        try (FileReader reader = new FileReader(PROFESSIONS_FILE)) {
            Type type = new com.google.gson.reflect.TypeToken<ConcurrentHashMap<UUID, String>>(){}.getType();
            Map<UUID, String> loaded = GSON.fromJson(reader, type);
            if (loaded != null) {
                playerProfessions.clear();
                playerProfessions.putAll(loaded);
                EssentialsMod.LOGGER.info("Successfully loaded {} professions.", playerProfessions.size());
            }
        } catch (IOException ex) {
            EssentialsMod.LOGGER.error("Could not load professions from file", ex);
        }
    }

    public static void saveProfessions() {
        try {
            // ensure directory exists
            if (PROFESSIONS_FILE.getParentFile() != null) {
                //noinspection ResultOfMethodCallIgnored
                PROFESSIONS_FILE.getParentFile().mkdirs();
            }
            try (FileWriter writer = new FileWriter(PROFESSIONS_FILE)) {
                GSON.toJson(playerProfessions, writer);
            }
        } catch (IOException ex) {
            EssentialsMod.LOGGER.error("Could not save professions to file", ex);
        }
    }

    /**
     * Expose immutable view for debugging/commands.
     */
    public static Map<UUID, String> getPlayerProfessions() {
        return java.util.Collections.unmodifiableMap(playerProfessions);
    }
} 