package org.lupz.doomsdayessentials.professions.bounty;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class BountiesListPacket {
	private final List<Entry> list;
	public static class Entry { public final String targetName; public final String rewardName; public final int amount; public Entry(String n,String r,int a){this.targetName=n;this.rewardName=r;this.amount=a;} }
	public BountiesListPacket(List<org.lupz.doomsdayessentials.professions.bounty.BountyManager.Bounty> server){
		this.list = new ArrayList<>();
		for (var b : server){
			String name = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayer(b.target) != null ?
				net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayer(b.target).getName().getString() : b.target.toString().substring(0,8);
			String reward = new net.minecraft.world.item.ItemStack(b.rewardItem).getHoverName().getString();
			this.list.add(new Entry(name, reward, b.amount));
		}
	}
	private BountiesListPacket(List<Entry> list, boolean dummy){ this.list=list; }
	public static void encode(BountiesListPacket msg, FriendlyByteBuf buf){ buf.writeVarInt(msg.list.size()); for (var e: msg.list){ buf.writeUtf(e.targetName); buf.writeUtf(e.rewardName); buf.writeVarInt(e.amount);} }
	public static BountiesListPacket decode(FriendlyByteBuf buf){ int n=buf.readVarInt(); List<Entry> l=new ArrayList<>(); for(int i=0;i<n;i++){ l.add(new Entry(buf.readUtf(), buf.readUtf(), buf.readVarInt())); } return new BountiesListPacket(l,true);} 
	public static void handle(BountiesListPacket msg, Supplier<NetworkEvent.Context> ctx){
		ctx.get().enqueueWork(() -> {
			List<BountyClientData.Entry> list = new ArrayList<>();
			for (var e : msg.list) list.add(new BountyClientData.Entry(e.targetName, e.rewardName, e.amount));
			BountyClientData.set(list);
		});
		ctx.get().setPacketHandled(true);
	}
} 