package org.lupz.doomsdayessentials.killfeed.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import org.lupz.doomsdayessentials.killfeed.client.KillFeedManager;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.api.distmarker.Dist;

import java.util.UUID;
import java.util.function.Supplier;

public class KillFeedPacket {
    private final UUID killerUUID;
    private final String killerName;
    private final UUID victimUUID;
    private final String victimName;
    private final String weaponName;
    private final String weaponId;
    private final String entityTypeId;
    private final boolean hasKiller;

    public KillFeedPacket(UUID killerUUID, String killerName, UUID victimUUID, String victimName, String weaponName, String weaponId, String entityTypeId) {
        this.killerUUID = killerUUID;
        this.killerName = killerName;
        this.victimUUID = victimUUID;
        this.victimName = victimName;
        this.weaponName = weaponName;
        this.weaponId = weaponId;
        this.entityTypeId = entityTypeId;
        this.hasKiller = killerUUID != null;
    }

    public KillFeedPacket(FriendlyByteBuf buf) {
        this.hasKiller = buf.readBoolean();
        if (this.hasKiller) {
            this.killerUUID = buf.readUUID();
            this.killerName = buf.readUtf();
        } else {
            this.killerUUID = null;
            this.killerName = "";
        }
        this.victimUUID = buf.readUUID();
        this.victimName = buf.readUtf();
        this.weaponName = buf.readUtf();
        this.weaponId = buf.readUtf();
        this.entityTypeId = buf.readUtf();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeBoolean(this.hasKiller);
        if (this.hasKiller) {
            buf.writeUUID(this.killerUUID);
            buf.writeUtf(this.killerName);
        }
        buf.writeUUID(this.victimUUID);
        buf.writeUtf(this.victimName);
        buf.writeUtf(this.weaponName);
        buf.writeUtf(this.weaponId);
        buf.writeUtf(this.entityTypeId);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                KillFeedManager.addEntry(killerUUID, killerName, victimUUID, victimName, weaponName, weaponId, entityTypeId);
            });
        });
        return true;
    }
} 