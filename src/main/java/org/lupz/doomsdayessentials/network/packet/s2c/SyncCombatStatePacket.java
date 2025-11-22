package org.lupz.doomsdayessentials.network.packet.s2c;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import org.lupz.doomsdayessentials.client.ClientCombatState;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

public class SyncCombatStatePacket {

    private final Map<UUID, Integer> combatState;
    private final Set<UUID> wantedPlayers;

    public SyncCombatStatePacket(Map<UUID, Integer> combatState, Set<UUID> wantedPlayers) {
        this.combatState = combatState;
        this.wantedPlayers = wantedPlayers;
    }

    public static void encode(SyncCombatStatePacket pkt, FriendlyByteBuf buf) {
        buf.writeMap(pkt.combatState, FriendlyByteBuf::writeUUID, FriendlyByteBuf::writeInt);
        buf.writeCollection(pkt.wantedPlayers, FriendlyByteBuf::writeUUID);
    }

    public static SyncCombatStatePacket decode(FriendlyByteBuf buf) {
        Map<UUID, Integer> map = buf.readMap(FriendlyByteBuf::readUUID, FriendlyByteBuf::readInt);
        Set<UUID> wanted = buf.readCollection(java.util.HashSet::new, FriendlyByteBuf::readUUID);
        return new SyncCombatStatePacket(map, wanted);
    }

    public static void handle(SyncCombatStatePacket pkt, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ClientCombatState.setPlayersInCombat(pkt.combatState);
            ClientCombatState.setWantedPlayers(pkt.wantedPlayers);
        });
        ctx.get().setPacketHandled(true);
    }
}