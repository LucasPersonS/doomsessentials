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
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;
import org.lupz.doomsdayessentials.config.EssentialsConfig;

import java.util.ArrayList;
import java.util.List;

/** Read-only menu listing as recompensas configuradas para Médicos. */
public class MedicRewardMenu extends AbstractContainerMenu {
    private final Container container;

    public MedicRewardMenu(int windowId, Inventory playerInv) {
        super(ProfessionMenuTypes.MEDIC_REWARD_MENU.get(), windowId);
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
        for (String line : EssentialsConfig.MEDICO_REWARD_ITEMS.get()) {
            if (slot >= 27) break;
            String[] parts = line.split(",");
            try {
                if (parts.length == 2) {
                    var id = new net.minecraft.resources.ResourceLocation(parts[0]);
                    int count = Integer.parseInt(parts[1]);
                    var item = ForgeRegistries.ITEMS.getValue(id);
                    if (item == null) continue;
                    ItemStack stack = new ItemStack(item, count);
                    List<Component> lore = new ArrayList<>();
                    lore.add(Component.literal("§7Recompensa rotativa para Médicos"));
                    addLore(stack, lore);
                    this.container.setItem(slot++, stack);
                }
            } catch (Exception ignored) {}
        }
        while (slot < 27) this.container.setItem(slot++, ItemStack.EMPTY);
    }

    private void addLore(ItemStack stack, List<Component> lore) {
        ListTag tag = new ListTag();
        for (Component c : lore) tag.add(StringTag.valueOf(Component.Serializer.toJson(c)));
        stack.getOrCreateTagElement("display").put("Lore", tag);
    }

    @Override public boolean stillValid(@NotNull Player p) { return true; }
    @Override public @NotNull ItemStack quickMoveStack(Player p, int idx) { return ItemStack.EMPTY; }

    private static class ReadOnlySlot extends Slot {
        public ReadOnlySlot(Container cont,int index,int x,int y){super(cont,index,x,y);} @Override public boolean mayPlace(@NotNull ItemStack s){return false;} @Override public boolean mayPickup(@NotNull Player p){return false;}
    }
} 