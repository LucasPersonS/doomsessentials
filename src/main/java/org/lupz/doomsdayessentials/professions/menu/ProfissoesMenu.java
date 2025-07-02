package org.lupz.doomsdayessentials.professions.menu;

import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Component.Serializer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.NotNull;
import org.lupz.doomsdayessentials.professions.ProfissaoManager;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Simple 3-row GUI that lets the player choose or abandon professions.
 * Only the Médicos emerald (slot 13) is currently implemented.
 */
public class ProfissoesMenu extends AbstractContainerMenu {

    private static final int PROFESSION_SLOTS = 27;

    private final Container container;
    private final Player player;

    public ProfissoesMenu(int windowId, Inventory playerInventory, FriendlyByteBuf data) {
        this(windowId, playerInventory);
    }

    public ProfissoesMenu(int windowId, Inventory playerInventory) {
        super(ProfessionMenuTypes.PROFISSOES_MENU.get(), windowId);
        this.player = playerInventory.player;
        this.container = new SimpleContainer(PROFESSION_SLOTS);
        createProfessionItems();

        // 3 rows, 9 columns like a chest
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                this.addSlot(new ReadOnlySlot(this.container, col + row * 9, 8 + col * 18, 18 + row * 18));
            }
        }
    }

    private void createProfessionItems() {
        if (ProfissaoManager.hasProfession(this.player.getUUID())) {
            ItemStack abandon = new ItemStack(Items.BARRIER);
            abandon.setHoverName(Component.literal("§cAbandonar Profissão"));

            // Add lore describing the action (single line)
            ListTag loreTag = new ListTag();
            loreTag.add(StringTag.valueOf(Component.Serializer.toJson(Component.literal("§7Abandone sua profissão clicando aqui."))));
            abandon.getOrCreateTagElement("display").put("Lore", loreTag);

            this.container.setItem(13, abandon);
            return;
        }

        createProfessionItem(11, Items.EMERALD, "medico");
        createProfessionItem(13, Items.IRON_SWORD, "combatente");
        createProfessionItem(15, Items.COMPASS, "rastreador");
    }

    private void createProfessionItem(int slot, net.minecraft.world.item.Item vanillaItem, String professionKey) {
        ItemStack stack = new ItemStack(vanillaItem);
        stack.setHoverName(Component.translatable("profession." + professionKey + ".gui.name"));

        ListTag loreTag = new ListTag();
        // assume up to 6 lore lines; stop when translation returns the raw key
        for (int i = 1; i <= 6; i++) {
            String key = "profession." + professionKey + ".gui.lore" + i;
            Component c = Component.translatable(key);
            // If translation missing, break
            if (c.getString().equals(key)) break;
            loreTag.add(StringTag.valueOf(Component.Serializer.toJson(c)));
        }
        if (!loreTag.isEmpty()) {
            stack.getOrCreateTagElement("display").put("Lore", loreTag);
        }

        // Hide default attribute tooltip for combatente sword item
        if ("combatente".equals(professionKey)) {
            stack.getOrCreateTag().putInt("HideFlags", 2); // hide attribute modifiers lines
        }

        this.container.setItem(slot, stack);
    }

    @Override
    public boolean stillValid(@NotNull Player pPlayer) {
        return true;
    }

    @Override
    public @NotNull ItemStack quickMoveStack(Player pPlayer, int pIndex) {
        return ItemStack.EMPTY;
    }

    // ---------------------------------------------------------------------
    // Slot that forbids placing/taking items
    // ---------------------------------------------------------------------
    private static class ReadOnlySlot extends Slot {
        public ReadOnlySlot(Container container, int index, int xPosition, int yPosition) {
            super(container, index, xPosition, yPosition);
        }

        @Override
        public boolean mayPlace(@NotNull ItemStack stack) {
            return false;
        }

        @Override
        public boolean mayPickup(@NotNull Player player) {
            return false;
        }
    }
} 