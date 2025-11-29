package org.lupz.doomsdayessentials.lootbox.farming;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.loading.FMLPaths;
import org.lupz.doomsdayessentials.EssentialsMod;
import org.lupz.doomsdayessentials.item.ModItems;
import org.lupz.doomsdayessentials.lootbox.LootboxManager;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gerencia o sistema de farming de lootboxes.
 * Jogadores ganham fragmentos através de atividades (mobs, mining, quests)
 * e podem craftar lootboxes completas.
 */
public final class LootboxFarmingManager {
    private LootboxFarmingManager() {
    }

    private static final Path FILE_PATH = FMLPaths.CONFIGDIR.get().resolve("doomsdayessentials/lootbox_farming.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    // Limites semanais por raridade (configuráveis)
    // Incomum: 100 = 1 lootbox, Rara: 150 = 1 lootbox
    // Épica: 200 = 1 lootbox, Lendária: 300 = 1 lootbox
    public static final Map<String, Integer> DEFAULT_WEEKLY_LIMITS = Map.of(
            LootboxManager.R_INCOMUM, 100,
            LootboxManager.R_RARA, 150,
            LootboxManager.R_EPICA, 200,
            LootboxManager.R_LENDARIA, 300);

    // Dados de farming por jogador
    private static final Map<UUID, PlayerFarmingData> playerData = new ConcurrentHashMap<>();

    /**
     * Dados de farming de um jogador
     */
    public static class PlayerFarmingData {
        public UUID playerId;
        public Map<String, Integer> fragmentsThisWeek; // rarity -> count
        public long lastResetTime; // Timestamp do último reset semanal
        public String currentDailyQuest; // ID da quest diária atual
        public int questProgress; // Progresso da quest atual

        public PlayerFarmingData(UUID playerId) {
            this.playerId = playerId;
            this.fragmentsThisWeek = new HashMap<>();
            for (String rarity : LootboxManager.RARITIES) {
                this.fragmentsThisWeek.put(rarity, 0);
            }
            this.lastResetTime = System.currentTimeMillis();
            this.currentDailyQuest = null;
            this.questProgress = 0;
        }
    }

    /**
     * Carrega dados do disco
     */
    public static void load() {
        playerData.clear();
        ensureFile();
        if (!Files.exists(FILE_PATH))
            return;

        try (BufferedReader br = Files.newBufferedReader(FILE_PATH, StandardCharsets.UTF_8)) {
            Type type = new TypeToken<Map<UUID, PlayerFarmingData>>() {
            }.getType();
            Map<UUID, PlayerFarmingData> loaded = GSON.fromJson(br, type);
            if (loaded != null) {
                playerData.putAll(loaded);
                EssentialsMod.LOGGER.info("Loaded lootbox farming data for {} players", playerData.size());
            }
        } catch (IOException e) {
            EssentialsMod.LOGGER.error("Failed to load lootbox farming data", e);
        }
    }

    /**
     * Salva dados no disco
     */
    public static void save() {
        ensureFile();
        try (BufferedWriter bw = Files.newBufferedWriter(FILE_PATH, StandardCharsets.UTF_8)) {
            bw.write(GSON.toJson(playerData));
        } catch (IOException e) {
            EssentialsMod.LOGGER.error("Failed to save lootbox farming data", e);
        }
    }

    private static void ensureFile() {
        try {
            Files.createDirectories(FILE_PATH.getParent());
            if (!Files.exists(FILE_PATH)) {
                Files.writeString(FILE_PATH, "{}", StandardCharsets.UTF_8);
            }
        } catch (IOException e) {
            EssentialsMod.LOGGER.error("Failed to ensure lootbox farming file", e);
        }
    }

    /**
     * Obtém ou cria dados de farming para um jogador
     */
    public static PlayerFarmingData getOrCreateData(UUID playerId) {
        return playerData.computeIfAbsent(playerId, PlayerFarmingData::new);
    }

    /**
     * Adiciona fragmentos para um jogador
     * 
     * @return quantidade realmente adicionada (pode ser menor que amount se atingir
     *         limite)
     */
    public static int addFragments(ServerPlayer player, String rarity, int amount) {
        if (!LootboxManager.RARITIES.contains(rarity) || amount <= 0)
            return 0;

        PlayerFarmingData data = getOrCreateData(player.getUUID());
        checkAndResetWeekly(data);

        int current = data.fragmentsThisWeek.getOrDefault(rarity, 0);
        int limit = DEFAULT_WEEKLY_LIMITS.getOrDefault(rarity, 999999);
        int remaining = limit - current;

        if (remaining <= 0) {
            player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                    "§cVocê atingiu o limite semanal de fragmentos §e" + rarity + "§c! (§f" + limit + "§c/semana)"));
            return 0;
        }

