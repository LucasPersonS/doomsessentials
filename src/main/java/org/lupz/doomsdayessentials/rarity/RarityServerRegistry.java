package org.lupz.doomsdayessentials.rarity;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.lupz.doomsdayessentials.network.PacketHandler;
import org.lupz.doomsdayessentials.network.packet.s2c.SyncRarityMapPacket;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/** Server-side registry of global item rarity tiers, synced to all clients. */
public final class RarityServerRegistry {
    private static final Map<String, RarityManager.RarityTier> ITEM_TO_TIER = new HashMap<>();
    // Composite key: itemId + "|" + variantId (e.g., TACZ GunId)
    private static final Map<String, RarityManager.RarityTier> VARIANT_TO_TIER = new HashMap<>();

    private RarityServerRegistry() {}

    public static synchronized void setItemTier(String itemId, RarityManager.RarityTier tier) {
        if (itemId == null || tier == null) return;
        ITEM_TO_TIER.put(itemId, tier);
        broadcastFull();
    }

    public static synchronized void removeItemTier(String itemId) {
        if (itemId == null) return;
        ITEM_TO_TIER.remove(itemId);
        broadcastFull();
    }

    public static synchronized RarityManager.RarityTier getItemTier(String itemId) {
        if (itemId == null) return null;
        return ITEM_TO_TIER.get(itemId);
    }

    public static synchronized void setVariantTier(String itemId, String variantId, RarityManager.RarityTier tier) {
        if (itemId == null || variantId == null || tier == null) return;
        VARIANT_TO_TIER.put(itemId + "|" + variantId, tier);
        broadcastFull();
    }

    public static synchronized void removeVariantTier(String itemId, String variantId) {
        if (itemId == null || variantId == null) return;
        VARIANT_TO_TIER.remove(itemId + "|" + variantId);
        broadcastFull();
    }

    public static synchronized RarityManager.RarityTier getVariantTier(String itemId, String variantId) {
        if (itemId == null || variantId == null) return null;
        return VARIANT_TO_TIER.get(itemId + "|" + variantId);
    }

    public static synchronized Map<String, RarityManager.RarityTier> snapshot() {
        return Collections.unmodifiableMap(new HashMap<>(ITEM_TO_TIER));
    }

    public static synchronized Map<String, RarityManager.RarityTier> snapshotVariants() {
        return Collections.unmodifiableMap(new HashMap<>(VARIANT_TO_TIER));
    }

    public static void broadcastFull() {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;
        SyncRarityMapPacket pkt = SyncRarityMapPacket.full(snapshot(), snapshotVariants());
        for (ServerPlayer sp : server.getPlayerList().getPlayers()) {
            PacketHandler.CHANNEL.sendTo(pkt, sp.connection.connection, net.minecraftforge.network.NetworkDirection.PLAY_TO_CLIENT);
        }
    }
}
