package org.lupz.doomsdayessentials.injury.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import org.lupz.doomsdayessentials.injury.InjuryEvents;

import java.util.function.Supplier;

public class PlayerActionPacket {

    private final Action action;

    public PlayerActionPacket(Action action) {
        this.action = action;
    }

    public PlayerActionPacket(FriendlyByteBuf buf) {
        this.action = buf.readEnum(Action.class);
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeEnum(this.action);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            switch (this.action) {
                case GIVE_UP:
                    InjuryEvents.killPlayer(player);
                    break;
            }
        });
        ctx.get().setPacketHandled(true);
    }


    public enum Action {
        GIVE_UP
    }
} 