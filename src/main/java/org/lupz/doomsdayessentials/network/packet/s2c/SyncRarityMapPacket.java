package org.lupz.doomsdayessentials.network.packet.s2c;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkEvent;
import org.lupz.doomsdayessentials.rarity.RarityClientOverrides;
import org.lupz.doomsdayessentials.rarity.RarityManager;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/** Synchronize global item->rarity map from server to clients. */
public class SyncRarityMapPacket {
    private final Map<String, RarityManager.RarityTier> items;
    private final Map<String, RarityManager.RarityTier> variants;

    public SyncRarityMapPacket(Map<String, RarityManager.RarityTier> items,
                               Map<String, RarityManager.RarityTier> variants) {
        this.items = items;
        this.variants = variants;
    }

    public static SyncRarityMapPacket full(Map<String, RarityManager.RarityTier> items,
                                           Map<String, RarityManager.RarityTier> variants) {
        return new SyncRarityMapPacket(items, variants);
    }

    // Back-compat ctor for servers that only send item map (not used here but kept for safety)
    public SyncRarityMapPacket(Map<String, RarityManager.RarityTier> itemsOnly) {
        this(itemsOnly, Map.of());
    }

    public static void encode(SyncRarityMapPacket pkt, FriendlyByteBuf buf) {
        // items
        Map<String, RarityManager.RarityTier> map = pkt.items;
        buf.writeVarInt(map.size());
        for (Map.Entry<String, RarityManager.RarityTier> e : map.entrySet()) {
            buf.writeUtf(e.getKey());
            buf.writeVarInt(e.getValue().ordinal());
        }
        // variants
        Map<String, RarityManager.RarityTier> vmap = pkt.variants;
        buf.writeVarInt(vmap.size());
        for (Map.Entry<String, RarityManager.RarityTier> e : vmap.entrySet()) {
            buf.writeUtf(e.getKey());
            buf.writeVarInt(e.getValue().ordinal());
        }
    }

    public static SyncRarityMapPacket decode(FriendlyByteBuf buf) {
        int n = buf.readVarInt();
        Map<String, RarityManager.RarityTier> items = new HashMap<>(n);
        for (int i = 0; i < n; i++) {
            String id = buf.readUtf();
            int ord = buf.readVarInt();
            RarityManager.RarityTier tier = null;
            if (ord >= 0 && ord < RarityManager.RarityTier.values().length) tier = RarityManager.RarityTier.values()[ord];
            if (tier != null) items.put(id, tier);
        }
        int m = buf.readVarInt();
        Map<String, RarityManager.RarityTier> variants = new HashMap<>(m);
        for (int i = 0; i < m; i++) {
            String id = buf.readUtf();
            int ord = buf.readVarInt();
            RarityManager.RarityTier tier = null;
            if (ord >= 0 && ord < RarityManager.RarityTier.values().length) tier = RarityManager.RarityTier.values()[ord];
            if (tier != null) variants.put(id, tier);
        }
        return new SyncRarityMapPacket(items, variants);
    }

    public static void handle(SyncRarityMapPacket pkt, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> clientHandle(pkt));
        ctx.get().setPacketHandled(true);
    }

    @OnlyIn(Dist.CLIENT)
    private static void clientHandle(SyncRarityMapPacket pkt) {
        RarityClientOverrides.replaceAll(pkt.items, pkt.variants);
    }
}
