package org.lupz.doomsdayessentials.kit.menu;

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
import org.lupz.doomsdayessentials.command.VipCommand;
import org.lupz.doomsdayessentials.kit.Kit;
import org.lupz.doomsdayessentials.kit.KitCooldownManager;
import org.lupz.doomsdayessentials.kit.KitManager;

import java.util.ArrayList;
import java.util.List;

/**
 * Main kit selection menu with tabs for different cooldown types.
 * Inspired by GuildMainMenu design.
 */
public class KitMenu extends AbstractContainerMenu {
    private final Container container = new SimpleContainer(54);
    private final Player player;
    private Kit.CooldownType currentCooldownType;

    // Tab slots (top row)
    private static final int SLOT_TAB_DAILY = 1;
    private static final int SLOT_TAB_WEEKLY = 4;
    private static final int SLOT_TAB_MONTHLY = 7;

    // Kit tier slots (centered layout)
    private static final int SLOT_KIT_FREE = 20; // row 2, col 2
    private static final int SLOT_KIT_SOBREVIVENTE = 22; // row 2, col 4
    private static final int SLOT_KIT_INFECTADO = 24; // row 2, col 6
    private static final int SLOT_KIT_DISSOLUTO = 31; // row 3, col 4 (center)

    public KitMenu(int windowId, Inventory inv, Kit.CooldownType cooldownType) {
        super(KitMenus.KIT_MENU.get(), windowId);
        this.player = inv.player;
        this.currentCooldownType = cooldownType;
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
        title.setHoverName(Component.literal("§6§lKits VIP"));
        addLore(title, List.of(Component.literal("§7Selecione um kit para reivindicar")));
        container.setItem(4, title);

        // Tab buttons
        buildTab(SLOT_TAB_DAILY, Kit.CooldownType.DAILY);
        buildTab(SLOT_TAB_WEEKLY, Kit.CooldownType.WEEKLY);
        buildTab(SLOT_TAB_MONTHLY, Kit.CooldownType.MONTHLY);

        // Kit tier buttons
        if (player instanceof ServerPlayer sp) {
            String vipTier = VipCommand.getTier(sp);
            buildKitButton(SLOT_KIT_FREE, KitManager.TIER_FREE, true);
            buildKitButton(SLOT_KIT_SOBREVIVENTE, KitManager.TIER_SOBREVIVENTE,
                    vipTier != null && (vipTier.equals("sobrevivente") || vipTier.equals("infectado")
                            || vipTier.equals("dissoluto")));
            buildKitButton(SLOT_KIT_INFECTADO, KitManager.TIER_INFECTADO,
                    vipTier != null && (vipTier.equals("infectado") || vipTier.equals("dissoluto")));
            buildKitButton(SLOT_KIT_DISSOLUTO, KitManager.TIER_DISSOLUTO,
                    vipTier != null && vipTier.equals("dissoluto"));
        }

        // Fill background
        ItemStack filler = new ItemStack(Items.GRAY_STAINED_GLASS_PANE);
        filler.setHoverName(Component.literal(""));
        for (int i = 0; i < 54; i++) {
            if (container.getItem(i).isEmpty())
                container.setItem(i, filler.copy());
        }
    }

    private void buildTab(int slot, Kit.CooldownType cooldownType) {
        boolean isActive = cooldownType == currentCooldownType;
        ItemStack tab = new ItemStack(isActive ? Items.LIME_STAINED_GLASS : Items.RED_STAINED_GLASS);

        String name = switch (cooldownType) {
            case DAILY -> "§eDaily";
            case WEEKLY -> "§bWeekly";
            case MONTHLY -> "§dMonthly";
        };

        tab.setHoverName(Component.literal(name + (isActive ? " §a✓" : "")));
        List<Component> lore = new ArrayList<>();
        lore.add(Component.literal(isActive ? "§aTab ativa" : "§7Clique para mudar"));
        addLore(tab, lore);
        container.setItem(slot, tab);
    }

