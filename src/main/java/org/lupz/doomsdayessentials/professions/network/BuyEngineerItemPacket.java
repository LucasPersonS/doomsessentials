package org.lupz.doomsdayessentials.professions.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.registries.ForgeRegistries;
import org.lupz.doomsdayessentials.professions.EngenheiroProfession;
import org.lupz.doomsdayessentials.professions.shop.EngineerShopUtil;

import java.util.function.Supplier;

public class BuyEngineerItemPacket {
    private final String alias;
    public BuyEngineerItemPacket(String a){this.alias=a;}
    public static void encode(BuyEngineerItemPacket m,FriendlyByteBuf buf){buf.writeUtf(m.alias);} 
    public static BuyEngineerItemPacket decode(FriendlyByteBuf buf){return new BuyEngineerItemPacket(buf.readUtf());}
    public static void handle(BuyEngineerItemPacket msg, Supplier<NetworkEvent.Context> ctx){
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if(player==null) return;
            if(!EngenheiroProfession.isEngineer(player)){
                player.sendSystemMessage(Component.translatable("profession.engenheiro.not_engineer"));
                return;
            }
            var entry = EngineerShopUtil.getEntries().get(msg.alias);
            if(entry==null) return;
            var outItem  = ForgeRegistries.ITEMS.getValue(entry.outputId());
            if(outItem==null) return;
            for(var cost: entry.costs().entrySet()){
                var item = ForgeRegistries.ITEMS.getValue(cost.getKey());
                if(item==null) return;
                int avail = player.getInventory().countItem(item);
                if(avail < cost.getValue()) { player.sendSystemMessage(Component.literal("§cVocê precisa de mais itens para craftar.")); return; }
            }
            // remove costs
            for(var cost: entry.costs().entrySet()){
                var item = ForgeRegistries.ITEMS.getValue(cost.getKey());
                int toRemove = cost.getValue();
                for(int i=0;i<player.getInventory().getContainerSize();i++){
                    var st = player.getInventory().getItem(i);
                    if(st.is(item)){
                        int rem = Math.min(toRemove, st.getCount()); st.shrink(rem); toRemove-=rem; if(toRemove<=0) break;
                    }
                }
            }
            player.getInventory().add(new net.minecraft.world.item.ItemStack(outItem, entry.outputCount()));
            player.sendSystemMessage(Component.literal("§aItem fabricado!"));
            player.getInventory().setChanged();
            player.containerMenu.broadcastChanges();
        });
        ctx.get().setPacketHandled(true);
    }
} 