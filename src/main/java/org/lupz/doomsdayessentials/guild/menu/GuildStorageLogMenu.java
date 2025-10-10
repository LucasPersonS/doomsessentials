package org.lupz.doomsdayessentials.guild.menu;

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
import org.jetbrains.annotations.NotNull;
import org.lupz.doomsdayessentials.guild.GuildsManager;
import org.lupz.doomsdayessentials.professions.menu.ProfessionMenuTypes;

public class GuildStorageLogMenu extends AbstractContainerMenu {
    private final Container cont = new SimpleContainer(54);
    private final Player player;
    private final String guildName;

    // Pagination and filter controls
    private int pageLog = 0; // 0-based
    private enum ActionFilter { ALL, ADD, REMOVE, MOVE }
    private ActionFilter filter = ActionFilter.ALL;

    private static final int SLOT_BACK = 45;
    private static final int SLOT_FILTER = 46;
    private static final int SLOT_NEXT = 53;

    public GuildStorageLogMenu(int windowId, Inventory inv, String guildName) {
        super(ProfessionMenuTypes.GUILD_STORAGE_LOG_MENU.get(), windowId);
        this.player = inv.player;
        this.guildName = guildName;
        rebuild();
        for (int row = 0; row < 6; ++row) {
            for (int col = 0; col < 9; ++col) {
                this.addSlot(new Slot(cont, col + row * 9, 8 + col * 18, 18 + row * 18) {
                    @Override public boolean mayPlace(@NotNull ItemStack s) { return false; }
                    @Override public boolean mayPickup(@NotNull Player p) { return false; }
                });
            }
        }
        int invY = 198; int invTopY = invY - 58; // align to large chest layout
        for (int row = 0; row < 3; ++row) for (int col = 0; col < 9; ++col) this.addSlot(new Slot(inv, col + row * 9 + 9, 8 + col * 18, invTopY + row * 18));
        for (int col = 0; col < 9; ++col) this.addSlot(new Slot(inv, col, 8 + col * 18, invY));
    }

    private void rebuild() {
        for (int i = 0; i < 54; i++) cont.setItem(i, ItemStack.EMPTY);
        if (!(player instanceof ServerPlayer sp)) return;
        GuildsManager gm = GuildsManager.get(sp.serverLevel());

        java.util.List<GuildsManager.StorageLogEntry> all = gm.getStorageLogs(guildName);
        // Filter
        java.util.List<GuildsManager.StorageLogEntry> filtered = new java.util.ArrayList<>();
        for (var le : all) {
            if (filter == ActionFilter.ALL ||
                (filter == ActionFilter.ADD && "add".equalsIgnoreCase(le.action)) ||
                (filter == ActionFilter.REMOVE && "remove".equalsIgnoreCase(le.action)) ||
                (filter == ActionFilter.MOVE && "move".equalsIgnoreCase(le.action))) {
                filtered.add(le);
            }
        }
        // Newest first
        java.util.List<GuildsManager.StorageLogEntry> reversed = new java.util.ArrayList<>(filtered);
        java.util.Collections.reverse(reversed);

        int perPage = 54;
        int start = pageLog * perPage;
        int end = Math.min(start + perPage, reversed.size());

        java.time.ZoneId zone = gm.getLogZoneId();
        int idx = 0;
        for (int i = start; i < end && idx < 54; i++) {
            var le = reversed.get(i);
            ItemStack paper = new ItemStack(net.minecraft.world.item.Items.PAPER);
            java.time.Instant inst = java.time.Instant.ofEpochMilli(le.ts);
            java.time.ZonedDateTime z = java.time.ZonedDateTime.ofInstant(inst, zone);
            String ts = String.format("%02d/%02d %02d:%02d", z.getDayOfMonth(), z.getMonthValue(), z.getHour(), z.getMinute());
            String actor = (le.actorName != null && !le.actorName.isEmpty()) ? le.actorName : (le.actor == null ? "Unknown" : le.actor.toString().substring(0, 8));
            paper.setHoverName(Component.literal("§7" + ts + " §8• §b" + actor + " §8• §e" + le.action + " §7" + le.itemId + " §fx" + le.amount + " §8(p" + le.page + "/s" + le.slot + ")"));
            cont.setItem(idx++, paper);
        }

        // Controls
        ItemStack back = new ItemStack(net.minecraft.world.item.Items.ARROW);
        back.setHoverName(Component.literal("§ePágina Anterior"));
        cont.setItem(SLOT_BACK, back);

        ItemStack next = new ItemStack(net.minecraft.world.item.Items.ARROW);
        next.setHoverName(Component.literal("§ePróxima Página"));
        cont.setItem(SLOT_NEXT, next);

        ItemStack filterBtn = new ItemStack(net.minecraft.world.item.Items.HOPPER);
        String fName = switch (filter) { case ALL -> "Tudo"; case ADD -> "Adicionar"; case REMOVE -> "Remover"; case MOVE -> "Mover"; };
        filterBtn.setHoverName(Component.literal("§bFiltro: §f" + fName));
        cont.setItem(SLOT_FILTER, filterBtn);
    }

    @Override public boolean stillValid(@NotNull Player p) { return true; }
    @Override public @NotNull ItemStack quickMoveStack(Player p, int idx) { return ItemStack.EMPTY; }

    @Override
    public void clicked(int slotId, int dragType, @NotNull ClickType clickType, @NotNull Player clickPlayer) {
        if (!(clickPlayer instanceof ServerPlayer)) { super.clicked(slotId, dragType, clickType, clickPlayer); return; }
        if (slotId == SLOT_BACK) { if (pageLog > 0) pageLog--; rebuild(); broadcastChanges(); return; }
        if (slotId == SLOT_NEXT) { pageLog++; rebuild(); broadcastChanges(); return; }
        if (slotId == SLOT_FILTER) {
            filter = switch (filter) {
                case ALL -> ActionFilter.ADD;
                case ADD -> ActionFilter.REMOVE;
                case REMOVE -> ActionFilter.MOVE;
                case MOVE -> ActionFilter.ALL;
            };
            pageLog = 0;
            rebuild();
            broadcastChanges();
            return;
        }
        super.clicked(slotId, dragType, clickType, clickPlayer);
    }
}


