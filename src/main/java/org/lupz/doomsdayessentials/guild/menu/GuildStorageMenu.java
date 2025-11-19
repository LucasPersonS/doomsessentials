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
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.lupz.doomsdayessentials.guild.Guild;
import org.lupz.doomsdayessentials.guild.GuildsManager;
import org.lupz.doomsdayessentials.professions.menu.ProfessionMenuTypes;

/**
 * Global guild storage with pagination. 6x9 grid for storage; bottom rows show
 * player inventory.
 */
public class GuildStorageMenu extends AbstractContainerMenu {
    private final Player player;
    private Guild guild;
    private int page = 0; // 0-based
    private final Container view = new SimpleContainer(54) {
        @Override
        public int getMaxStackSize() {
            return 64;
        }
    };
    private Filter filter = Filter.ALL;
    /**
     * Tracks which view slots mirror real storage this rebuild (true) vs
     * filtered/control (false).
     */
    private final boolean[] mirrored = new boolean[54];
    private final int[] viewMap = new int[54];
    private boolean allowControlAction = false;
    private static final int SLOT_BACK = 45;
    private static final int SLOT_NEXT = 53;
    private static final int SLOT_FILTER = 46; // toggle
    private static final int SLOT_SORT = 47; // organize button
    private static final int SLOT_LOG = 52; // open logs
    private static final int SLOT_UPGRADE = 50; // lower right book

    public enum Filter {
        ALL, BLOCKS, ITEMS, TOOLS, WEAPONS, ARMOR, FOOD, POTIONS, ENCHANTED
    }

    private String searchText = "";

    // Container data for syncing to client: [0] = currentPage, [1] = storageLevel
    private final ContainerData data;
    private int[] clientCounts = new int[54];
    private static final java.util.Map<String, java.util.Set<GuildStorageMenu>> OPEN_MENUS = new java.util.concurrent.ConcurrentHashMap<>();

