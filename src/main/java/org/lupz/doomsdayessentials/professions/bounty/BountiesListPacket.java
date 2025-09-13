package org.lupz.doomsdayessentials.professions.bounty;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class BountiesListPacket {
	private final List<Entry> list;
	public static class Entry { public final String targetName; public final int gears; public Entry(String n,int g){this.targetName=n;this.gears=g;} }
	public BountiesListPacket(List<org.lupz.doomsdayessentials.professions.bounty.BountyManager.Bounty> server){
		this.list = new ArrayList<>();
		for (var b : server){
			String name = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayer(b.target) != null ?
				net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayer(b.target).getName().getString() : b.target.toString().substring(0,8);
			this.list.add(new Entry(name, b.gears));
		}
	}
	private BountiesListPacket(List<Entry> list, boolean dummy){ this.list=list; }
	public static void encode(BountiesListPacket msg, FriendlyByteBuf buf){ buf.writeVarInt(msg.list.size()); for (var e: msg.list){ buf.writeUtf(e.targetName); buf.writeVarInt(e.gears);} }
	public static BountiesListPacket decode(FriendlyByteBuf buf){ int n=buf.readVarInt(); List<Entry> l=new ArrayList<>(); for(int i=0;i<n;i++){ l.add(new Entry(buf.readUtf(), buf.readVarInt())); } return new BountiesListPacket(l,true);} 
	public static void handle(BountiesListPacket msg, Supplier<NetworkEvent.Context> ctx){
		ctx.get().enqueueWork(() -> {
			List<BountyClientData.Entry> list = new ArrayList<>();
			for (var e : msg.list) list.add(new BountyClientData.Entry(e.targetName, e.gears));
			BountyClientData.set(list);
		});
		ctx.get().setPacketHandled(true);
	}
} 