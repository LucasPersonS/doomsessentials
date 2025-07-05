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
import org.jetbrains.annotations.NotNull;
import org.lupz.doomsdayessentials.professions.shop.EngineerShopUtil;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;

/** GUI container for Engineer crafting workshop (read-only). */
public class EngineerCraftMenu extends AbstractContainerMenu {
    private final Container container;

    public EngineerCraftMenu(int windowId, Inventory inv) {
        super(ProfessionMenuTypes.ENGINEER_CRAFT.get(), windowId);
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
        for (var entry : EngineerShopUtil.getEntries().values()) {
            if (slot >= 27) break;
            var item = ForgeRegistries.ITEMS.getValue(entry.outputId());
            if (item == null) continue;
            ItemStack stack = new ItemStack(item, entry.outputCount());
            List<Component> lore = new ArrayList<>();
            for (var cost : entry.costs().entrySet()) {
                var costIt = ForgeRegistries.ITEMS.getValue(cost.getKey());
                if(costIt==null) continue;
                Component line = Component.literal("Custo: ").withStyle(net.minecraft.ChatFormatting.GREEN)
                        .append(Component.literal(cost.getValue()+"x ").withStyle(net.minecraft.ChatFormatting.GREEN))
                        .append(Component.literal(costIt.getDescription().getString()).withStyle(net.minecraft.ChatFormatting.GRAY));
                lore.add(line);
            }
            addLore(stack, lore);
            this.container.setItem(slot++, stack);
        }
        while (slot < 27) this.container.setItem(slot++, ItemStack.EMPTY);
    }

    private void addLore(ItemStack stack, List<Component> lore) {
        ListTag tag = new ListTag();
        for (Component c : lore) tag.add(StringTag.valueOf(Component.Serializer.toJson(c)));
        stack.getOrCreateTagElement("display").put("Lore", tag);
    }

    @Override public boolean stillValid(@NotNull Player p) { return true; }
    @Override public @NotNull ItemStack quickMoveStack(Player p, int i) { return ItemStack.EMPTY; }

    private static class ReadOnlySlot extends Slot {
        public ReadOnlySlot(Container c,int idx,int x,int y){super(c,idx,x,y);} @Override public boolean mayPlace(@NotNull ItemStack s){return false;} @Override public boolean mayPickup(@NotNull Player p){return false;}
    }
} 