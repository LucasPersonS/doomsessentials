package org.lupz.doomsdayessentials.network.packet.s2c;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import org.lupz.doomsdayessentials.client.ClientCombatState;

import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

public class SyncCombatStatePacket {

    private final Map<UUID, Integer> combatState;

    public SyncCombatStatePacket(Map<UUID, Integer> combatState) {
        this.combatState = combatState;
    }

    public static void encode(SyncCombatStatePacket pkt, FriendlyByteBuf buf) {
        buf.writeMap(pkt.combatState, FriendlyByteBuf::writeUUID, FriendlyByteBuf::writeInt);
    }

    public static SyncCombatStatePacket decode(FriendlyByteBuf buf) {
        Map<UUID, Integer> map = buf.readMap(FriendlyByteBuf::readUUID, FriendlyByteBuf::readInt);
        return new SyncCombatStatePacket(map);
    }

    public static void handle(SyncCombatStatePacket pkt, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ClientCombatState.setPlayersInCombat(pkt.combatState);
        });
        ctx.get().setPacketHandled(true);
    }
} 