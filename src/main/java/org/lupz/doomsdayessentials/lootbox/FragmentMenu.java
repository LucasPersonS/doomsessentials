package org.lupz.doomsdayessentials.lootbox;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.NotNull;
import org.lupz.doomsdayessentials.item.ModItems;
import org.lupz.doomsdayessentials.lootbox.farming.LootboxFarmingManager;

import java.util.ArrayList;
import java.util.List;

/**
 * Menu para troca de fragmentos por lootboxes
 * Segue o mesmo padrão do KitMenu
 */
public class FragmentMenu extends AbstractContainerMenu {
    private final Container container = new SimpleContainer(54);
    private final Player player;

    // Slots das lootboxes (linha 2, centralizado)
    private static final int SLOT_INCOMUM = 20; // row 2, col 2
    private static final int SLOT_RARA = 22; // row 2, col 4
    private static final int SLOT_EPICA = 24; // row 2, col 6
    private static final int SLOT_LENDARIA = 31; // row 3, col 4 (center)

    public FragmentMenu(int windowId, Inventory inv) {
        super(LootboxMenus.FRAGMENT_MENU.get(), windowId);
        this.player = inv.player;
        build();

        // Menu grid (top 6x9)
        for (int row = 0; row < 6; ++row) {
            for (int col = 0; col < 9; ++col) {
                this.addSlot(new ReadOnlySlot(container, col + row * 9, 8 + col * 18, 18 + row * 18));
            }
        }

        // Player inventory slots
        int yBase = 103 + ((6 - 4) * 18); // 139
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                this.addSlot(new Slot(inv, col + row * 9 + 9, 8 + col * 18, yBase + row * 18));
            }
        }
        for (int col = 0; col < 9; ++col) {
            this.addSlot(new Slot(inv, col, 8 + col * 18, 161 + ((6 - 4) * 18))); // 197
        }
    }

    private void build() {
        // Clear
        for (int i = 0; i < 54; i++)
            container.setItem(i, ItemStack.EMPTY);

        // Title
        ItemStack title = new ItemStack(Items.PAPER);
        title.setHoverName(Component.literal("§6§lTroca de Fragmentos"));
        addLore(title, List.of(Component.literal("§7Clique nas lootboxes para trocar fragmentos")));
        container.setItem(4, title);

        // Lootbox buttons
        buildLootboxButton(SLOT_INCOMUM, LootboxManager.R_INCOMUM, 100);
        buildLootboxButton(SLOT_RARA, LootboxManager.R_RARA, 150);
        buildLootboxButton(SLOT_EPICA, LootboxManager.R_EPICA, 200);
        buildLootboxButton(SLOT_LENDARIA, LootboxManager.R_LENDARIA, 300);

        // Fill background
        ItemStack filler = new ItemStack(Items.GRAY_STAINED_GLASS_PANE);
        filler.setHoverName(Component.literal(""));
        for (int i = 0; i < 54; i++) {
            if (container.getItem(i).isEmpty())
                container.setItem(i, filler.copy());
        }
    }

    private void buildLootboxButton(int slot, String rarity, int needed) {
        ItemStack lootbox = switch (rarity) {
            case LootboxManager.R_INCOMUM -> new ItemStack(ModItems.LOOTBOX_INCOMUM.get());
            case LootboxManager.R_RARA -> new ItemStack(ModItems.LOOTBOX_RARA.get());
            case LootboxManager.R_EPICA -> new ItemStack(ModItems.LOOTBOX_EPICA.get());
            case LootboxManager.R_LENDARIA -> new ItemStack(ModItems.LOOTBOX_LENDARIA.get());
            default -> new ItemStack(Items.CHEST);
        };

        String colorCode = switch (rarity) {
            case "incomum" -> "§a";
            case "rara" -> "§9";
            case "epica" -> "§5";
            case "lendaria" -> "§6";
            default -> "§7";
        };

        lootbox.setHoverName(Component.literal(colorCode + "§lLootbox " + capitalize(rarity)));

        List<Component> lore = new ArrayList<>();

        if (player instanceof ServerPlayer sp) {
            int count = LootboxFarmingManager.countFragmentsInInventory(sp, rarity);
            boolean canExchange = count >= needed;

            lore.add(Component.literal("§7Fragmentos necessários: §e" + needed));
            lore.add(Component.literal("§7Você tem: §f" + count + " §7fragmentos"));
            lore.add(Component.literal(""));

            if (canExchange) {
                lore.add(Component.literal("§a✓ Disponível"));
                lore.add(Component.literal("§eClique para trocar!"));
            } else {
                lore.add(Component.literal("§c✗ Insuficiente"));
                lore.add(Component.literal("§7Faltam: §c" + (needed - count) + " §7fragmentos"));
            }
        }

        addLore(lootbox, lore);
        container.setItem(slot, lootbox);
    }

    private void addLore(ItemStack stack, List<Component> lore) {
        net.minecraft.nbt.ListTag tag = new net.minecraft.nbt.ListTag();
        for (Component c : lore) {
            tag.add(net.minecraft.nbt.StringTag.valueOf(Component.Serializer.toJson(c)));
        }
        stack.getOrCreateTagElement("display").put("Lore", tag);
    }

    private String capitalize(String str) {
        if (str == null || str.isEmpty())
            return str;
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }

    @Override
    public void clicked(int slotId, int dragType, @NotNull ClickType clickType, @NotNull Player clickPlayer) {
        if (!(clickPlayer instanceof ServerPlayer sp)) {
            super.clicked(slotId, dragType, clickType, clickPlayer);
            return;
        }

        // Handle lootbox clicks
        if (slotId == SLOT_INCOMUM) {
            exchangeLootbox(sp, LootboxManager.R_INCOMUM, 100);
            return;
        }
        if (slotId == SLOT_RARA) {
            exchangeLootbox(sp, LootboxManager.R_RARA, 150);
            return;
        }
        if (slotId == SLOT_EPICA) {
            exchangeLootbox(sp, LootboxManager.R_EPICA, 200);
            return;
        }
        if (slotId == SLOT_LENDARIA) {
            exchangeLootbox(sp, LootboxManager.R_LENDARIA, 300);
            return;
        }

        super.clicked(slotId, dragType, clickType, clickPlayer);
    }

    private void exchangeLootbox(ServerPlayer sp, String rarity, int needed) {
        int count = LootboxFarmingManager.countFragmentsInInventory(sp, rarity);

        if (count < needed) {
            sp.sendSystemMessage(Component.literal(
                    "§cVocê não tem fragmentos suficientes! Precisa de §e" + needed + " §cfragmentos."));
            return;
        }

        // Remover fragmentos
        boolean removed = LootboxFarmingManager.removeFragmentsFromInventory(sp, rarity, needed);
        if (!removed) {
            sp.sendSystemMessage(Component.literal("§cErro ao remover fragmentos!"));
            return;
        }

        // Dar lootbox
        ItemStack lootbox = switch (rarity) {
            case LootboxManager.R_INCOMUM -> new ItemStack(ModItems.LOOTBOX_INCOMUM.get());
            case LootboxManager.R_RARA -> new ItemStack(ModItems.LOOTBOX_RARA.get());
            case LootboxManager.R_EPICA -> new ItemStack(ModItems.LOOTBOX_EPICA.get());
            case LootboxManager.R_LENDARIA -> new ItemStack(ModItems.LOOTBOX_LENDARIA.get());
            default -> ItemStack.EMPTY;
        };

        if (!lootbox.isEmpty()) {
            sp.getInventory().placeItemBackInInventory(lootbox);

            String color = switch (rarity) {
                case "incomum" -> "§a";
                case "rara" -> "§9";
                case "epica" -> "§5";
                case "lendaria" -> "§6";
                default -> "§7";
            };

            sp.sendSystemMessage(Component.literal(
                    "§a§l✔ §7Você trocou §e" + needed + " §7fragmentos por " +
                            color + "§l1 Lootbox " + capitalize(rarity) + "§7!"));

            // Tocar som de sucesso
            sp.playSound(net.minecraft.sounds.SoundEvents.PLAYER_LEVELUP, 0.5f, 1.5f);

            // Fechar e reabrir menu para atualizar contadores
            sp.closeContainer();
            sp.openMenu(new net.minecraft.world.MenuProvider() {
                @Override
                public Component getDisplayName() {
                    return Component.literal("Troca de Fragmentos");
                }

                @Override
                public net.minecraft.world.inventory.AbstractContainerMenu createMenu(int windowId,
                        Inventory playerInv, Player p) {
                    return new FragmentMenu(windowId, playerInv);
                }
            });
        }
    }

    @Override
    public boolean stillValid(@NotNull Player p) {
        return true;
    }

    @Override
    public @NotNull ItemStack quickMoveStack(@NotNull Player p, int idx) {
        return ItemStack.EMPTY;
    }

    private static class ReadOnlySlot extends Slot {
        public ReadOnlySlot(Container cont, int idx, int x, int y) {
            super(cont, idx, x, y);
        }

        @Override
        public boolean mayPlace(@NotNull ItemStack s) {
            return false;
        }

        @Override
        public boolean mayPickup(@NotNull Player p) {
            return false;
        }
    }
}
