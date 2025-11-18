package org.lupz.doomsdayessentials.event.eclipse.market.admin;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import org.lupz.doomsdayessentials.client.screen.NightMarketAdminScreen;

import java.util.UUID;
import java.util.function.Supplier;

/** S2C: Open Night Market Admin Screen on client with current offers and active preset. */
public class NightMarketAdminOpenPacket {
    private final UUID marketId;
    private final BlockPos pos;
    private final String offersJson;
    private final String activePreset;

    public NightMarketAdminOpenPacket(UUID marketId, BlockPos pos, String offersJson, String activePreset){
        this.marketId = marketId; this.pos = pos; this.offersJson = offersJson; this.activePreset = activePreset==null?"":activePreset;
    }

    public static void encode(NightMarketAdminOpenPacket msg, FriendlyByteBuf buf){
        buf.writeUUID(msg.marketId);
        buf.writeBlockPos(msg.pos);
        buf.writeUtf(msg.offersJson);
        buf.writeUtf(msg.activePreset);
    }

    public static NightMarketAdminOpenPacket decode(FriendlyByteBuf buf){
        UUID id = buf.readUUID();
        BlockPos pos = buf.readBlockPos();
        String offers = buf.readUtf();
        String preset = buf.readUtf();
        return new NightMarketAdminOpenPacket(id, pos, offers, preset);
    }

    public static void handle(NightMarketAdminOpenPacket msg, Supplier<NetworkEvent.Context> ctx){
        ctx.get().enqueueWork(() -> NightMarketAdminScreen.open(msg.marketId, msg.pos, msg.offersJson, msg.activePreset));
        ctx.get().setPacketHandled(true);
    }
}

