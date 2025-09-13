package org.lupz.doomsdayessentials.professions.bounty;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import org.lupz.doomsdayessentials.network.PacketHandler;

import java.util.function.Supplier;

public class RequestBountiesPacket {
	public static void encode(RequestBountiesPacket m, FriendlyByteBuf b){}
	public static RequestBountiesPacket decode(FriendlyByteBuf b){return new RequestBountiesPacket();}
	public static void handle(RequestBountiesPacket m, Supplier<NetworkEvent.Context> ctx){
		ctx.get().enqueueWork(() -> {
			ServerPlayer sp = ctx.get().getSender(); if (sp==null) return;
			org.lupz.doomsdayessentials.network.PacketHandler.CHANNEL.send(net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> sp), new BountiesListPacket(BountyManager.listAll()));
		});
		ctx.get().setPacketHandled(true);
	}
} 