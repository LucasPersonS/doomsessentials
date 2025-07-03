package org.lupz.doomsdayessentials.professions.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import org.lupz.doomsdayessentials.professions.shop.ShopUtil;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.function.Supplier;

public class BuyShopItemPacket {
    private final String alias;
    public BuyShopItemPacket(String alias) { this.alias = alias; }
    public static void encode(BuyShopItemPacket msg, FriendlyByteBuf buf) { buf.writeUtf(msg.alias); }
    public static BuyShopItemPacket decode(FriendlyByteBuf buf) { return new BuyShopItemPacket(buf.readUtf()); }
    public static void handle(BuyShopItemPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;
            var entry = ShopUtil.getEntries().get(msg.alias);
            if (entry == null) return;
            var costItem = ForgeRegistries.ITEMS.getValue(entry.costId());
            var outItem = ForgeRegistries.ITEMS.getValue(entry.outputId());
            if (costItem == null || outItem == null) return;
            int available = player.getInventory().countItem(costItem);
            if (available < entry.costCount()) {
                player.sendSystemMessage(net.minecraft.network.chat.Component.literal("§cVocê precisa de mais itens para comprar."));
                return;
            }
            int toRemove = entry.costCount();
            for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                net.minecraft.world.item.ItemStack stack = player.getInventory().getItem(i);
                if (stack.is(costItem)) {
                    int rem = Math.min(toRemove, stack.getCount());
                    stack.shrink(rem);
                    toRemove -= rem;
                    if (toRemove <= 0) break;
                }
            }
            player.getInventory().add(new net.minecraft.world.item.ItemStack(outItem, entry.outputCount()));
            player.sendSystemMessage(net.minecraft.network.chat.Component.literal("§aCompra realizada!"));

            // Mark inventory dirty and sync
            player.getInventory().setChanged();
            player.containerMenu.broadcastChanges();

            // Reopen shop to force client inventory refresh while keeping GUI open
            net.minecraftforge.network.NetworkHooks.openScreen(player, new org.lupz.doomsdayessentials.professions.menu.ShopMenuProvider());
        });
        ctx.get().setPacketHandled(true);
    }
} 