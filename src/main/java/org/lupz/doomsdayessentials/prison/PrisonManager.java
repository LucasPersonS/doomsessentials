package org.lupz.doomsdayessentials.prison;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.lupz.doomsdayessentials.combat.AreaManager;
import org.lupz.doomsdayessentials.combat.ManagedArea;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE)
public class PrisonManager {

    private static PrisonManager INSTANCE;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final File SAVE_FILE = new File("world/dooms_prison_data.json");

    private final Map<UUID, PrisonerData> prisoners = new ConcurrentHashMap<>();

    public static PrisonManager get() {
        if (INSTANCE == null) {
            INSTANCE = new PrisonManager();
            INSTANCE.load();
        }
        return INSTANCE;
    }

    public static class PrisonerData {
        public UUID playerUUID;
        public long releaseTime; // Unix timestamp in seconds
        public BlockPos originalPosition;
        public String originalDimension;

        public PrisonerData() {
        }

        public PrisonerData(UUID playerUUID, long releaseTime, BlockPos originalPos, ResourceKey<Level> originalDim) {
            this.playerUUID = playerUUID;
            this.releaseTime = releaseTime;
            this.originalPosition = originalPos;
            this.originalDimension = originalDim.location().toString();
        }

        public int getRemainingSeconds() {
            long now = System.currentTimeMillis() / 1000;
            return (int) Math.max(0, releaseTime - now);
        }
    }

    /**
     * Jails a player: teleport to prison zone and record their sentence
     * 
     * @param player          The player to jail
     * @param prisonZoneName  The name of the prison zone area
     * @param durationSeconds How long to jail them for (in seconds)
     */
    public boolean jailPlayer(ServerPlayer player, String prisonZoneName, int durationSeconds) {
        ManagedArea prisonZone = AreaManager.get().getArea(prisonZoneName);
        if (prisonZone == null) {
            return false;
        }

        // Save original location
        BlockPos originalPos = player.blockPosition();
        ResourceKey<Level> originalDim = player.serverLevel().dimension();

        // Calculate release time
        long releaseTime = (System.currentTimeMillis() / 1000) + durationSeconds;

        // Store prisoner data
        PrisonerData data = new PrisonerData(player.getUUID(), releaseTime, originalPos, originalDim);
        prisoners.put(player.getUUID(), data);
        save();

        // Teleport to prison center
        BlockPos prisonCenter = new BlockPos(
                (prisonZone.getPos1().getX() + prisonZone.getPos2().getX()) / 2,
                prisonZone.getPos1().getY() + 1,
                (prisonZone.getPos1().getZ() + prisonZone.getPos2().getZ()) / 2);

        MinecraftServer server = player.getServer();
        if (server != null) {
            var level = server.getLevel(prisonZone.getDimension());
            if (level != null) {
                player.teleportTo(level, prisonCenter.getX() + 0.5, prisonCenter.getY(), prisonCenter.getZ() + 0.5,
                        player.getYRot(), player.getXRot());
            }
        }

        player.sendSystemMessage(
                Component.literal("§c§l[PRISÃO] §eVocê foi preso por " + formatTime(durationSeconds) + "!"));
        return true;
    }

    /**
     * Releases a player early from prison
     */
    public boolean releasePlayer(ServerPlayer player) {
        PrisonerData data = prisoners.remove(player.getUUID());
        if (data == null) {
            return false;
        }

        save();

        org.lupz.doomsdayessentials.combat.CombatManager.get().removeWanted(player.getUUID());

        org.lupz.doomsdayessentials.network.PacketHandler.CHANNEL.send(
                net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> player),
                new org.lupz.doomsdayessentials.network.packet.s2c.SyncPrisonTimePacket(0));

        // Teleport back to original location
        MinecraftServer server = player.getServer();
        if (server != null && data.originalDimension != null && data.originalPosition != null) {
            ResourceKey<Level> dimKey = ResourceKey.create(
                    net.minecraft.core.registries.Registries.DIMENSION,
                    net.minecraft.resources.ResourceLocation.parse(data.originalDimension));
            var level = server.getLevel(dimKey);
            if (level != null) {
                player.teleportTo(level, data.originalPosition.getX() + 0.5,
                        data.originalPosition.getY(),
                        data.originalPosition.getZ() + 0.5,
                        player.getYRot(), player.getXRot());
            }
        }

        player.sendSystemMessage(Component.literal("§a§l[LIBERDADE] §eVocê foi liberado da prisão!"));
        return true;
    }

    /**
     * Check if a player is currently imprisoned
     */
    public boolean isPrisoner(UUID playerUUID) {
        return prisoners.containsKey(playerUUID);
    }

    /**
     * Get remaining prison time in seconds, or 0 if not imprisoned
     */
    public int getRemainingTime(UUID playerUUID) {
        PrisonerData data = prisoners.get(playerUUID);
        return data != null ? data.getRemainingSeconds() : 0;
    }

    /**
     * Automatic release check - called every server tick
     */
    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END)
            return;

        PrisonManager manager = get();
        long now = System.currentTimeMillis() / 1000;

        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null)
            return;

        manager.prisoners.entrySet().removeIf(entry -> {
            PrisonerData data = entry.getValue();
            if (now >= data.releaseTime) {
                // Time to release this prisoner
                ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
                if (player != null) {
                    manager.releasePlayer(player);
                } else {
                    // Player offline: mantém registro para soltar ao logar
                    return false;
                }
                return true;
            }
            return false;
        });

        for (UUID uuid : manager.prisoners.keySet()) {
            ServerPlayer p = server.getPlayerList().getPlayer(uuid);
            if (p != null) {
                int secs = manager.getRemainingTime(uuid);
                org.lupz.doomsdayessentials.network.PacketHandler.CHANNEL.send(net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> p), new org.lupz.doomsdayessentials.network.packet.s2c.SyncPrisonTimePacket(secs));
            }
        }
    }

    private String formatTime(int seconds) {
        int hours = seconds / 3600;
        int minutes = (seconds % 3600) / 60;
        int secs = seconds % 60;

        if (hours > 0) {
            return String.format("%dh %dm %ds", hours, minutes, secs);
        } else if (minutes > 0) {
            return String.format("%dm %ds", minutes, secs);
        } else {
            return String.format("%ds", secs);
        }
    }

    private void save() {
        try {
            SAVE_FILE.getParentFile().mkdirs();
            try (FileWriter writer = new FileWriter(SAVE_FILE)) {
                GSON.toJson(prisoners, writer);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void load() {
        if (!SAVE_FILE.exists())
            return;
        try (FileReader reader = new FileReader(SAVE_FILE)) {
            Type type = new TypeToken<ConcurrentHashMap<UUID, PrisonerData>>() {
            }.getType();
            Map<UUID, PrisonerData> loaded = GSON.fromJson(reader, type);
            if (loaded != null) {
                prisoners.putAll(loaded);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
