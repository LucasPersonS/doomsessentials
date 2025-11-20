package org.lupz.doomsdayessentials.event.eclipse.market;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.extensions.IForgeMenuType;

public class NightMarketMenu extends AbstractContainerMenu {

    private final net.minecraft.world.SimpleContainer ghost = new net.minecraft.world.SimpleContainer(3);
    private final java.util.UUID marketId;
    private final net.minecraft.core.BlockPos blockPos;

    public NightMarketMenu(int id, Inventory inv, FriendlyByteBuf buf){
        super(org.lupz.doomsdayessentials.event.eclipse.market.NightMarketMenus.NIGHT_MARKET_MENU.get(), id);
        this.marketId = buf.readUUID();
        this.blockPos = buf.readBlockPos();
        this.addSlot(new Slot(ghost, 0, 44, 35));
        this.addSlot(new Slot(ghost, 1, 44, 57));
        this.addSlot(new Slot(ghost, 2, 120, 46));
        int xOff = 8; int yOff = 84;
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                this.addSlot(new Slot(inv, col + row * 9 + 9, xOff + col * 18, yOff + row * 18));
            }
        }
        for (int i = 0; i < 9; ++i) this.addSlot(new Slot(inv, i, xOff + i * 18, yOff + 58));
    }

    public NightMarketMenu(int id, Inventory inv){
        super(org.lupz.doomsdayessentials.event.eclipse.market.NightMarketMenus.NIGHT_MARKET_MENU.get(), id);
        this.marketId = java.util.UUID.fromString("00000000-0000-0000-0000-000000000000");
        this.blockPos = net.minecraft.core.BlockPos.ZERO;
        this.addSlot(new Slot(ghost, 0, 44, 35));
        this.addSlot(new Slot(ghost, 1, 44, 57));
        this.addSlot(new Slot(ghost, 2, 120, 46));
        int xOff = 8; int yOff = 84;
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                this.addSlot(new Slot(inv, col + row * 9 + 9, xOff + col * 18, yOff + row * 18));
            }
        }
        for (int i = 0; i < 9; ++i) this.addSlot(new Slot(inv, i, xOff + i * 18, yOff + 58));
    }

    public java.util.UUID getMarketId(){ return marketId; }
    public net.minecraft.core.BlockPos getBlockPos(){ return blockPos; }

    @Override
    public boolean stillValid(Player p){ return true; }

    @Override
    public ItemStack quickMoveStack(Player player, int index){ return ItemStack.EMPTY; }

    @Override
    public void removed(Player player){
        if (player != null && !player.level().isClientSide){
            for (int i=0;i<ghost.getContainerSize();i++){
                ItemStack st = ghost.getItem(i);
                if (!st.isEmpty()){
                    ghost.setItem(i, ItemStack.EMPTY);
                    player.getInventory().placeItemBackInInventory(st);
                }
            }
        }
        super.removed(player);
    }

    public ItemStack getBuy1(){ return ghost.getItem(0); }
    public ItemStack getBuy2(){ return ghost.getItem(1); }
    public ItemStack getSell(){ return ghost.getItem(2); }
}
