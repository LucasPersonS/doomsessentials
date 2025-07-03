package org.lupz.doomsdayessentials.professions.menu;

import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.NotNull;
import org.lupz.doomsdayessentials.professions.shop.ShopUtil;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;

/** GUI container for the professions shop. Read-only chest. */
public class ShopMenu extends AbstractContainerMenu {
    private final Container container;

    public ShopMenu(int windowId, Inventory playerInv) {
        super(ProfessionMenuTypes.SHOP_MENU.get(), windowId);
        this.container = new SimpleContainer(27);
        buildContents();

        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                this.addSlot(new ReadOnlySlot(this.container, col + row * 9, 8 + col * 18, 18 + row * 18));
            }
        }
    }

    private void buildContents() {
        int slot = 0;
        for (var entry : ShopUtil.getEntries().values()) {
            if (slot >= 27) break;
            var item = ForgeRegistries.ITEMS.getValue(entry.outputId());
            var costItem = ForgeRegistries.ITEMS.getValue(entry.costId());
            if (item == null || costItem == null) continue;
            ItemStack stack = new ItemStack(item, entry.outputCount());
            List<Component> lore = new ArrayList<>();
            lore.add(Component.literal("§7Custa " + entry.costCount() + "x " + costItem.getDescription().getString()));
            addLore(stack, lore);
            this.container.setItem(slot++, stack);
        }
        // fill remaining with barrier
        while (slot < 27) {
            this.container.setItem(slot++, ItemStack.EMPTY);
        }
    }

    private void addLore(ItemStack stack, List<Component> lore) {
        ListTag tag = new ListTag();
        for (Component c : lore) {
            tag.add(StringTag.valueOf(Component.Serializer.toJson(c)));
        }
        stack.getOrCreateTagElement("display").put("Lore", tag);
    }

    @Override
    public boolean stillValid(@NotNull Player pPlayer) { return true; }

    @Override
    public @NotNull ItemStack quickMoveStack(Player pPlayer, int pIndex) { return ItemStack.EMPTY; }

    private static class ReadOnlySlot extends Slot {
        public ReadOnlySlot(Container cont, int i, int x, int y) { super(cont, i, x, y); }
        @Override public boolean mayPlace(@NotNull ItemStack stack) { return false; }
        @Override public boolean mayPickup(@NotNull Player player) { return false; }
    }
} 