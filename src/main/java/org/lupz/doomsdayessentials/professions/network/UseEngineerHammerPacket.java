package org.lupz.doomsdayessentials.professions.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import org.lupz.doomsdayessentials.professions.EngenheiroProfession;

import java.util.function.Supplier;

public class UseEngineerHammerPacket {

    public UseEngineerHammerPacket() {
    }

    public static void encode(UseEngineerHammerPacket pkt, FriendlyByteBuf buf) {
    }

    public static UseEngineerHammerPacket decode(FriendlyByteBuf buf) {
        return new UseEngineerHammerPacket();
    }

    public static void handle(UseEngineerHammerPacket pkt, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null) {
                EngenheiroProfession.handleHammerUse(player);
            }
        });
        ctx.get().setPacketHandled(true);
    }
} 