    private void buildKitButton(int slot, String tier, boolean hasAccess) {
        Kit kit = KitManager.getKit(tier, currentCooldownType);

        if (kit == null) {
            // No kit configured for this tier/cooldown
            ItemStack noKit = new ItemStack(Items.BARRIER);
            noKit.setHoverName(Component.literal("§c" + capitalize(tier)));
            addLore(noKit, List.of(Component.literal("§7Kit não configurado")));
            container.setItem(slot, noKit);
            return;
        }

        if (!hasAccess) {
            // Player doesn't have access to this tier
            ItemStack locked = new ItemStack(Items.IRON_BARS);
            locked.setHoverName(Component.literal("§c" + capitalize(tier) + " §7(Bloqueado)"));
            addLore(locked, List.of(
                    Component.literal("§7Requer VIP: §e" + capitalize(tier)),
                    Component.literal("§cVocê não tem acesso a este kit")));
            container.setItem(slot, locked);
            return;
        }

        // Player has access - show kit with cooldown status
        if (player instanceof ServerPlayer sp) {
            boolean canClaim = KitCooldownManager.canClaim(sp, tier, currentCooldownType);
            long remainingMs = KitCooldownManager.getRemainingCooldown(sp, tier, currentCooldownType);
            String cooldownStr = KitCooldownManager.formatCooldown(remainingMs);

            ItemStack kitIcon = canClaim ? new ItemStack(Items.CHEST) : new ItemStack(Items.ENDER_CHEST);
            kitIcon.setHoverName(Component.literal("§6Kit " + capitalize(tier)));

            List<Component> lore = new ArrayList<>();
            lore.add(Component.literal("§7Cooldown: §e" + currentCooldownType.getId()));
            lore.add(Component.literal(""));

            // Show first 3 items as preview
            List<ItemStack> items = kit.getItems();
            if (!items.isEmpty()) {
                lore.add(Component.literal("§7Itens:"));
                for (int i = 0; i < Math.min(3, items.size()); i++) {
                    ItemStack item = items.get(i);
                    lore.add(Component.literal("  §f" + item.getCount() + "x §7" + item.getHoverName().getString()));
                }
                if (items.size() > 3) {
                    lore.add(Component.literal("  §7... e mais " + (items.size() - 3) + " itens"));
                }
            }

            lore.add(Component.literal(""));
            if (canClaim) {
                lore.add(Component.literal("§a✓ Disponível"));
                lore.add(Component.literal("§eClique para reivindicar!"));
            } else {
                lore.add(Component.literal("§c✗ Em cooldown"));
                lore.add(Component.literal("§7Tempo restante: §f" + cooldownStr));
            }

            addLore(kitIcon, lore);
            container.setItem(slot, kitIcon);
        }
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

        // Handle tab clicks
        if (slotId == SLOT_TAB_DAILY) {
            switchTab(sp, Kit.CooldownType.DAILY);
            return;
        }
        if (slotId == SLOT_TAB_WEEKLY) {
            switchTab(sp, Kit.CooldownType.WEEKLY);
            return;
        }
        if (slotId == SLOT_TAB_MONTHLY) {
            switchTab(sp, Kit.CooldownType.MONTHLY);
            return;
        }

        // Handle kit claims
        if (slotId == SLOT_KIT_FREE) {
            claimKit(sp, KitManager.TIER_FREE);
            return;
        }
        if (slotId == SLOT_KIT_SOBREVIVENTE) {
            claimKit(sp, KitManager.TIER_SOBREVIVENTE);
            return;
        }
        if (slotId == SLOT_KIT_INFECTADO) {
            claimKit(sp, KitManager.TIER_INFECTADO);
            return;
        }
        if (slotId == SLOT_KIT_DISSOLUTO) {
            claimKit(sp, KitManager.TIER_DISSOLUTO);
            return;
        }

        super.clicked(slotId, dragType, clickType, clickPlayer);
    }

    private void switchTab(ServerPlayer sp, Kit.CooldownType newCooldownType) {
        if (newCooldownType == currentCooldownType)
            return;

        sp.closeContainer();
        KitMenuProvider.open(sp, newCooldownType);
    }

    private void claimKit(ServerPlayer sp, String tier) {
        // Check VIP access
        String vipTier = VipCommand.getTier(sp);
        if (!hasVipAccess(tier, vipTier)) {
            sp.sendSystemMessage(Component.literal("§cVocê não tem acesso a este kit!"));
            return;
        }

        // Check if kit exists
        Kit kit = KitManager.getKit(tier, currentCooldownType);
        if (kit == null) {
            sp.sendSystemMessage(Component.literal("§cEste kit não está configurado."));
            return;
        }

        // Check cooldown
        if (!KitCooldownManager.canClaim(sp, tier, currentCooldownType)) {
            long remainingMs = KitCooldownManager.getRemainingCooldown(sp, tier, currentCooldownType);
            String cooldownStr = KitCooldownManager.formatCooldown(remainingMs);
            sp.sendSystemMessage(Component
                    .literal("§cVocê precisa esperar §e" + cooldownStr + " §cpara reivindicar este kit novamente."));
            return;
        }

        // Give items
        List<ItemStack> items = kit.getItems();
        int given = 0;
        int dropped = 0;

        for (ItemStack stack : items) {
            ItemStack copy = stack.copy();
            if (sp.getInventory().add(copy)) {
                given++;
            } else {
                sp.drop(copy, false);
                dropped++;
            }
        }

        // Update cooldown
        KitCooldownManager.setClaimed(sp, tier, currentCooldownType);

        // Send feedback
        sp.sendSystemMessage(Component.literal("§aVocê reivindicou o kit §e" + capitalize(tier) + "§a!"));
        sp.sendSystemMessage(Component
                .literal("§7Itens recebidos: §f" + given + (dropped > 0 ? " §7(§e" + dropped + " §7dropados)" : "")));

        // Close menu and reopen to refresh cooldown display
        sp.closeContainer();
        KitMenuProvider.open(sp, currentCooldownType);
    }

    private boolean hasVipAccess(String kitTier, String vipTier) {
        if (kitTier.equals(KitManager.TIER_FREE))
            return true;
        if (vipTier == null)
            return false;

        return switch (kitTier) {
            case "sobrevivente" ->
                vipTier.equals("sobrevivente") || vipTier.equals("infectado") || vipTier.equals("dissoluto");
            case "infectado" -> vipTier.equals("infectado") || vipTier.equals("dissoluto");
            case "dissoluto" -> vipTier.equals("dissoluto");
            default -> false;
        };
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
