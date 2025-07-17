package org.lupz.doomsdayessentials.menu;

import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.item.ItemStack;
import org.lupz.doomsdayessentials.block.RecycleBlockEntity;
import org.lupz.doomsdayessentials.professions.menu.ProfessionMenuTypes;
import org.jetbrains.annotations.NotNull;

/**
 * Container for the Recycler – 5 input + 5 output + player inventory.
 */
public class RecycleMenu extends AbstractContainerMenu {
    private final Container recycler;
    private final BlockPos pos;

    public RecycleMenu(int windowId, Inventory playerInv, RecycleBlockEntity be) {
        super(ProfessionMenuTypes.RECYCLE_MENU.get(), windowId);
        this.recycler = be;
        this.pos = be.getBlockPos();

        // Input slots (0-4)
        for (int i = 0; i < 5; i++) {
            this.addSlot(new Slot(recycler, i, 44 + i * 18, 20));
        }
        // Output slots (5-9)
        for (int i = 0; i < 5; i++) {
            this.addSlot(new OutputSlot(recycler, 5 + i, 44 + i * 18, 46));
        }

        // Player inventory 3 rows (start at 7,70)
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                this.addSlot(new Slot(playerInv, col + row * 9 + 9, 7 + col * 18, 70 + row * 18));
            }
        }
        // Hotbar (start Y 128)
        for (int col = 0; col < 9; ++col) {
            this.addSlot(new Slot(playerInv, col, 7 + col * 18, 128));
        }
    }

    @Override public boolean stillValid(@NotNull Player p) { return recycler.stillValid(p); }

    public BlockPos getBlockPos(){return pos;}
    public RecycleBlockEntity getBlockEntity() { return (RecycleBlockEntity) this.recycler; }
    public boolean isEnabled(){ return recycler instanceof RecycleBlockEntity rb && rb.isEnabled(); }

    @Override
    public @NotNull ItemStack quickMoveStack(Player pPlayer, int index) {
        Slot slot = this.slots.get(index);
        if (slot == null || !slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();

        if (index < 10) {
            // From recycler to player
            if (!this.moveItemStackTo(stack, 10, this.slots.size(), true)) return ItemStack.EMPTY;
        } else {
            // From player to input slots
            if (!this.moveItemStackTo(stack, 0, 5, false)) return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) slot.set(ItemStack.EMPTY); else slot.setChanged();
        return copy;
    }

    private static class OutputSlot extends Slot {
        public OutputSlot(Container cont, int idx, int x, int y) { super(cont, idx, x, y); }
        @Override public boolean mayPlace(@NotNull ItemStack stack) { return false; }
    }
} 