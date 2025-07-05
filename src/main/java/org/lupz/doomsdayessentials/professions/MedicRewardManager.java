package org.lupz.doomsdayessentials.professions;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;
import org.lupz.doomsdayessentials.config.EssentialsConfig;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Handles rotation and cooldown of medic rewards when they heal patients.
 */
public final class MedicRewardManager {

    private static final MedicRewardManager INSTANCE = new MedicRewardManager();
    public static MedicRewardManager get() { return INSTANCE; }

    private static final Duration COOLDOWN = Duration.ofDays(3);

    private record RewardEntry(ResourceLocation id, int count) {}

    private static final Codec<MedicData> MEDIC_DATA_CODEC = MedicData.CODEC;
    private static final Codec<Map<String, MedicData>> STORAGE_CODEC = Codec.unboundedMap(Codec.STRING, MEDIC_DATA_CODEC);

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    // ---------------------------------------------------------------------
    // Fields
    // ---------------------------------------------------------------------

    private final List<RewardEntry> rewardPool = new ArrayList<>();
    private final Map<UUID, MedicData> medicData = new ConcurrentHashMap<>();
    private final Path saveFile;

    private MedicRewardManager() {
        Path configDir = Path.of("config", "doomsdayessentials");
        this.saveFile = configDir.resolve("medic_rewards.json");
        if (!Files.exists(configDir)) {
            try { Files.createDirectories(configDir); } catch (IOException ignored) {}
        }
        reloadConfig();
        load();
    }

    // ---------------------------------------------------------------------
    // Public API
    // ---------------------------------------------------------------------

    /**
     * Grants a reward to the medic for the specified patient if eligible.
     * Returns true if a reward was granted.
     */
    public synchronized boolean grantReward(@NotNull ServerPlayer medic, @NotNull UUID patientUuid) {
        if (rewardPool.isEmpty()) {
            return false; // Nothing configured
        }

        MedicData data = medicData.computeIfAbsent(medic.getUUID(), k -> new MedicData());
        long now = System.currentTimeMillis();
        Long last = data.patientTimestamps.get(patientUuid);
        if (last != null && now - last < COOLDOWN.toMillis()) {
            // Cooldown active
            return false;
        }

        // Pick next reward entry (rotating)
        RewardEntry entry = rewardPool.get(data.nextIndex % rewardPool.size());
        data.nextIndex = (data.nextIndex + 1) % rewardPool.size();

        // Give item
        Item item = ForgeRegistries.ITEMS.getValue(entry.id);
        if (item != null) {
            medic.getInventory().add(new ItemStack(item, entry.count));
            medic.sendSystemMessage(net.minecraft.network.chat.Component.literal("§aVocê recebeu " + entry.count + "x " + item.getDescription().getString() + " pela ajuda médica!"));
        }

        // Update timestamp and persist
        data.patientTimestamps.put(patientUuid, now);
        save();
        return true;
    }

    /**
     * Returns an immutable view of the configured reward pool.
     */
    public List<String> getRewardDescriptions() {
        List<String> list = new ArrayList<>();
        for (RewardEntry e : rewardPool) {
            Item item = ForgeRegistries.ITEMS.getValue(e.id);
            if (item != null) {
                list.add(e.count + "x " + item.getDescription().getString() + " (" + e.id + ")");
            }
        }
        return java.util.Collections.unmodifiableList(list);
    }

    /** Reload rewards from config. */
    public synchronized void reloadConfig() {
        rewardPool.clear();
        for (String line : EssentialsConfig.MEDICO_REWARD_ITEMS.get()) {
            String[] parts = line.split(",");
            try {
                if (parts.length == 2) {
                    ResourceLocation id = new ResourceLocation(parts[0]);
                    int count = Integer.parseInt(parts[1]);
                    rewardPool.add(new RewardEntry(id, count));
                }
            } catch (Exception ignored) {}
        }
    }

    // ---------------------------------------------------------------------
    // Persistence helpers
    // ---------------------------------------------------------------------

    private void load() {
        if (!Files.exists(saveFile)) return;
        try {
            String json = Files.readString(saveFile);
            if (json.isEmpty()) return;
            JsonElement el = JsonParser.parseString(json);
            STORAGE_CODEC.parse(JsonOps.INSTANCE, el).result().ifPresent(map -> {
                medicData.clear();
                map.forEach((k,v) -> medicData.put(UUID.fromString(k), v));
            });
        } catch (IOException ignored) {}
    }

    private void save() {
        try {
            Map<String, MedicData> map = new HashMap<>();
            medicData.forEach((k,v) -> map.put(k.toString(), v));
            STORAGE_CODEC.encodeStart(JsonOps.INSTANCE, map).result().ifPresent(el -> {
                try { Files.writeString(saveFile, GSON.toJson(el)); } catch (IOException ignored) {}
            });
        } catch (Exception ignored) {}
    }

    // ---------------------------------------------------------------------
    // Nested serialisable classes
    // ---------------------------------------------------------------------

    public static class MedicData {
        int nextIndex = 0;
        Map<UUID, Long> patientTimestamps = new HashMap<>();

        public static final Codec<MedicData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.INT.fieldOf("idx").forGetter(d -> d.nextIndex),
                Codec.unboundedMap(Codec.STRING.xmap(UUID::fromString, UUID::toString), Codec.LONG).fieldOf("patients").forGetter(d -> d.patientTimestamps)
        ).apply(instance, MedicData::from));

        static MedicData from(int idx, Map<UUID, Long> map) {
            MedicData d = new MedicData();
            d.nextIndex = idx;
            d.patientTimestamps = map;
            return d;
        }
    }
} 