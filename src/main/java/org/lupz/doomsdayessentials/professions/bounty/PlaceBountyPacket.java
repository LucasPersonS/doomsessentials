package org.lupz.doomsdayessentials.professions.bounty;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class PlaceBountyPacket {
	private final String targetName; private final int gears;
	public PlaceBountyPacket(String targetName, int gears){this.targetName=targetName;this.gears=gears;}
	public static void encode(PlaceBountyPacket msg, FriendlyByteBuf buf){buf.writeUtf(msg.targetName); buf.writeVarInt(msg.gears);} 
	public static PlaceBountyPacket decode(FriendlyByteBuf buf){return new PlaceBountyPacket(buf.readUtf(), buf.readVarInt());}
	public static void handle(PlaceBountyPacket msg, Supplier<NetworkEvent.Context> ctx){
		ctx.get().enqueueWork(() -> {
			ServerPlayer sp = ctx.get().getSender(); if (sp==null) return;
			ServerPlayer target = sp.server.getPlayerList().getPlayerByName(msg.targetName);
			if (target==null) return;
			BountyManager.placeBounty(sp, target.getUUID(), msg.gears);
		});
		ctx.get().setPacketHandled(true);
	}
} 