        int toAdd = Math.min(amount, remaining);
        data.fragmentsThisWeek.put(rarity, current + toAdd);

        // Dar o item ao jogador
        ItemStack fragmentStack = switch (rarity) {
            case LootboxManager.R_INCOMUM -> new ItemStack(ModItems.LOOTBOX_FRAGMENT_INCOMUM.get(), toAdd);
            case LootboxManager.R_RARA -> new ItemStack(ModItems.LOOTBOX_FRAGMENT_RARA.get(), toAdd);
            case LootboxManager.R_EPICA -> new ItemStack(ModItems.LOOTBOX_FRAGMENT_EPICA.get(), toAdd);
            case LootboxManager.R_LENDARIA -> new ItemStack(ModItems.LOOTBOX_FRAGMENT_LENDARIA.get(), toAdd);
            default -> ItemStack.EMPTY;
        };

        if (!fragmentStack.isEmpty()) {
            player.getInventory().placeItemBackInInventory(fragmentStack);
            player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                    "§a+§e" + toAdd + " §aFragmento(s) de Lootbox §f" + rarity.toUpperCase() + " §a(§f" +
                            (current + toAdd) + "§a/§f" + limit + "§a)"));
        }

        save();
        return toAdd;
    }

    /**
     * Verifica se é hora de resetar os dados semanais
     * Reset acontece toda segunda-feira às 00:00
     */
    private static void checkAndResetWeekly(PlayerFarmingData data) {
        long now = System.currentTimeMillis();
        ZonedDateTime lastReset = ZonedDateTime.ofInstant(Instant.ofEpochMilli(data.lastResetTime),
                ZoneId.systemDefault());
        ZonedDateTime nowDate = ZonedDateTime.ofInstant(Instant.ofEpochMilli(now), ZoneId.systemDefault());

        // Calcular próxima segunda-feira após o último reset
        ZonedDateTime nextMonday = lastReset.with(DayOfWeek.MONDAY);
        if (lastReset.getDayOfWeek() == DayOfWeek.MONDAY && lastReset.getHour() >= 0) {
            nextMonday = nextMonday.plusWeeks(1);
        }
        nextMonday = nextMonday.withHour(0).withMinute(0).withSecond(0).withNano(0);

        // Se já passou da próxima segunda, resetar
        if (nowDate.isAfter(nextMonday) || nowDate.isEqual(nextMonday)) {
            EssentialsMod.LOGGER.info("Resetting weekly farming data for player {}", data.playerId);
            for (String rarity : LootboxManager.RARITIES) {
                data.fragmentsThisWeek.put(rarity, 0);
            }
            data.lastResetTime = now;
        }
    }

    /**
     * Reseta dados de farming de um jogador (admin)
     */
    public static void resetPlayer(UUID playerId) {
        PlayerFarmingData data = playerData.get(playerId);
        if (data != null) {
            for (String rarity : LootboxManager.RARITIES) {
                data.fragmentsThisWeek.put(rarity, 0);
            }
            data.lastResetTime = System.currentTimeMillis();
            data.currentDailyQuest = null;
            data.questProgress = 0;
            save();
        }
    }

    /**
     * Reseta todos os jogadores (admin)
     */
    public static void resetAll() {
        for (PlayerFarmingData data : playerData.values()) {
            for (String rarity : LootboxManager.RARITIES) {
                data.fragmentsThisWeek.put(rarity, 0);
            }
            data.lastResetTime = System.currentTimeMillis();
            data.currentDailyQuest = null;
            data.questProgress = 0;
        }
        save();
    }

    /**
     * Obtém fragmentos ganhos esta semana
     */
    public static int getFragmentsThisWeek(UUID playerId, String rarity) {
        PlayerFarmingData data = playerData.get(playerId);
        if (data == null)
            return 0;
        checkAndResetWeekly(data);
        return data.fragmentsThisWeek.getOrDefault(rarity, 0);
    }

    /**
     * Obtém limite semanal para uma raridade
     */
    public static int getWeeklyLimit(String rarity) {
        return DEFAULT_WEEKLY_LIMITS.getOrDefault(rarity, 999999);
    }

    /**
     * Verifica se jogador atingiu o limite semanal
     */
    public static boolean hasReachedLimit(UUID playerId, String rarity) {
        int current = getFragmentsThisWeek(playerId, rarity);
        int limit = getWeeklyLimit(rarity);
        return current >= limit;
    }

    /**
     * Obtém todos os dados de jogadores (para leaderboard)
     */
    public static Map<UUID, PlayerFarmingData> getAllData() {
        return Collections.unmodifiableMap(playerData);
    }

    /**
     * Conta quantos fragmentos de uma raridade o jogador tem no inventário
     */
    public static int countFragmentsInInventory(ServerPlayer player, String rarity) {
        ItemStack fragmentItem = switch (rarity) {
            case LootboxManager.R_INCOMUM -> new ItemStack(ModItems.LOOTBOX_FRAGMENT_INCOMUM.get());
            case LootboxManager.R_RARA -> new ItemStack(ModItems.LOOTBOX_FRAGMENT_RARA.get());
            case LootboxManager.R_EPICA -> new ItemStack(ModItems.LOOTBOX_FRAGMENT_EPICA.get());
            case LootboxManager.R_LENDARIA -> new ItemStack(ModItems.LOOTBOX_FRAGMENT_LENDARIA.get());
            default -> ItemStack.EMPTY;
        };

        if (fragmentItem.isEmpty())
            return 0;

        int count = 0;
        for (ItemStack stack : player.getInventory().items) {
            if (ItemStack.isSameItem(stack, fragmentItem)) {
                count += stack.getCount();
            }
        }
        return count;
    }

    /**
     * Remove fragmentos do inventário do jogador
     */
    public static boolean removeFragmentsFromInventory(ServerPlayer player, String rarity, int amount) {
        ItemStack fragmentItem = switch (rarity) {
            case LootboxManager.R_INCOMUM -> new ItemStack(ModItems.LOOTBOX_FRAGMENT_INCOMUM.get());
            case LootboxManager.R_RARA -> new ItemStack(ModItems.LOOTBOX_FRAGMENT_RARA.get());
            case LootboxManager.R_EPICA -> new ItemStack(ModItems.LOOTBOX_FRAGMENT_EPICA.get());
            case LootboxManager.R_LENDARIA -> new ItemStack(ModItems.LOOTBOX_FRAGMENT_LENDARIA.get());
            default -> ItemStack.EMPTY;
        };

        if (fragmentItem.isEmpty())
            return false;

        int remaining = amount;
        for (int i = 0; i < player.getInventory().items.size(); i++) {
            ItemStack stack = player.getInventory().items.get(i);
            if (ItemStack.isSameItem(stack, fragmentItem)) {
                int toRemove = Math.min(remaining, stack.getCount());
                stack.shrink(toRemove);
                remaining -= toRemove;
                if (remaining <= 0)
                    break;
            }
        }

        return remaining == 0;
    }
}
