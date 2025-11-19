package org.lupz.doomsdayessentials.event.eclipse.market.admin;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import org.lupz.doomsdayessentials.client.screen.NightMarketAdminScreen;

import java.util.UUID;
import java.util.function.Supplier;

/** S2C: Push refreshed market data to the client admin UI without reopening. */
public class NightMarketAdminRefreshPacket {
    private final UUID marketId;
    private final BlockPos pos;
    private final String offersJson;
    private final String activePreset;

    public NightMarketAdminRefreshPacket(UUID marketId, BlockPos pos, String offersJson, String activePreset){
        this.marketId = marketId; this.pos = pos; this.offersJson = offersJson; this.activePreset = activePreset==null?"":activePreset;
    }

    public static void encode(NightMarketAdminRefreshPacket msg, FriendlyByteBuf buf){
        buf.writeUUID(msg.marketId);
        buf.writeBoolean(msg.pos != null);
        if (msg.pos != null) buf.writeBlockPos(msg.pos);
        buf.writeUtf(msg.offersJson);
        buf.writeUtf(msg.activePreset);
    }

    public static NightMarketAdminRefreshPacket decode(FriendlyByteBuf buf){
        UUID id = buf.readUUID();
        BlockPos pos = buf.readBoolean() ? buf.readBlockPos() : null;
        String json = buf.readUtf();
        String ap = buf.readUtf();
        return new NightMarketAdminRefreshPacket(id, pos, json, ap);
    }

    public static void handle(NightMarketAdminRefreshPacket msg, Supplier<NetworkEvent.Context> ctx){
        ctx.get().enqueueWork(() -> {
            if (Minecraft.getInstance().screen instanceof NightMarketAdminScreen screen){
                // Only update if the same market and position
                screen.updateData(msg.offersJson, msg.activePreset);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