    public GuildStorageMenu(int windowId, Inventory inv, int page) {
        super(ProfessionMenuTypes.GUILD_STORAGE_MENU.get(), windowId);
        this.player = inv.player;
        this.page = Math.max(0, page);

        // Initialize container data for syncing
        if (player instanceof ServerPlayer sp) {
            ServerLevel lvl = sp.serverLevel();
            this.guild = GuildsManager.get(lvl).getGuildByMember(sp.getUUID());
            // Ensure we have latest guild data on menu open
            if (this.guild != null) {
                GuildsManager gm = GuildsManager.get(lvl);
                this.guild = gm.getGuild(this.guild.getName()); // Get fresh copy
            }
            // Server side: create data that will be synced
            this.data = new SimpleContainerData(4);
            this.data.set(0, this.page);
            this.data.set(1, getStorageLevel());
            // initialize counts
            if (this.guild != null) {
                GuildsManager gm2 = GuildsManager.get(lvl);
                var st2 = gm2.getOrCreateStorage(this.guild.getName());
                int used2 = 0;
                for (ItemStack s : st2)
                    if (s != null && !s.isEmpty())
                        used2++;
                this.data.set(2, used2);
                this.data.set(3, st2.size());
            }
            if (this.guild != null) {
                OPEN_MENUS
                        .computeIfAbsent(this.guild.getName(), k -> java.util.concurrent.ConcurrentHashMap.newKeySet())
                        .add(this);
            }
        } else {
            // Client side: create data that will receive synced values
            this.data = new SimpleContainerData(4);
        }

        this.addDataSlots(this.data);
        java.util.Arrays.fill(viewMap, -1);
        rebuild();
        // storage view slots (6 rows from y=18 like a large chest) with extended stack
        // size (32k)
        for (int row = 0; row < 6; ++row) {
            for (int col = 0; col < 9; ++col) {
                final int idx = col + row * 9;
                this.addSlot(new Slot(view, idx, 8 + col * 18, 18 + row * 18));
            }
        }
        // Player inventory aligned to large chest layout: top row y=140, hotbar y=198
        int invY = 198;
        int invTopY = invY - 58; // 140
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                this.addSlot(new Slot(inv, col + row * 9 + 9, 8 + col * 18, invTopY + row * 18));
            }
        }
        for (int col = 0; col < 9; ++col) {
            this.addSlot(new Slot(inv, col, 8 + col * 18, invY));
        }
        if (player instanceof ServerPlayer) {
            broadcastChanges();
        }
    }

    public void setAllowControlAction(boolean allow) {
        this.allowControlAction = allow;
    }

    public void setClientCounts(int page, int[] counts) {
        if (counts == null || counts.length != 54)
            return;
        this.clientCounts = counts;
    }

    public int getClientCountAt(int viewIndex) {
        if (viewIndex < 0 || viewIndex >= 54)
            return 0;
        int v = clientCounts[viewIndex];
        if (v > 0)
            return v;
        ItemStack s = view.getItem(viewIndex);
        return s == null || s.isEmpty() ? 0 : s.getCount();
    }

    private static void notifyStorageChanged(String guildName) {
        java.util.Set<GuildStorageMenu> set = OPEN_MENUS.get(guildName);
        if (set == null || set.isEmpty())
            return;
        for (GuildStorageMenu m : set) {
            try {
                m.rebuild();
                m.refreshCounts();
                m.broadcastChanges();
            } catch (Throwable ignored) {
            }
        }
    }

    public void setSearchText(String text) {
        this.searchText = text.toLowerCase();
        rebuild();
        broadcastChanges();
    }

    public int getCurrentPage() {
        // Use synced data on client, actual page on server
        if (player.level().isClientSide()) {
            return data.get(0);
        }
        return page;
    }

    public int getMaxPages() {
        return 4 + getStorageLevel();
    }

    public int getUsedItems() {
        if (player.level().isClientSide()) {
            return data.get(2);
        }
        if (guild == null || !(player instanceof ServerPlayer sp))
            return 0;
        GuildsManager gm = GuildsManager.get(sp.serverLevel());
        var storage = gm.getOrCreateStorage(guild.getName());
        int used = 0;
        for (ItemStack stack : storage)
            if (!stack.isEmpty())
                used++;
        return used;
    }

    public int getMaxCapacity() {
        if (player.level().isClientSide()) {
            return data.get(3);
        }
        if (guild == null || !(player instanceof ServerPlayer sp))
            return 54 * getMaxPages();
        GuildsManager gm = GuildsManager.get(sp.serverLevel());
        var storage = gm.getOrCreateStorage(guild.getName());
        return storage.size();
    }

    public int getStorageLevel() {
        // Use synced data on client
        if (player.level().isClientSide()) {
            return data.get(1);
        }
        // Always get fresh storage level from manager on server
        if (guild == null || !(player instanceof ServerPlayer sp))
            return 1;
        GuildsManager gm = GuildsManager.get(sp.serverLevel());
        Guild freshGuild = gm.getGuild(guild.getName());
        if (freshGuild == null)
            return 1;
        return freshGuild.getStorageLevel();
    }

    public Filter getFilter() {
        return filter;
    }

    public void refreshGuild() {
        if (player instanceof ServerPlayer sp) {
            ServerLevel lvl = sp.serverLevel();
            GuildsManager gm = GuildsManager.get(lvl);
            Guild memberGuild = gm.getGuildByMember(sp.getUUID());
            if (memberGuild != null) {
                // Always get the fresh copy from the manager
                this.guild = gm.getGuild(memberGuild.getName());
            }
        }
    }

    public boolean areAllSlotsOnPageFull() {
        if (guild == null)
            return false;

        // Check if all 54 visible slots in the view have items
        for (int i = 0; i < 54; i++) {
            ItemStack stack = view.getItem(i);
            if (stack.isEmpty()) {
                return false;
            }
        }
        return true; // All 54 slots are occupied
    }

    private void rebuild() {
        if (!(player instanceof ServerPlayer sp) || guild == null)
            return;
        GuildsManager gm = GuildsManager.get(sp.serverLevel());
        var storage = gm.getOrCreateStorage(guild.getName());

        // If searching, build filtered and sorted view
        if (!searchText.isEmpty() || filter != Filter.ALL) {
            rebuildFilteredView(gm, storage);
        } else {
            // Normal pagination view
            int start = page * 54;
            migrateControlSlotItems(storage, start);
            java.util.Arrays.fill(mirrored, false);
            java.util.Arrays.fill(viewMap, -1);
            for (int i = 0; i < 54; i++) {
                ItemStack s = (start + i) < storage.size() ? storage.get(start + i) : ItemStack.EMPTY;
                view.setItem(i, s.copy());
                mirrored[i] = true;
                viewMap[i] = (start + i) < storage.size() ? (start + i) : -1;
            }
        }
    }

    private void rebuildFilteredView(GuildsManager gm, java.util.List<ItemStack> storage) {
        java.util.Arrays.fill(mirrored, false);
        java.util.Arrays.fill(viewMap, -1);
        // Clear view first
        for (int i = 0; i < 54; i++) {
            view.setItem(i, ItemStack.EMPTY);
        }

        // Collect all matching items
        java.util.List<ItemStack> filtered = new java.util.ArrayList<>();
        for (int i = 0; i < storage.size(); i++) {
            ItemStack s = storage.get(i);
            if (!s.isEmpty() && matchesFilter(s) && matchesSearch(s)) {
                ItemStack copy = s.copy();
                copy.getOrCreateTag().putInt("_storageIndex", i); // Track original position
                filtered.add(copy);
            }
        }

        // Sort alphabetically by item name
        filtered.sort((a, b) -> {
            String nameA = a.getHoverName().getString().toLowerCase();
            String nameB = b.getHoverName().getString().toLowerCase();
            return nameA.compareTo(nameB);
        });

        // Place filtered items starting from slot 0
        int viewSlot = 0;
        int start = page * 54;
        for (int i = start; i < Math.min(start + 54, filtered.size()); i++) {
            if (viewSlot < 54) {
                ItemStack item = filtered.get(i);
                // Remove tracking tag before setting in view
                if (item.hasTag()) {
                    int mapped = item.getTag().getInt("_storageIndex");
                    item.getTag().remove("_storageIndex");
                    viewMap[viewSlot] = mapped;
                }
                view.setItem(viewSlot, item);
                mirrored[viewSlot] = true;
                viewSlot++;
            }
        }
    }

    public void refreshCounts() {
        if (!(player instanceof ServerPlayer sp) || guild == null)
            return;
        GuildsManager gm = GuildsManager.get(sp.serverLevel());
        var storage = gm.getOrCreateStorage(guild.getName());
        int used = 0;
        for (ItemStack ss : storage)
            if (ss != null && !ss.isEmpty())
                used++;
        data.set(2, used);
        data.set(3, storage.size());
    }

    private boolean matchesSearch(ItemStack stack) {
        if (searchText.isEmpty())
            return true;
        if (stack.isEmpty())
            return false;

        String itemName = stack.getHoverName().getString().toLowerCase();
        String itemId = net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(stack.getItem()).toString()
                .toLowerCase();

        return itemName.contains(searchText) || itemId.contains(searchText);
    }

    private void addLore(ItemStack stack, java.util.List<Component> lore) {
        net.minecraft.nbt.ListTag tag = new net.minecraft.nbt.ListTag();
        for (Component c : lore)
            tag.add(net.minecraft.nbt.StringTag.valueOf(Component.Serializer.toJson(c)));
        stack.getOrCreateTagElement("display").put("Lore", tag);
    }

    @Override
    public void broadcastChanges() {
        super.broadcastChanges();
        // Update synced data on server
        if (!player.level().isClientSide()) {
            this.data.set(0, this.page);
            this.data.set(1, getStorageLevel());
            GuildsManager gm = GuildsManager.get(((ServerPlayer) player).serverLevel());
            var storage = gm.getOrCreateStorage(guild == null ? null : guild.getName());
            int[] counts = new int[54];
            for (int i = 0; i < 54; i++) {
                if (!mirrored[i]) {
                    counts[i] = 0;
                    continue;
                }
                int idx = viewMap[i];
                if (idx >= 0 && idx < storage.size())
                    counts[i] = storage.get(idx).getCount();
            }
            int used = 0;
            for (ItemStack ss : storage)
                if (ss != null && !ss.isEmpty())
                    used++;
            this.data.set(2, used);
            this.data.set(3, storage.size());
            if (player instanceof ServerPlayer sp)
                org.lupz.doomsdayessentials.network.PacketHandler.CHANNEL.send(
                        net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> sp),
                        new GuildStorageCountsPacket(page, counts));
        }
        // Periodically refresh guild data (but not every tick)
        if (player.level().getGameTime() % 20 == 0) { // Every second
            refreshGuild();
        }
    }

    @Override
    public void clicked(int slotId, int dragType, @NotNull ClickType clickType, @NotNull Player clickPlayer) {
        if (!(clickPlayer instanceof ServerPlayer sp) || guild == null) {
            super.clicked(slotId, dragType, clickType, clickPlayer);
            return;
        }
        GuildsManager gm = GuildsManager.get(sp.serverLevel());
        var storage = gm.getOrCreateStorage(guild.getName());
        // Control actions only execute when called from external buttons via packet
        if (allowControlAction && slotId == SLOT_BACK) {
            // Back to main menu if on first page; otherwise previous page
            if (page == 0) {
                sp.openMenu(new net.minecraft.world.SimpleMenuProvider((id, inv, p) -> new GuildMainMenu(id, inv),
                        Component.literal("Organização")));
                return;
            } else {
                page--;
                this.data.set(0, this.page); // Update synced data immediately
                rebuild();
                broadcastChanges();
                if (guild != null)
                    notifyStorageChanged(guild.getName());
                return;
            }
        }
        if (allowControlAction && slotId == SLOT_NEXT) {
            // Get the actual max pages from fresh data
            int maxAllowedPages = getMaxPages(); // This now fetches fresh data

            if (page < maxAllowedPages - 1) {
                // Expand storage if needed
                int newSize = (page + 2) * 54; // Ensure next page exists
                while (storage.size() < newSize) {
                    storage.add(ItemStack.EMPTY);
                }
                page++;
                this.data.set(0, this.page); // Update synced data immediately
                rebuild();
                broadcastChanges();
                gm.setDirty();
                sp.sendSystemMessage(Component.literal("§aPágina " + (page + 1) + "/" + maxAllowedPages));
                if (guild != null)
                    notifyStorageChanged(guild.getName());
            } else {
                sp.sendSystemMessage(Component.literal("§cÚltima página. Aprimore o cofre para mais páginas."));
            }
            return;
        }
        if (allowControlAction && slotId == SLOT_FILTER) {
            // cycle filter
            filter = switch (filter) {
                case ALL -> Filter.BLOCKS;
                case BLOCKS -> Filter.ITEMS;
                case ITEMS -> Filter.TOOLS;
                case TOOLS -> Filter.WEAPONS;
                case WEAPONS -> Filter.ARMOR;
                case ARMOR -> Filter.FOOD;
                case FOOD -> Filter.POTIONS;
                case POTIONS -> Filter.ENCHANTED;
                case ENCHANTED -> Filter.ALL;
            };
            rebuild();
            broadcastChanges();
            if (guild != null)
                notifyStorageChanged(guild.getName());
            return;
        }
        if (allowControlAction && slotId == SLOT_SORT) {
            sortEntireStorage(sp);
            rebuild();
            broadcastChanges();
            if (guild != null)
                notifyStorageChanged(guild.getName());
            return;
        }
        if (allowControlAction && slotId == SLOT_LOG) {
            sp.openMenu(new net.minecraft.world.SimpleMenuProvider((id, inv,
                    p) -> new org.lupz.doomsdayessentials.guild.menu.GuildStorageLogMenu(id, inv, guild.getName()),
                    Component.literal("HistÃ³rico do Cofre")));
            return;
        }
        if (allowControlAction && slotId == SLOT_UPGRADE) {
            org.lupz.doomsdayessentials.guild.GuildMember self = guild.getMember(sp.getUUID());
            if (self == null || (self.getRank() != org.lupz.doomsdayessentials.guild.GuildMember.Rank.LEADER
                    && self.getRank() != org.lupz.doomsdayessentials.guild.GuildMember.Rank.OFFICER)) {
                sp.sendSystemMessage(Component.literal("§cApenas Líder/Oficial pode comprar upgrades."));
                return;
            }
            int level = guild.getStorageLevel();
            if (level >= 10) {
                sp.sendSystemMessage(Component.literal("§aNível máximo atingido."));
                return;
            }
            int cost = 500 + (level - 1) * 500;
            // Use guild resource bank instead of player's inventory
            org.lupz.doomsdayessentials.guild.GuildResourceBank bank = org.lupz.doomsdayessentials.guild.GuildResourceBank
                    .get(sp.serverLevel());
            String scrapId = net.minecraftforge.registries.ForgeRegistries.ITEMS
                    .getKey(org.lupz.doomsdayessentials.item.ModItems.SCRAPMETAL.get()).toString();
            int have = bank.get(guild.getName(), scrapId);
            if (have < cost) {
                sp.sendSystemMessage(Component.literal("§cA organização precisa de " + cost + " sucata no cofre."));
                return;
            }
            boolean debited = bank.consume(guild.getName(), scrapId, cost);
            if (!debited) {
                sp.sendSystemMessage(Component.literal("§cFalha ao debitar recursos da organização."));
                return;
            }
            boolean ok = gm.upgradeStorageLevel(guild.getName());
            if (ok) {
                // Force refresh guild data from manager
                refreshGuild();
                // Get the new level directly from manager to ensure it's current
                int newLevel = getStorageLevel();
                this.data.set(1, newLevel); // Update synced data immediately
                sp.sendSystemMessage(Component.literal("§aArmazenamento aprimorado! Nível " + newLevel));
                rebuild();
                broadcastChanges();
            }
            return;
        }
        // Handle pickup (non-shift) with true-count aware storage updates
        if (clickType == ClickType.PICKUP && slotId >= 0 && slotId < 54 && mirrored[slotId]) {
            ItemStack carried = this.getCarried().copy();
            int start2 = page * 54;
            int button = dragType;
            ItemStack encSlot = storage.get(start2 + slotId);
            ItemStack decSlot = encSlot.copy();
            org.lupz.doomsdayessentials.guild.StorageDiagnostics.logTrace("clicked_start",
                    guild == null ? "" : guild.getName(), page, slotId, decSlot);
            org.lupz.doomsdayessentials.guild.StorageDiagnostics.logTrace("clicked_carried",
                    guild == null ? "" : guild.getName(), page, slotId, carried);

            // Standard pickup logic for guild storage (no overstacking)
            if (carried.isEmpty() && !decSlot.isEmpty()) {
                int toTake = (button == 1) ? (decSlot.getCount() + 1) / 2 : decSlot.getCount();
                if (toTake > 0) {
                    ItemStack taken = decSlot.copy();
                    taken.setCount(toTake);
                    this.setCarried(taken);
                    decSlot.shrink(toTake);
                    storage.set(start2 + slotId, decSlot.isEmpty() ? ItemStack.EMPTY : decSlot);
                    view.setItem(slotId, decSlot.isEmpty() ? ItemStack.EMPTY : decSlot.copy());
                    gm.setDirty();
                    broadcastChanges();
                    if (guild != null)
                        notifyStorageChanged(guild.getName());
                }
            } else if (!carried.isEmpty() && decSlot.isEmpty()) {
                int limit = Math.min(view.getMaxStackSize(), carried.getMaxStackSize());
                int toPlace = (button == 1) ? 1 : carried.getCount();
                toPlace = Math.min(toPlace, limit);
                if (toPlace > 0) {
                    ItemStack placing = carried.copy();
                    placing.setCount(toPlace);
                    storage.set(start2 + slotId, placing);
                    view.setItem(slotId, placing.copy());
                    carried.shrink(toPlace);
                    this.setCarried(carried.isEmpty() ? ItemStack.EMPTY : carried);
                    gm.setDirty();
                    broadcastChanges();
                    if (guild != null)
                        notifyStorageChanged(guild.getName());
                }
            } else if (!carried.isEmpty() && !decSlot.isEmpty() && ItemStack.isSameItemSameTags(carried, decSlot)) {
                int limit = Math.min(view.getMaxStackSize(), decSlot.getMaxStackSize());
                int free = Math.max(0, limit - decSlot.getCount());
                int toAdd = (button == 1) ? 1 : Math.min(carried.getCount(), free);
                if (toAdd > 0) {
                    decSlot.grow(toAdd);
                    storage.set(start2 + slotId, decSlot);
                    view.setItem(slotId, decSlot.copy());
                    carried.shrink(toAdd);
                    this.setCarried(carried.isEmpty() ? ItemStack.EMPTY : carried);
                    gm.setDirty();
                    broadcastChanges();
                    if (guild != null)
                        notifyStorageChanged(guild.getName());
                }
            } else if (!carried.isEmpty() && !decSlot.isEmpty()) {
                // Swap
                int limit = Math.min(view.getMaxStackSize(), carried.getMaxStackSize());
                if (carried.getCount() <= limit) {
                    ItemStack temp = carried.copy();
                    this.setCarried(decSlot.copy());
                    storage.set(start2 + slotId, temp);
                    view.setItem(slotId, temp.copy());
                    gm.setDirty();
                    broadcastChanges();
                    if (guild != null)
                        notifyStorageChanged(guild.getName());
                }
            }
            return;
        }
        if (clickType == ClickType.SWAP && slotId >= 0 && slotId < 54 && mirrored[slotId]) {
            int hotbarIndex = dragType;
            if (hotbarIndex >= 0 && hotbarIndex < 9) {
                Inventory inv = sp.getInventory();
                ItemStack inView = view.getItem(slotId);
                if (!inView.isEmpty()) {
                    ItemStack hotbar = inv.getItem(hotbarIndex);
                    int maxStack = Math.max(1, Math.min(64, inView.getMaxStackSize()));
                    ItemStack beforeSlot = inView.copy();
                    int removed = 0;
                    boolean changed = false;
                    if (hotbar.isEmpty()) {
                        int take = Math.min(maxStack, inView.getCount());
                        if (take > 0) {
                            ItemStack taken = inView.copy();
                            taken.setCount(take);
                            inv.setItem(hotbarIndex, taken);
                            inView.shrink(take);
                            removed = take;
                            changed = true;
                        }
                    } else if (ItemStack.isSameItemSameTags(inView, hotbar)) {
                        int maxHotbar = Math.max(1, Math.min(64, hotbar.getMaxStackSize()));
                        int free = maxHotbar - hotbar.getCount();
                        if (free > 0) {
                            int move = Math.min(free, inView.getCount());
                            if (move > 0) {
                                hotbar.grow(move);
                                inv.setItem(hotbarIndex, hotbar);
                                inView.shrink(move);
                                removed = move;
                                changed = true;
                            }
                        }
                    } else {
                        // Swap
                        ItemStack toInsert = hotbar.copy();
                        ItemStack toTake = inView.copy();

                        view.setItem(slotId, toInsert);
                        inv.setItem(hotbarIndex, toTake);

                        changed = true;
                        removed = toTake.getCount();
                    }
                    if (changed) {
                        if (inView.isEmpty())
                            view.setItem(slotId, ItemStack.EMPTY);
                        else
                            view.setItem(slotId, inView);
                        inv.setChanged();
                        persistPage(gm, storage);
                        gm.setDirty();
                        broadcastChanges();
                        if (guild != null)
                            notifyStorageChanged(guild.getName());
                        org.lupz.doomsdayessentials.guild.StorageDiagnostics.logTrace("hotbar_swap",
                                guild == null ? "" : guild.getName(), page, slotId, inView);
                        if (removed > 0) {
                            String id = net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(beforeSlot.getItem())
                                    .toString();
                            gm.logStorageChange(guild.getName(), sp.getUUID(), "remove", id, removed, page, slotId);
                            org.lupz.doomsdayessentials.guild.StorageDiagnostics.logOp("hotbar_swap_remove",
                                    guild.getName(), page, slotId, beforeSlot, removed);
                        }
                    }
                    return;
                }
            }
        }
        // Sync changes to underlying storage on any other click that changes items
        // Track changes for logging: compare before/after for this slot
        ItemStack before = slotId >= 0 && slotId < view.getContainerSize() && mirrored[slotId]
                ? view.getItem(slotId).copy()
                : ItemStack.EMPTY;
        try {
            super.clicked(slotId, dragType, clickType, clickPlayer);
        } catch (Throwable t) {
            org.lupz.doomsdayessentials.guild.StorageDiagnostics
                    .logError("clicked slotId=" + slotId + ", dragType=" + dragType + ", clickType=" + clickType, t);
        }
        persistPage(gm, storage);
        gm.setDirty();
        if (guild != null)
            notifyStorageChanged(guild.getName());
        if (slotId >= 0 && slotId < 54 && mirrored[slotId] && clickPlayer instanceof ServerPlayer sp2) {
            ItemStack after = view.getItem(slotId);
            org.lupz.doomsdayessentials.guild.StorageDiagnostics.logTrace("clicked_after",
                    guild == null ? "" : guild.getName(), page, slotId, after);
            if (!ItemStack.isSameItemSameTags(before, after)) {
                String idBefore = before.isEmpty() ? ""
                        : net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(before.getItem()).toString();
                String idAfter = after.isEmpty() ? ""
                        : net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(after.getItem()).toString();
                int delta = after.getCount() - before.getCount();
                String action;
                String id;
                int amount;
                if (delta > 0) {
                    action = "add";
                    id = idAfter;
                    amount = delta;
                } else if (delta < 0) {
                    action = "remove";
                    id = idBefore;
                    amount = -delta;
                } else {
                    action = "move";
                    id = idAfter;
                    amount = after.getCount();
                }
                gm.logStorageChange(guild.getName(), sp2.getUUID(), action, id, amount, page, slotId);
                org.lupz.doomsdayessentials.guild.StorageDiagnostics.logOp(action, guild.getName(), page, slotId, after,
                        amount);
            }
        }
    }

    @Override
    public void removed(Player p) {
        super.removed(p);
        if (!(p instanceof ServerPlayer sp) || guild == null)
            return;
        try {
            java.util.Set<GuildStorageMenu> set = OPEN_MENUS.get(guild.getName());
            if (set != null)
                set.remove(this);
        } catch (Throwable ignored) {
        }
        GuildsManager gm = GuildsManager.get(sp.serverLevel());
        var storage = gm.getOrCreateStorage(guild.getName());
        persistPage(gm, storage);
        // no capacity enforcement on close
        gm.setDirty();
    }

    private String filterName() {
        return switch (filter) {
            case ALL -> "Tudo";
            case BLOCKS -> "Blocos";
            case ITEMS -> "Itens";
            case TOOLS -> "Ferramentas";
            case WEAPONS -> "Armas";
            case ARMOR -> "Armaduras";
            case FOOD -> "Comida";
            case POTIONS -> "Poções";
            case ENCHANTED -> "Encantados";
        };
    }

    @Override
    public @NotNull ItemStack quickMoveStack(@NotNull Player player, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack itemstack1 = slot.getItem();
            itemstack = itemstack1.copy();
            if (index < 54) {
                if (!this.moveItemStackTo(itemstack1, 54, 90, true)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.moveItemStackTo(itemstack1, 0, 54, false)) {
                return ItemStack.EMPTY;
            }

            if (itemstack1.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
            if (itemstack1.getCount() == itemstack.getCount()) {
                return ItemStack.EMPTY;
            }
            slot.onTake(player, itemstack1);

            if (player instanceof ServerPlayer sp && guild != null) {
                GuildsManager gm = GuildsManager.get(sp.serverLevel());
                var storage = gm.getOrCreateStorage(guild.getName());
                persistPage(gm, storage);
                gm.setDirty();
                broadcastChanges();
                notifyStorageChanged(guild.getName());
            }
        }
        return itemstack;
    }

    // moveItemStackTo removed (uses super)

    private boolean isStorageIndex(int idx) {
        return idx >= 0 && idx < 54;
    }

    private int storageLimit(ItemStack stack) {
        return stack.getMaxStackSize();
    }

    // canInsertHotbarStack and insertHotbarStack removed

    private void persistPage(GuildsManager gm, net.minecraft.core.NonNullList<ItemStack> storage) {
        int start = page * 54;
        if (!searchText.isEmpty() || filter != Filter.ALL) {
            for (int i = 0; i < 54; i++) {
                if (!mirrored[i])
                    continue;
                int mapped = (i >= 0 && i < viewMap.length) ? viewMap[i] : -1;
                if (mapped >= 0 && mapped < storage.size()) {
                    ItemStack curView = view.getItem(i);
                    storage.set(mapped, curView.copy());
                }
            }
        } else {
            for (int i = 0; i < 54 && (start + i) < storage.size(); i++) {
                if (!mirrored[i])
                    continue;
                ItemStack curView = view.getItem(i);
                storage.set(start + i, curView.copy());
            }
        }
    }

    // enforceCapacity and decodedTotal removed

    private boolean isReserved(int slot) {
        return slot == SLOT_BACK || slot == SLOT_NEXT || slot == SLOT_FILTER || slot == SLOT_SORT || slot == SLOT_LOG
                || slot == SLOT_UPGRADE;
    }

    // encodeForStorage and decodeFromStorage removed

    private void migrateControlSlotItems(net.minecraft.core.NonNullList<ItemStack> storage, int start) {
        int[] reserved = { SLOT_BACK, SLOT_NEXT, SLOT_FILTER, SLOT_SORT, SLOT_LOG, SLOT_UPGRADE };
        java.util.Set<Integer> reservedSet = new java.util.HashSet<>();
        for (int r : reserved)
            reservedSet.add(r);
        // Gather items from reserved slots on this page
        java.util.List<ItemStack> toReinsert = new java.util.ArrayList<>();
        for (int i = 0; i < 54; i++) {
            if (!reservedSet.contains(i))
                continue;
            int idx = start + i;
            if (idx >= 0 && idx < storage.size()) {
                ItemStack enc = storage.get(idx);
                if (enc != null && !enc.isEmpty()) {
                    toReinsert.add(enc);
                    storage.set(idx, ItemStack.EMPTY);
                }
            }
        }
        // Reinsert into nearest free non-reserved slots, merging by same item+nbt
        for (ItemStack enc : toReinsert) {
            ItemStack dec = enc.copy();
            int remaining = enc.getCount();
            // First pass: merge
            for (int i = 0; i < 54 && remaining > 0; i++) {
                if (reservedSet.contains(i))
                    continue;
                int idx = start + i;
                if (idx >= storage.size())
                    break;
                ItemStack exist = storage.get(idx).copy();
                if (!exist.isEmpty() && ItemStack.isSameItemSameTags(exist, dec)) {
                    int free = Math.max(0, storageLimit(exist) - exist.getCount());
                    if (free > 0) {
                        int move = Math.min(free, remaining);
                        exist.setCount(exist.getCount() + move);
                        storage.set(idx, exist);
                        remaining -= move;
                    }
                }
            }
            // Second pass: empty slots
            for (int i = 0; i < 54 && remaining > 0; i++) {
                if (reservedSet.contains(i))
                    continue;
                int idx = start + i;
                if (idx >= storage.size())
                    break;
                ItemStack exist = storage.get(idx);
                if (exist == null || exist.isEmpty()) {
                    int put = Math.min(storageLimit(dec), remaining);
                    ItemStack place = dec.copy();
                    place.setCount(put);
                    storage.set(idx, place);
                    remaining -= put;
                }
            }
            // If still remaining, append to next pages if exist
            int idx = start + 54;
            while (remaining > 0 && idx < storage.size()) {
                int rel = idx % 54;
                if (!reservedSet.contains(rel)) {
                    ItemStack exist = storage.get(idx).copy();
                    if (exist.isEmpty()) {
                        int put = Math.min(storageLimit(dec), remaining);
                        ItemStack place = dec.copy();
                        place.setCount(put);
                        storage.set(idx, place);
                        remaining -= put;
                    } else if (ItemStack.isSameItemSameTags(exist, dec)) {
                        int free = Math.max(0, storageLimit(exist) - exist.getCount());
                        if (free > 0) {
                            int move = Math.min(free, remaining);
                            exist.setCount(exist.getCount() + move);
                            storage.set(idx, exist);
                            remaining -= move;
                        }
                    }
                }
                idx++;
            }
        }
    }

    // -------- Sorting --------
    private void sortEntireStorage(ServerPlayer sp) {
        GuildsManager gm = GuildsManager.get(sp.serverLevel());
        var storage = gm.getOrCreateStorage(guild.getName());
        // Aggregate by item+nbt
        class Key {
            final net.minecraft.world.item.Item item;
            final net.minecraft.nbt.CompoundTag tag;

            Key(ItemStack s) {
                this.item = s.getItem();
                this.tag = s.getTag() == null ? null : s.getTag().copy();
            }

            @Override
            public boolean equals(Object o) {
                if (this == o)
                    return true;
                if (!(o instanceof Key k))
                    return false;
                if (item != k.item)
                    return false;
                return java.util.Objects.equals(tag, k.tag);
            }

            @Override
            public int hashCode() {
                return java.util.Objects.hash(item, tag == null ? 0 : tag.hashCode());
            }
        }
        java.util.Map<Key, Integer> totals = new java.util.HashMap<>();
        java.util.Map<Key, ItemStack> reps = new java.util.HashMap<>();
        for (ItemStack s : storage) {
            if (s == null || s.isEmpty())
                continue;
            ItemStack decoded = s.copy();
            ItemStack normalized = decoded; // No tag stripping needed
            Key k = new Key(normalized);
            totals.merge(k, s.getCount(), Integer::sum);
            reps.putIfAbsent(k, normalized.copy());
        }
        // Build available fill indices (skip control slots on every page)
        java.util.List<Integer> validIndices = new java.util.ArrayList<>(storage.size());
        for (int abs = 0; abs < storage.size(); abs++) {
            int rel = abs % 54;
            if (isStorageIndex(rel) && !isReserved(rel))
                validIndices.add(abs);
        }
        int capacity = validIndices.size();
        // Capacity pre-check: ensure enough slots when respecting per-item limits
        // (e.g., unstackables=1)
        int requiredSlots = 0;
        for (java.util.Map.Entry<Key, Integer> e : totals.entrySet()) {
            ItemStack rep = reps.get(e.getKey());
            int limit = storageLimit(rep);
            if (limit <= 0)
                limit = 1;
            int cnt = Math.max(0, e.getValue());
            requiredSlots += (cnt + limit - 1) / limit;
        }
        if (requiredSlots > capacity) {
            sp.sendSystemMessage(Component.literal("§cOrganizar cancelado: itens demais para a capacidade atual ("
                    + requiredSlots + "/" + capacity + ")."));
            return;
        }
        java.util.List<Key> keys = new java.util.ArrayList<>(totals.keySet());
        keys.sort((a, b) -> {
            int ca = categoryOf(reps.get(a));
            int cb = categoryOf(reps.get(b));
            if (ca != cb)
                return Integer.compare(ca, cb);
            String ia = java.util.Objects
                    .toString(net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(reps.get(a).getItem()));
            String ib = java.util.Objects
                    .toString(net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(reps.get(b).getItem()));
            int c = ia.compareTo(ib);
            if (c != 0)
                return c;
            String ta = reps.get(a).getTag() == null ? "" : reps.get(a).getTag().toString();
            String tb = reps.get(b).getTag() == null ? "" : reps.get(b).getTag().toString();
            return ta.compareTo(tb);
        });
        // Prepare new storage filled only in valid indices
        net.minecraft.core.NonNullList<ItemStack> newStorage = net.minecraft.core.NonNullList.withSize(storage.size(),
                ItemStack.EMPTY);
        int pos = 0;
        for (Key k : keys) {
            int cnt = totals.getOrDefault(k, 0);
            ItemStack base = reps.get(k).copy();
            base.setCount(1);
            while (cnt > 0 && pos < capacity) {
                ItemStack toPut = base.copy();
                int limit = storageLimit(toPut);
                int put = Math.min(limit, cnt);
                toPut.setCount(put);
                int target = validIndices.get(pos++);
                newStorage.set(target, toPut);
                cnt -= put;
            }
        }
        // Apply new layout and clear any remaining slots (including reserved/control)
        for (int i = 0; i < storage.size(); i++) {
            storage.set(i, newStorage.get(i));
        }
        gm.setDirty();
    }

    private int categoryOf(ItemStack s) {
        net.minecraft.world.item.Item i = s.getItem();
        if (i instanceof net.minecraft.world.item.BlockItem)
            return 0;
        if (i instanceof net.minecraft.world.item.ArmorItem)
            return 1;
        if (i instanceof net.minecraft.world.item.SwordItem || i instanceof net.minecraft.world.item.BowItem
                || i instanceof net.minecraft.world.item.CrossbowItem
                || i instanceof net.minecraft.world.item.TridentItem)
            return 2;
        if (i instanceof net.minecraft.world.item.TieredItem)
            return 3;
        if (s.isEdible())
            return 4;
        return 5;
    }

    private boolean matchesFilter(ItemStack s) {
        if (s == null || s.isEmpty())
            return true;
        net.minecraft.world.item.Item i = s.getItem();
        return switch (filter) {
            case ALL -> true;
            case BLOCKS -> i instanceof net.minecraft.world.item.BlockItem;
            case ITEMS -> !(i instanceof net.minecraft.world.item.BlockItem);
            case TOOLS -> (i instanceof net.minecraft.world.item.TieredItem) ||
                    (s.isDamageableItem() &&
                            !(i instanceof net.minecraft.world.item.ArmorItem) &&
                            !(i instanceof net.minecraft.world.item.SwordItem) &&
                            !(i instanceof net.minecraft.world.item.BowItem) &&
                            !(i instanceof net.minecraft.world.item.CrossbowItem) &&
                            !(i instanceof net.minecraft.world.item.TridentItem));
            case WEAPONS -> (i instanceof net.minecraft.world.item.SwordItem) ||
                    (i instanceof net.minecraft.world.item.BowItem) ||
                    (i instanceof net.minecraft.world.item.CrossbowItem) ||
                    (i instanceof net.minecraft.world.item.TridentItem);
            case ARMOR -> i instanceof net.minecraft.world.item.ArmorItem;
            case FOOD -> s.isEdible();
            case POTIONS -> i instanceof net.minecraft.world.item.PotionItem ||
                    i == net.minecraft.world.item.Items.POTION ||
                    i == net.minecraft.world.item.Items.SPLASH_POTION ||
                    i == net.minecraft.world.item.Items.LINGERING_POTION;
            case ENCHANTED -> s.isEnchanted() || i instanceof net.minecraft.world.item.EnchantedBookItem;
        };
    }

    @Override
    public boolean stillValid(@NotNull Player p) {
        if (!(p instanceof ServerPlayer sp))
            return true;
        GuildsManager gm = GuildsManager.get(sp.serverLevel());
        Guild current = gm.getGuildByMember(sp.getUUID());
        if (guild == null)
            return false;
        return current != null && current.getName().equals(guild.getName());
    }

    // StorageSlot removed

}
