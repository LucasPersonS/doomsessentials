package org.lupz.doomsdayessentials.network.packet.s2c;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import org.lupz.doomsdayessentials.client.ClientCombatState;
import org.lupz.doomsdayessentials.combat.ManagedArea;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;

public class SyncAreasPacket {

    private final List<ManagedArea> areas;

    public SyncAreasPacket(Collection<ManagedArea> areas) {
        this.areas = new ArrayList<>(areas);
    }

    public static void encode(SyncAreasPacket pkt, FriendlyByteBuf buf) {
        buf.writeCollection(pkt.areas, (b, area) -> area.write(b));
    }

    public static SyncAreasPacket decode(FriendlyByteBuf buf) {
        List<ManagedArea> areas = buf.readList(ManagedArea::read);
        return new SyncAreasPacket(areas);
    }

    public static void handle(SyncAreasPacket pkt, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ClientCombatState.setManagedAreas(pkt.areas);
        });
        ctx.get().setPacketHandled(true);
    }
} 