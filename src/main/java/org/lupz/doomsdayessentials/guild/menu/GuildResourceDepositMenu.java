package org.lupz.doomsdayessentials.guild.menu;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.lupz.doomsdayessentials.guild.Guild;
import org.lupz.doomsdayessentials.guild.GuildsManager;
import org.lupz.doomsdayessentials.item.ModItems;
import org.lupz.doomsdayessentials.professions.menu.ProfessionMenuTypes;

import java.util.HashMap;
import java.util.Map;

/**
 * Deposit UI for Guild Resources. Players can place only accepted resources here.
 * When the menu closes, items are converted into the guild's shared resource balances.
 */
public class GuildResourceDepositMenu extends AbstractContainerMenu {
    private final Container cont = new SimpleContainer(54);
    private final Player player;
    private Guild guild;

    private static final int SLOT_BACK = 45;      // bottom-left
    private static final int SLOT_INFO = 49;      // bottom-center

    public GuildResourceDepositMenu(int windowId, Inventory inv) {
        super(ProfessionMenuTypes.GUILD_RESOURCE_DEPOSIT_MENU.get(), windowId);
        this.player = inv.player;
        if (player instanceof ServerPlayer sp) {
            ServerLevel lvl = sp.serverLevel();
            this.guild = GuildsManager.get(lvl).getGuildByMember(sp.getUUID());
        }
        rebuildInfo();

        // Grid 6x9
        for (int row = 0; row < 6; ++row) {
            for (int col = 0; col < 9; ++col) {
                final int idx = col + row * 9;
                this.addSlot(new Slot(cont, idx, 8 + col * 18, 18 + row * 18) {
                    @Override public boolean mayPlace(@NotNull ItemStack s) {
                        // Only allow deposit into non-control slots and only accepted items
                        if (idx == SLOT_BACK || idx == SLOT_INFO) return false;
                        return isAccepted(s.getItem());
                    }
                    @Override public boolean mayPickup(@NotNull Player p) { return idx != SLOT_BACK && idx != SLOT_INFO; }
                });
            }
        }
        // Player inventory (like large chest)
        int invY = 198;
        int invTopY = invY - 58; // 140
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                this.addSlot(new Slot(inv, col + row * 9 + 9, 8 + col * 18, invTopY + row * 18));
            }
        }
        for (int col = 0; col < 9; ++col) this.addSlot(new Slot(inv, col, 8 + col * 18, invY));
    }

    private void rebuildInfo() {
        // Title
        ItemStack title = new ItemStack(net.minecraft.world.item.Items.PAPER);
        title.setHoverName(Component.literal("§6§lDepósito de Recursos"));
        cont.setItem(4, title);

        // Back button
        ItemStack back = new ItemStack(org.lupz.doomsdayessentials.item.ModItems.GUI_BACK.get());
        back.setHoverName(Component.literal("§eVoltar"));
        addLore(back, java.util.List.of(Component.literal("§7Voltar ao menu da organização")));
        cont.setItem(SLOT_BACK, back);

        // Info with accepted + balances
        ItemStack info = new ItemStack(net.minecraft.world.item.Items.BOOK);
        info.setHoverName(Component.literal("§bRecursos Aceitos"));
        java.util.List<Component> lore = new java.util.ArrayList<>();
        lore.add(Component.literal("§7Você pode depositar:"));
        lore.add(Component.literal("§f- Sucata (scrapmetal)"));
        lore.add(Component.literal("§f- Fragmentos (metal_fragments)"));
        lore.add(Component.literal("§f- Lâmina (metalblade)"));
        lore.add(Component.literal("§f- Chapas (sheetmetal)"));
        if (player instanceof ServerPlayer sp && guild != null) {
            org.lupz.doomsdayessentials.guild.GuildResourceBank bank = org.lupz.doomsdayessentials.guild.GuildResourceBank.get(sp.serverLevel());
            lore.add(Component.literal(""));
            lore.add(Component.literal("§7Saldo da Guilda:"));
            lore.add(Component.literal("§fSucata: §e" + bank.get(guild.getName(), key(ModItems.SCRAPMETAL.get()))));
            lore.add(Component.literal("§fFragmentos: §e" + bank.get(guild.getName(), key(ModItems.METAL_FRAGMENTS.get()))));
            lore.add(Component.literal("§fLâminas: §e" + bank.get(guild.getName(), key(ModItems.METALBLADE.get()))));
            lore.add(Component.literal("§fChapas: §e" + bank.get(guild.getName(), key(ModItems.SHEETMETAL.get()))));
        }
        addLore(info, lore);
        cont.setItem(SLOT_INFO, info);

        // Do not fill background: keep slots empty for deposit
    }

    private static String key(Item item) {
        return net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(item).toString();
    }

    private boolean isAccepted(Item item) {
        return item == ModItems.SCRAPMETAL.get() ||
               item == ModItems.METAL_FRAGMENTS.get() ||
               item == ModItems.METALBLADE.get() ||
               item == ModItems.SHEETMETAL.get();
    }

    private void addLore(ItemStack stack, java.util.List<Component> lore) {
        net.minecraft.nbt.ListTag tag = new net.minecraft.nbt.ListTag();
        for (Component c : lore) tag.add(net.minecraft.nbt.StringTag.valueOf(Component.Serializer.toJson(c)));
        stack.getOrCreateTagElement("display").put("Lore", tag);
    }

    @Override
    public void clicked(int slotId, int dragType, @NotNull ClickType clickType, @NotNull Player clickPlayer) {
        if (!(clickPlayer instanceof ServerPlayer sp)) { super.clicked(slotId, dragType, clickType, clickPlayer); return; }
        if (slotId == SLOT_BACK && clickType == ClickType.PICKUP) {
            sp.openMenu(new net.minecraft.world.SimpleMenuProvider((id, inv, p) -> new GuildMainMenu(id, inv), Component.literal("Organização")));
            return;
        }
        super.clicked(slotId, dragType, clickType, clickPlayer);
    }

    @Override
    public void removed(Player p) {
        super.removed(p);
        if (!(p instanceof ServerPlayer sp) || guild == null) return;
        org.lupz.doomsdayessentials.guild.GuildResourceBank bank = org.lupz.doomsdayessentials.guild.GuildResourceBank.get(sp.serverLevel());
        Map<String, Integer> added = new HashMap<>();
        for (int i = 0; i < cont.getContainerSize(); i++) {
            if (i == SLOT_BACK || i == SLOT_INFO) continue;
            ItemStack s = cont.getItem(i);
            if (s.isEmpty()) continue;
            if (isAccepted(s.getItem())) {
                String id = key(s.getItem());
                int count = s.getCount();
                bank.add(guild.getName(), id, count);
                added.merge(id, count, Integer::sum);
                cont.setItem(i, ItemStack.EMPTY);
            } else {
                // return forbidden items to player
                if (!sp.getInventory().add(s.copy())) sp.drop(s.copy(), false);
                cont.setItem(i, ItemStack.EMPTY);
            }
        }
        if (!added.isEmpty()) {
            int scrap = added.getOrDefault(key(ModItems.SCRAPMETAL.get()), 0);
            int frags = added.getOrDefault(key(ModItems.METAL_FRAGMENTS.get()), 0);
            int blades = added.getOrDefault(key(ModItems.METALBLADE.get()), 0);
            int sheets = added.getOrDefault(key(ModItems.SHEETMETAL.get()), 0);
            sp.sendSystemMessage(Component.literal(String.format("§aDepositado: sucata=%d, fragmentos=%d, lâminas=%d, chapas=%d.", scrap, frags, blades, sheets)));
        }
    }

    @Override
    public boolean stillValid(@NotNull Player p) { return true; }

    @Override
    public @NotNull ItemStack quickMoveStack(Player p, int index) {
        ItemStack empty = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot == null || !slot.hasItem()) return empty;
        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();

        int playerStart = 54;
        int playerEndExclusive = this.slots.size();

        if (index < 54) {
            // deposit -> player
            if (index == SLOT_BACK || index == SLOT_INFO) return ItemStack.EMPTY;
            if (!this.moveItemStackTo(stack, playerStart, playerEndExclusive, true)) return ItemStack.EMPTY;
        } else {
            // player -> deposit (only accepted)
            if (!isAccepted(stack.getItem())) return ItemStack.EMPTY;
            // Avoid control slots
            boolean moved = this.moveItemStackTo(stack, 0, SLOT_BACK, false);
            if (!moved) moved = this.moveItemStackTo(stack, SLOT_BACK + 1, SLOT_INFO, false);
            if (!moved) moved = this.moveItemStackTo(stack, SLOT_INFO + 1, 54, false);
            if (!moved) return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) slot.set(ItemStack.EMPTY); else slot.setChanged();
        return copy;
    }
}
