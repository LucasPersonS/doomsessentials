package org.lupz.doomsdayessentials.recycler.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import org.lupz.doomsdayessentials.block.RecycleBlockEntity;

import java.util.function.Supplier;

public class ToggleRecyclerPacket {

    private final BlockPos pos;

    public ToggleRecyclerPacket(BlockPos pos) {
        this.pos = pos;
    }

    public static void encode(ToggleRecyclerPacket msg, FriendlyByteBuf buf) {
        buf.writeBlockPos(msg.pos);
    }

    public static ToggleRecyclerPacket decode(FriendlyByteBuf buf) {
        return new ToggleRecyclerPacket(buf.readBlockPos());
    }

    public static void handle(ToggleRecyclerPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null) {
                if (player.level().getBlockEntity(msg.pos) instanceof RecycleBlockEntity be) {
                    be.toggle();
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }
} 