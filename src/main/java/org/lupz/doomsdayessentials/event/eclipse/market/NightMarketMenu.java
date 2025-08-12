package org.lupz.doomsdayessentials.event.eclipse.market;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.extensions.IForgeMenuType;

public class NightMarketMenu extends AbstractContainerMenu {
    public static final MenuType<NightMarketMenu> TYPE = IForgeMenuType.create((windowId, inv, data) -> new NightMarketMenu(windowId, inv));

    public NightMarketMenu(int id, Inventory inv){ super(TYPE, id); }
    public NightMarketMenu(int id, Inventory inv, FriendlyByteBuf buf){ this(id, inv); }

    @Override
    public boolean stillValid(Player p){ return true; }

    @Override
    public ItemStack quickMoveStack(Player player, int index){ return ItemStack.EMPTY; }
} 