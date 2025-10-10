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
import org.lupz.doomsdayessentials.injury.InjuryItems;
import org.lupz.doomsdayessentials.professions.ProfissaoManager;
import org.lupz.doomsdayessentials.professions.items.ProfessionItems;

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

            List<Component> lore = new ArrayList<>();
            lore.add(Component.literal("§7Abandone sua profissão clicando aqui."));
            addLoreToItemStack(abandon, lore);

            this.container.setItem(13, abandon);
            return;
        }

        createProfessionItem(11, InjuryItems.MEDIC_KIT.get(), "medico");
        createProfessionItem(13, Items.IRON_SWORD, "combatente");
        createProfessionItem(15, Items.COMPASS, "rastreador");
        		createProfessionItem(17, ProfessionItems.ENGINEER_HAMMER.get(), "engenheiro");
		createProfessionItem(19, net.minecraft.world.item.Items.CROSSBOW, "cacador");
    }

    private void createProfessionItem(int slot, net.minecraft.world.item.Item vanillaItem, String professionKey) {
        ItemStack stack = new ItemStack(vanillaItem);
        // Colored title based on profession via style
        Component baseName = Component.translatable("profession." + professionKey + ".gui.name");
        int color = switch (professionKey) {
            case "medico" -> 0x78C8FF;
            case "engenheiro" -> 0xE6B866;
            case "rastreador" -> 0xA7FFAE;
            case "cacador" -> 0xFF8A65;
            case "combatente" -> 0xFF5555;
            default -> 0xFFFFFF;
        };
        stack.setHoverName(baseName.copy().withStyle(style -> style.withColor(color)));

        java.util.List<Component> lore = new java.util.ArrayList<>();
        for (int i = 1; i <= 6; i++) {
            String key = "profession." + professionKey + ".gui.lore" + i;
            Component line = Component.translatable(key);
            String lower = line.getString().toLowerCase(java.util.Locale.ROOT);
            if (lower.startsWith("passiva")) {
                lore.add(line.copy().withStyle(s -> s.withColor(0xE6B866)));
            } else if (lower.startsWith("skill")) {
                lore.add(line.copy().withStyle(s -> s.withColor(0x78C8FF)));
            } else if (lower.startsWith("utilidade")) {
                lore.add(line.copy().withStyle(s -> s.withColor(0xA7FFAE)));
            } else {
                lore.add(line.copy().withStyle(s -> s.withColor(0xCCCCCC)));
            }
        }

        if (!lore.isEmpty()) {
            addLoreToItemStack(stack, lore);
        }

        if ("combatente".equals(professionKey)) {
            stack.getOrCreateTag().putInt("HideFlags", 2);
        }

        this.container.setItem(slot, stack);
    }

    private void addLoreToItemStack(ItemStack stack, List<Component> lore) {
        ListTag loreTag = new ListTag();
        for (Component component : lore) {
            loreTag.add(StringTag.valueOf(Component.Serializer.toJson(component)));
        }
        stack.getOrCreateTagElement("display").put("Lore", loreTag);
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