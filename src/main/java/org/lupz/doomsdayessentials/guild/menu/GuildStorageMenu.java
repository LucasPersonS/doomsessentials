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
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.lupz.doomsdayessentials.guild.Guild;
import org.lupz.doomsdayessentials.guild.GuildsManager;
import org.lupz.doomsdayessentials.professions.menu.ProfessionMenuTypes;

/**
 * Global guild storage with pagination. 6x9 grid for storage; bottom rows show player inventory.
 */
public class GuildStorageMenu extends AbstractContainerMenu {
    private final Player player;
    private Guild guild;
    private int page = 0; // 0-based
    private final Container view = new LargeStackContainer(54);
    private Filter filter = Filter.ALL;
    /** Tracks which view slots mirror real storage this rebuild (true) vs filtered/control (false). */
    private final boolean[] mirrored = new boolean[54];

    private static final int SLOT_BACK = 45;
    private static final int SLOT_NEXT = 53;
    private static final int SLOT_FILTER = 46; // toggle
    private static final int SLOT_SORT = 47;   // organize button
    private static final int SLOT_LOG = 52; // open logs
    private static final int SLOT_UPGRADE = 50; // lower right book

    private enum Filter { ALL, BLOCKS, ITEMS }

    public GuildStorageMenu(int windowId, Inventory inv, int page) {
        super(ProfessionMenuTypes.GUILD_STORAGE_MENU.get(), windowId);
        this.player = inv.player;
        this.page = Math.max(0, page);
        if (player instanceof ServerPlayer sp) {
            ServerLevel lvl = sp.serverLevel();
            this.guild = GuildsManager.get(lvl).getGuildByMember(sp.getUUID());
        }
        rebuild();
        // storage view slots (6 rows from y=18 like a large chest) with extended stack size (32k)
        for (int row = 0; row < 6; ++row) {
            for (int col = 0; col < 9; ++col) {
                final int idx = col + row * 9;
                this.addSlot(new StorageSlot(view, idx, 8 + col * 18, 18 + row * 18));
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
    }

    private void rebuild() {
        if (!(player instanceof ServerPlayer sp) || guild == null) return;
        GuildsManager gm = GuildsManager.get(sp.serverLevel());
        var storage = gm.getOrCreateStorage(guild.getName());
        int start = page * 54;
        // Migrate any items that are in control-slot positions on this page to the nearest free normal slot
        migrateControlSlotItems(storage, start);
        java.util.Arrays.fill(mirrored, false);
        for (int i = 0; i < 54; i++) {
            ItemStack s = (start + i) < storage.size() ? storage.get(start + i) : ItemStack.EMPTY;
            if (i == SLOT_BACK || i == SLOT_NEXT || i == SLOT_FILTER || i == SLOT_SORT || i == SLOT_LOG || i == SLOT_UPGRADE) continue;
            boolean visible = true;
            if (filter == Filter.BLOCKS && !(!s.isEmpty() && s.getItem() instanceof net.minecraft.world.item.BlockItem)) visible = false;
            else if (filter == Filter.ITEMS && (!s.isEmpty() && s.getItem() instanceof net.minecraft.world.item.BlockItem)) visible = false;
            if (visible) {
                view.setItem(i, decodeFromStorage(s));
                mirrored[i] = true;
            } else {
                view.setItem(i, ItemStack.EMPTY);
                // Keep empty slots mirrored so deposits can persist even under filters
                mirrored[i] = s.isEmpty();
            }
        }
        // Upgrade book in center
        if (guild != null) {
            int level = guild.getStorageLevel();
            int cap = gm.getStorageCapacityItems(guild.getName());
            int nextLevel = Math.min(level + 1, 10);
            int nextCap = 1000 + (nextLevel - 1) * 2000;
            int cost = level >= 10 ? 0 : (500 + (level - 1) * 500);
            ItemStack book = new ItemStack(net.minecraft.world.item.Items.WRITABLE_BOOK);
            book.setHoverName(Component.literal("§bNível Atual"));
            java.util.List<Component> lore = new java.util.ArrayList<>();
            lore.add(Component.literal("§7Nível: §f" + level));
            lore.add(Component.literal("§7Capacidade: §e" + cap + " itens"));
            if (level < 10) {
                lore.add(Component.literal("§7Próximo: §e" + nextLevel + " (" + nextCap + " itens)"));
                lore.add(Component.literal("§7Custo: §c" + cost + " sucata (recursos da organização)"));
                lore.add(Component.literal("§aClique para Aprimorar"));
            } else {
                lore.add(Component.literal("§aNível Máximo"));
            }
            addLore(book, lore);
            view.setItem(SLOT_UPGRADE, book);
        }
        // place nav arrows (we use paper items with lore as placeholders)
        ItemStack back = new ItemStack(org.lupz.doomsdayessentials.item.ModItems.GUI_BACK.get());
        back.setHoverName(Component.literal("§eVoltar"));
        addLore(back, java.util.List.of(Component.literal("§7Voltar ao menu ou página anterior")));
        view.setItem(SLOT_BACK, back);
        ItemStack next = new ItemStack(org.lupz.doomsdayessentials.item.ModItems.GUI_NEXT.get());
        next.setHoverName(Component.literal("§ePróxima"));
        addLore(next, java.util.List.of(Component.literal("§7Ir para a próxima página")));
        view.setItem(SLOT_NEXT, next);

        ItemStack filterBtn = new ItemStack(net.minecraft.world.item.Items.HOPPER);
        filterBtn.setHoverName(Component.literal("§bFiltro: §f" + filterName()));
        addLore(filterBtn, java.util.List.of(Component.literal("§7Clique para alternar entre Tudo/Blocos/Itens")));
        view.setItem(SLOT_FILTER, filterBtn);

        ItemStack sortBtn = new ItemStack(net.minecraft.world.item.Items.COMPARATOR);
        sortBtn.setHoverName(Component.literal("§bOrganizar Cofre"));
        addLore(sortBtn, java.util.List.of(Component.literal("§7Organiza por tipo/ID/NBT (blocos, ferramentas, etc.)")));
        view.setItem(SLOT_SORT, sortBtn);

        ItemStack logBtn = new ItemStack(net.minecraft.world.item.Items.PAPER);
        logBtn.setHoverName(Component.literal("§eHistórico do Cofre"));
        addLore(logBtn, java.util.List.of(Component.literal("§7Ver registros de alterações do cofre")));
        view.setItem(SLOT_LOG, logBtn);
    }

    private void addLore(ItemStack stack, java.util.List<Component> lore) {
        net.minecraft.nbt.ListTag tag = new net.minecraft.nbt.ListTag();
        for (Component c : lore) tag.add(net.minecraft.nbt.StringTag.valueOf(Component.Serializer.toJson(c)));
        stack.getOrCreateTagElement("display").put("Lore", tag);
    }

    @Override
    public void clicked(int slotId, int dragType, @NotNull ClickType clickType, @NotNull Player clickPlayer) {
        if (!(clickPlayer instanceof ServerPlayer sp) || guild == null) { super.clicked(slotId, dragType, clickType, clickPlayer); return; }
        GuildsManager gm = GuildsManager.get(sp.serverLevel());
        var storage = gm.getOrCreateStorage(guild.getName());

        if (slotId == SLOT_BACK) {
            // Back to main menu if on first page; otherwise previous page
            if (page == 0) {
                sp.openMenu(new net.minecraft.world.SimpleMenuProvider((id, inv, p) -> new GuildMainMenu(id, inv), Component.literal("Organização")));
                return;
            } else { page--; rebuild(); broadcastChanges(); return; }
        }
        if (slotId == SLOT_NEXT) {
            if ((page + 1) * 54 < storage.size()) page++; rebuild(); broadcastChanges(); return;
        }
        if (slotId == SLOT_FILTER) {
            // cycle filter
            filter = switch (filter) { case ALL -> Filter.BLOCKS; case BLOCKS -> Filter.ITEMS; case ITEMS -> Filter.ALL; };
            rebuild(); broadcastChanges(); return;
        }
        if (slotId == SLOT_SORT) {
            sortEntireStorage(sp);
            rebuild(); broadcastChanges();
            return;
        }
        if (slotId == SLOT_LOG) {
            sp.openMenu(new net.minecraft.world.SimpleMenuProvider((id, inv, p) -> new org.lupz.doomsdayessentials.guild.menu.GuildStorageLogMenu(id, inv, guild.getName()), Component.literal("Histórico do Cofre")));
            return;
        }
        if (slotId == SLOT_UPGRADE) {
            org.lupz.doomsdayessentials.guild.GuildMember self = guild.getMember(sp.getUUID());
            if (self == null || (self.getRank() != org.lupz.doomsdayessentials.guild.GuildMember.Rank.LEADER && self.getRank() != org.lupz.doomsdayessentials.guild.GuildMember.Rank.OFFICER)) {
                sp.sendSystemMessage(Component.literal("§cApenas Líder/Oficial pode comprar upgrades."));
                return;
            }
            int level = guild.getStorageLevel();
            if (level >= 10) { sp.sendSystemMessage(Component.literal("§aNível máximo atingido.")); return; }
            int cost = 500 + (level - 1) * 500;
            // Use guild resource bank instead of player's inventory
            org.lupz.doomsdayessentials.guild.GuildResourceBank bank = org.lupz.doomsdayessentials.guild.GuildResourceBank.get(sp.serverLevel());
            String scrapId = net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(org.lupz.doomsdayessentials.item.ModItems.SCRAPMETAL.get()).toString();
            int have = bank.get(guild.getName(), scrapId);
            if (have < cost) { sp.sendSystemMessage(Component.literal("§cA organização precisa de " + cost + " sucata no cofre.")); return; }
            boolean debited = bank.consume(guild.getName(), scrapId, cost);
            if (!debited) { sp.sendSystemMessage(Component.literal("§cFalha ao debitar recursos da organização.")); return; }
            boolean ok = gm.upgradeStorageLevel(guild.getName());
            if (ok) {
                sp.sendSystemMessage(Component.literal("§aCofre aprimorado para nível " + guild.getStorageLevel() + "."));
                rebuild(); broadcastChanges();
            } else {
                sp.sendSystemMessage(Component.literal("§cNão foi possível aprimorar."));
            }
            return;
        }

        // Handle pickup (non-shift):
        // - If cursor empty: take up to 64
        // - If cursor has same item: merge into slot up to 32k (left-click all, right-click 1)
        // - If slot empty and cursor has item: place up to 32k (left-click all, right-click 1)
        if (clickType == ClickType.PICKUP && slotId >= 0 && slotId < 54 && mirrored[slotId]) {
            ItemStack inView = view.getItem(slotId);
            ItemStack carried = this.getCarried();
            int start2 = page * 54;
            int button = dragType; // 0 = left, 1 = right
            if (carried.isEmpty() && !inView.isEmpty()) {
                int toTake = (button == 1) ? ((inView.getCount() + 1) / 2) : Math.min(64, inView.getCount());
                if (toTake > 0) {
                    ItemStack taken = inView.copy();
                    taken.setCount(toTake);
                    this.setCarried(taken);
                    inView.shrink(toTake);
                    view.setItem(slotId, inView);
                    if ((start2 + slotId) < storage.size()) storage.set(start2 + slotId, encodeForStorage(inView));
                    gm.setDirty();
                    broadcastChanges();
                    return;
                }
            } else if (!carried.isEmpty()) {
                if (inView.isEmpty()) {
                    int maxAllowed = storageLimit(carried);
                    if (maxAllowed <= 0) maxAllowed = carried.getMaxStackSize();
                    int put = (button == 1)
                            ? Math.min(1, Math.min(maxAllowed, carried.getCount()))
                            : Math.min(maxAllowed, carried.getCount());
                    ItemStack toPlace = carried.copy(); toPlace.setCount(put);
                    view.setItem(slotId, toPlace);
                    carried.shrink(put);
                    this.setCarried(carried);
                    if ((start2 + slotId) < storage.size()) storage.set(start2 + slotId, encodeForStorage(toPlace));
                    gm.setDirty();
                    broadcastChanges();
                    return;
                } else if (ItemStack.isSameItemSameTags(inView, carried)) {
                    int max = storageLimit(inView);
                    int free = Math.max(0, max - inView.getCount());
                    if (free > 0) {
                        int move = (button == 1) ? Math.min(1, Math.min(free, carried.getCount())) : Math.min(free, carried.getCount());
                        inView.grow(move);
                        carried.shrink(move);
                        view.setItem(slotId, inView);
                        this.setCarried(carried);
                        if ((start2 + slotId) < storage.size()) storage.set(start2 + slotId, encodeForStorage(inView));
                        gm.setDirty();
                        broadcastChanges();
                        return;
                    }
                }
            }
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
                        // swap only if we can fully reinsert the hotbar stack and take at most one stack back
                        if (canInsertHotbarStack(hotbar, slotId)) {
                            ItemStack toReturn = insertHotbarStack(hotbar, slotId);
                            inv.setItem(hotbarIndex, ItemStack.EMPTY);
                            changed = true;
                            int take = Math.min(maxStack, inView.getCount());
                            if (take > 0) {
                                ItemStack taken = inView.copy();
                                taken.setCount(take);
                                inv.setItem(hotbarIndex, taken);
                                inView.shrink(take);
                                removed = take;
                                changed = true;
                            }
                            if (!toReturn.isEmpty()) {
                                // reinsert leftover into inventory if we couldn't place all
                                if (!inv.add(toReturn)) sp.drop(toReturn, false);
                            }
                        }
                    }

                    if (changed) {
                        if (inView.isEmpty()) view.setItem(slotId, ItemStack.EMPTY);
                        else view.setItem(slotId, inView);
                        inv.setChanged();

                        persistPage(gm, storage);
                        enforceCapacity(sp, gm, storage);
                        gm.setDirty();
                        broadcastChanges();

                        if (removed > 0) {
                            String id = net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(beforeSlot.getItem()).toString();
                            gm.logStorageChange(guild.getName(), sp.getUUID(), "remove", id, removed, page, slotId);
                        }
                    }
                    return;
                }
            }
        }

        // Sync changes to underlying storage on any other click that changes items
        // Track changes for logging: compare before/after for this slot
        ItemStack before = slotId >= 0 && slotId < view.getContainerSize() && mirrored[slotId] ? view.getItem(slotId).copy() : ItemStack.EMPTY;
        super.clicked(slotId, dragType, clickType, clickPlayer);
        persistPage(gm, storage);
        enforceCapacity(sp, gm, storage);
        gm.setDirty();
        if (slotId >= 0 && slotId < 54 && mirrored[slotId] && clickPlayer instanceof ServerPlayer sp2) {
            ItemStack after = view.getItem(slotId);
            if (!ItemStack.isSameItemSameTags(before, after)) {
                String idBefore = before.isEmpty() ? "" : net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(before.getItem()).toString();
                String idAfter = after.isEmpty() ? "" : net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(after.getItem()).toString();
                int delta = after.getCount() - before.getCount();
                String action;
                String id;
                int amount;
                if (delta > 0) { action = "add"; id = idAfter; amount = delta; }
                else if (delta < 0) { action = "remove"; id = idBefore; amount = -delta; }
                else { action = "move"; id = idAfter; amount = after.getCount(); }
                gm.logStorageChange(guild.getName(), sp2.getUUID(), action, id, amount, page, slotId);
            }
        }
    }

    @Override
    public void removed(Player p) {
        super.removed(p);
        if (!(p instanceof ServerPlayer sp) || guild == null) return;
        GuildsManager gm = GuildsManager.get(sp.serverLevel());
        var storage = gm.getOrCreateStorage(guild.getName());
        int start = page * 54;
        for (int i = 0; i < 54 && (start + i) < storage.size(); i++) {
            if (!mirrored[i]) continue;
            storage.set(start + i, encodeForStorage(view.getItem(i)));
        }
        // Enforce capacity on close as well
        int cap = gm.getStorageCapacityItems(guild.getName());
        int total = decodedTotal(storage);
        if (total > cap) {
            int toRefund = total - cap;
            for (int idx = storage.size() - 1; idx >= 0 && toRefund > 0; idx--) {
                ItemStack enc = storage.get(idx);
                if (enc.isEmpty()) continue;
                int trueCount = trueCountOf(enc);
                if (trueCount <= 0) { storage.set(idx, ItemStack.EMPTY); continue; }
                int take = Math.min(toRefund, Math.min(trueCount, 64));
                if (take <= 0) continue;
                ItemStack refund = enc.copy();
                if (refund.hasTag()) refund.getTag().remove(EXT_TAG);
                refund.setCount(take);
                int left = trueCount - take;
                if (left <= 0) storage.set(idx, ItemStack.EMPTY);
                else storage.set(idx, encodedWithTrueCount(enc, left));
                if (!sp.getInventory().add(refund)) sp.drop(refund, false);
                toRefund -= take;
            }
        }
        gm.setDirty();
    }

    private String filterName() {
        return switch (filter) { case ALL -> "Tudo"; case BLOCKS -> "Blocos"; case ITEMS -> "Itens"; };
    }

    @Override
    public @NotNull ItemStack quickMoveStack(Player p, int index) {
        // shift-click behavior: from player -> storage page, from storage -> player
        ItemStack empty = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot == null || !slot.hasItem()) return empty;
        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();

        int playerStart = 54;
        int playerEndExclusive = this.slots.size();

        if (index < 54) {
            // storage -> player: take up to 64 per shift-click
            if (!mirrored[index]) return ItemStack.EMPTY;
            int toTake = Math.min(64, stack.getCount());
            if (toTake <= 0) return ItemStack.EMPTY;
            ItemStack part = stack.copy();
            part.setCount(toTake);
            // move to player inventory using vanilla cap behavior
            boolean moved = super.moveItemStackTo(part, playerStart, playerEndExclusive, true);
            if (!moved) return ItemStack.EMPTY;
            // shrink from view slot and update underlying storage with extended encoding
            stack.shrink(toTake);
            slot.setChanged();
            if (player instanceof ServerPlayer sp && guild != null) {
                GuildsManager gm = GuildsManager.get(sp.serverLevel());
                var storage = gm.getOrCreateStorage(guild.getName());
                int start = page * 54;
                if ((start + index) < storage.size()) storage.set(start + index, encodeForStorage(view.getItem(index)));
                gm.setDirty();
            }
        } else {
            // player -> storage. Try normal slots excluding control buttons and center book
            boolean moved = this.moveItemStackTo(stack, 0, 22, false);
            if (!moved) moved = this.moveItemStackTo(stack, 23, 45, false);
            if (!moved) moved = this.moveItemStackTo(stack, 47, 52, false);
            if (!moved) return ItemStack.EMPTY;
            // Persist current page to backing storage immediately after a successful move
            if (player instanceof ServerPlayer sp && guild != null) {
                GuildsManager gm = GuildsManager.get(sp.serverLevel());
                var storage = gm.getOrCreateStorage(guild.getName());
                int start = page * 54;
                for (int i = 0; i < 54 && (start + i) < storage.size(); i++) {
                    if (!mirrored[i]) continue;
                    storage.set(start + i, encodeForStorage(view.getItem(i)));
                }
                gm.setDirty();
            }
        }
        if (stack.isEmpty()) slot.set(ItemStack.EMPTY); else slot.setChanged();
        return copy;
    }

    @Override
    protected boolean moveItemStackTo(@NotNull ItemStack stack, int startIndex, int endIndex, boolean reverse) {
        // For moves targeting player inventory, use vanilla behavior
        if (startIndex >= 54) return super.moveItemStackTo(stack, startIndex, endIndex, reverse);
        // Custom merge that allows up to 32k per slot in guild storage area (0..53 except control slots)
        boolean changed = false;
        int i = reverse ? endIndex - 1 : startIndex;
        // First pass: merge into existing stacks
        while (!stack.isEmpty() && (reverse ? i >= startIndex : i < endIndex)) {
            Slot slot = this.slots.get(i);
            if (slot.container == this.view && isStorageIndex(i) && mirrored[i]) {
                ItemStack inSlot = slot.getItem();
                if (!inSlot.isEmpty() && ItemStack.isSameItemSameTags(inSlot, stack)) {
                    int max = storageLimit(inSlot);
                    int free = max - inSlot.getCount();
                    if (free > 0) {
                        int move = Math.min(free, stack.getCount());
                        inSlot.grow(move);
                        stack.shrink(move);
                        slot.setChanged();
                        changed = true;
                    }
                }
            }
            i += reverse ? -1 : 1;
        }
        // Second pass: empty slots
        i = reverse ? endIndex - 1 : startIndex;
        while (!stack.isEmpty() && (reverse ? i >= startIndex : i < endIndex)) {
            Slot slot = this.slots.get(i);
            if (slot.container == this.view && isStorageIndex(i) && mirrored[i]) {
                ItemStack inSlot = slot.getItem();
                if (inSlot.isEmpty()) {
                    int limit = storageLimit(stack);
                    int put = Math.min(limit, stack.getCount());
                    ItemStack toPlace = stack.copy();
                    toPlace.setCount(put);
                    slot.set(toPlace);
                    slot.setChanged();
                    stack.shrink(put);
                    changed = true;
                    if (stack.isEmpty()) break;
                }
            }
            i += reverse ? -1 : 1;
        }
        return changed;
    }

    private boolean isStorageIndex(int idx) {
        return idx >= 0 && idx < 54 && idx != SLOT_BACK && idx != SLOT_NEXT && idx != SLOT_FILTER && idx != SLOT_SORT && idx != SLOT_LOG && idx != SLOT_UPGRADE;
    }

    private int storageLimit(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return 32767;
        int vanillaMax = stack.getMaxStackSize();
        if (vanillaMax <= 1) return 1;
        return 32767;
    }

    private boolean canInsertHotbarStack(ItemStack stack, int viewIndex) {
        if (stack.isEmpty()) return true;
        if (!isStorageIndex(viewIndex)) return false;
        ItemStack slotStack = view.getItem(viewIndex);
        if (!slotStack.isEmpty()) {
            if (!ItemStack.isSameItemSameTags(slotStack, stack)) return false;
            int max = storageLimit(slotStack);
            return slotStack.getCount() < max;
        }
        return storageLimit(stack) > 0;
    }

    private ItemStack insertHotbarStack(ItemStack stack, int viewIndex) {
        if (stack.isEmpty()) return ItemStack.EMPTY;
        ItemStack slotStack = view.getItem(viewIndex);
        ItemStack remaining = stack.copy();
        if (slotStack.isEmpty()) {
            ItemStack place = remaining.copy();
            place.setCount(Math.min(storageLimit(remaining), remaining.getCount()));
            view.setItem(viewIndex, place);
            remaining.shrink(place.getCount());
        } else if (ItemStack.isSameItemSameTags(slotStack, remaining)) {
            int max = storageLimit(slotStack);
            int free = max - slotStack.getCount();
            if (free > 0) {
                int move = Math.min(free, remaining.getCount());
                slotStack.grow(move);
                view.setItem(viewIndex, slotStack);
                remaining.shrink(move);
            }
        }
        return remaining;
    }

    private void persistPage(GuildsManager gm, net.minecraft.core.NonNullList<ItemStack> storage) {
        int start = page * 54;
        for (int i = 0; i < 54 && (start + i) < storage.size(); i++) {
            if (!mirrored[i]) continue;
            storage.set(start + i, encodeForStorage(view.getItem(i)));
        }
    }

    private void enforceCapacity(ServerPlayer sp, GuildsManager gm, net.minecraft.core.NonNullList<ItemStack> storage) {
        int cap = gm.getStorageCapacityItems(guild.getName());
        int total = decodedTotal(storage);
        if (total <= cap) return;
        int toRefund = total - cap;
        for (int idx = storage.size() - 1; idx >= 0 && toRefund > 0; idx--) {
            ItemStack enc = storage.get(idx);
            if (enc.isEmpty()) continue;
            int trueCount = trueCountOf(enc);
            if (trueCount <= 0) { storage.set(idx, ItemStack.EMPTY); continue; }
            int take = Math.min(toRefund, Math.min(trueCount, 64));
            if (take <= 0) continue;
            ItemStack refund = enc.copy();
            if (refund.hasTag()) refund.getTag().remove(EXT_TAG);
            refund.setCount(take);
            int left = trueCount - take;
            if (left <= 0) storage.set(idx, ItemStack.EMPTY);
            else storage.set(idx, encodedWithTrueCount(enc, left));
            if (!sp.getInventory().add(refund)) sp.drop(refund, false);
            toRefund -= take;
        }
        rebuild();
    }
    private int decodedTotal(net.minecraft.core.NonNullList<ItemStack> storage) {
        int sum = 0;
        for (ItemStack s : storage) {
            if (s.isEmpty()) continue;
            int c = s.getCount();
            if (s.hasTag() && s.getTag().contains(EXT_TAG)) c = Math.max(c, s.getTag().getInt(EXT_TAG));
            sum += c;
        }
        return sum;
    }

    // -------- Extended count encode/decode for persistence (>64 stacks) --------
    private static final String EXT_TAG = "gd_ext_count";

    private ItemStack encodeForStorage(ItemStack viewStack) {
        if (viewStack == null || viewStack.isEmpty()) return ItemStack.EMPTY;
        ItemStack out = viewStack.copy();
        int limit = storageLimit(out);
        if (limit > 0 && out.getCount() > limit) out.setCount(limit);
        int cnt = out.getCount();
        if (cnt > 64) {
            out.getOrCreateTag().putInt(EXT_TAG, cnt);
            out.setCount(64);
        } else if (out.hasTag() && out.getTag().contains(EXT_TAG)) {
            // remove tag if not needed
            out.getTag().remove(EXT_TAG);
        }
        return out;
    }

    private ItemStack decodeFromStorage(ItemStack stored) {
        if (stored == null || stored.isEmpty()) return ItemStack.EMPTY;
        ItemStack copy = stored.copy();
        int ext = 0;
        if (copy.hasTag() && copy.getTag().contains(EXT_TAG)) {
            ext = copy.getTag().getInt(EXT_TAG);
        }
        int limit = storageLimit(copy);
        int desired = copy.getCount();
        if (ext > 64) desired = ext;
        if (limit > 0) desired = Math.min(desired, limit);
        copy.setCount(desired);
        // Do not keep EXT_TAG in the view/cursor path to avoid leaking to player inventory
        if (copy.hasTag()) copy.getTag().remove(EXT_TAG);
        return copy;
    }

    private int trueCountOf(ItemStack encoded) {
        if (encoded == null || encoded.isEmpty()) return 0;
        int base = encoded.getCount();
        if (encoded.hasTag() && encoded.getTag().contains(EXT_TAG)) {
            int ext = encoded.getTag().getInt(EXT_TAG);
            base = Math.max(base, ext);
        }
        int limit = storageLimit(encoded);
        if (limit > 0) base = Math.min(base, limit);
        return base;
    }

    private ItemStack encodedWithTrueCount(ItemStack like, int trueCount) {
        if (like == null) return ItemStack.EMPTY;
        ItemStack out = like.copy();
        if (out.hasTag()) out.getTag().remove(EXT_TAG);
        int limit = storageLimit(out);
        if (limit > 0) trueCount = Math.min(trueCount, limit);
        out.setCount(trueCount);
        return encodeForStorage(out);
    }

    private void stripExtTag(ItemStack s) {
        if (s != null && s.hasTag()) s.getTag().remove(EXT_TAG);
    }

    private void migrateControlSlotItems(net.minecraft.core.NonNullList<ItemStack> storage, int start) {
        int[] reserved = {SLOT_BACK, SLOT_NEXT, SLOT_FILTER, SLOT_SORT, SLOT_LOG, SLOT_UPGRADE};
        java.util.Set<Integer> reservedSet = new java.util.HashSet<>();
        for (int r : reserved) reservedSet.add(r);
        // Gather items from reserved slots on this page
        java.util.List<ItemStack> toReinsert = new java.util.ArrayList<>();
        for (int i = 0; i < 54; i++) {
            if (!reservedSet.contains(i)) continue;
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
            ItemStack dec = decodeFromStorage(enc);
            int remaining = dec.getCount();
            // First pass: merge
            for (int i = 0; i < 54 && remaining > 0; i++) {
                if (reservedSet.contains(i)) continue;
                int idx = start + i;
                if (idx >= storage.size()) break;
                ItemStack exist = decodeFromStorage(storage.get(idx));
                if (!exist.isEmpty() && ItemStack.isSameItemSameTags(exist, dec)) {
                    int free = storageLimit(exist) - exist.getCount();
                    if (free > 0) {
                        int move = Math.min(free, remaining);
                        exist.grow(move);
                        remaining -= move;
                        storage.set(idx, encodeForStorage(exist));
                    }
                }
            }
            // Second pass: empty slots
            for (int i = 0; i < 54 && remaining > 0; i++) {
                if (reservedSet.contains(i)) continue;
                int idx = start + i;
                if (idx >= storage.size()) break;
                ItemStack exist = storage.get(idx);
                if (exist == null || exist.isEmpty()) {
                    int put = Math.min(storageLimit(dec), remaining);
                    ItemStack place = dec.copy(); place.setCount(put);
                    storage.set(idx, encodeForStorage(place));
                    remaining -= put;
                }
            }
            // If still remaining, append to next pages if exist
            int idx = start + 54;
            while (remaining > 0 && idx < storage.size()) {
                int rel = idx % 54;
                if (!reservedSet.contains(rel)) {
                    ItemStack exist = decodeFromStorage(storage.get(idx));
                    if (exist.isEmpty()) {
                        int put = Math.min(storageLimit(dec), remaining);
                        ItemStack place = dec.copy(); place.setCount(put);
                        storage.set(idx, encodeForStorage(place));
                        remaining -= put;
                    } else if (ItemStack.isSameItemSameTags(exist, dec)) {
                        int free = storageLimit(exist) - exist.getCount();
                        if (free > 0) {
                            int move = Math.min(free, remaining);
                            exist.grow(move);
                            remaining -= move;
                            storage.set(idx, encodeForStorage(exist));
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
        class Key { final net.minecraft.world.item.Item item; final net.minecraft.nbt.CompoundTag tag; Key(ItemStack s){ this.item=s.getItem(); this.tag=s.getTag()==null?null:s.getTag().copy(); }
            @Override public boolean equals(Object o){ if(this==o) return true; if(!(o instanceof Key k)) return false; if(item!=k.item) return false; return java.util.Objects.equals(tag,k.tag);} @Override public int hashCode(){ return java.util.Objects.hash(item, tag==null?0:tag.hashCode()); } }
        java.util.Map<Key, Integer> totals = new java.util.HashMap<>();
        java.util.Map<Key, ItemStack> reps = new java.util.HashMap<>();
        for (ItemStack s : storage) {
            if (s == null || s.isEmpty()) continue;
            ItemStack decoded = decodeFromStorage(s);
            Key k = new Key(decoded);
            totals.merge(k, decoded.getCount(), Integer::sum);
            reps.putIfAbsent(k, decoded.copy());
        }
        // Capacity pre-check: ensure enough slots when respecting per-item limits (e.g., unstackables=1)
        int requiredSlots = 0;
        for (java.util.Map.Entry<Key, Integer> e : totals.entrySet()) {
            ItemStack rep = reps.get(e.getKey());
            int limit = storageLimit(rep);
            if (limit <= 0) limit = 1;
            int cnt = Math.max(0, e.getValue());
            requiredSlots += (cnt + limit - 1) / limit;
        }
        if (requiredSlots > storage.size()) {
            sp.sendSystemMessage(Component.literal("§cOrganizar cancelado: itens demais para a capacidade atual (" + requiredSlots + "/" + storage.size() + ")."));
            return;
        }
        java.util.List<Key> keys = new java.util.ArrayList<>(totals.keySet());
        keys.sort((a,b) -> {
            int ca = categoryOf(reps.get(a));
            int cb = categoryOf(reps.get(b));
            if (ca != cb) return Integer.compare(ca, cb);
            String ia = java.util.Objects.toString(net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(reps.get(a).getItem()));
            String ib = java.util.Objects.toString(net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(reps.get(b).getItem()));
            int c = ia.compareTo(ib);
            if (c != 0) return c;
            String ta = reps.get(a).getTag()==null?"":reps.get(a).getTag().toString();
            String tb = reps.get(b).getTag()==null?"":reps.get(b).getTag().toString();
            return ta.compareTo(tb);
        });
        // Refill storage sequentially
        int idx = 0;
        for (Key k : keys) {
            int cnt = totals.getOrDefault(k, 0);
            ItemStack base = reps.get(k).copy();
            base.setCount(1); // ensure copy of NBT
            while (cnt > 0 && idx < storage.size()) {
                ItemStack toPut = base.copy();
                int limit = storageLimit(toPut);
                int put = Math.min(limit, cnt);
                toPut.setCount(put);
                storage.set(idx++, encodeForStorage(toPut));
                cnt -= put;
            }
        }
        while (idx < storage.size()) storage.set(idx++, ItemStack.EMPTY);
        gm.setDirty();
    }

    private int categoryOf(ItemStack s) {
        net.minecraft.world.item.Item i = s.getItem();
        if (i instanceof net.minecraft.world.item.BlockItem) return 0;
        if (i instanceof net.minecraft.world.item.ArmorItem) return 1;
        if (i instanceof net.minecraft.world.item.SwordItem || i instanceof net.minecraft.world.item.BowItem || i instanceof net.minecraft.world.item.CrossbowItem || i instanceof net.minecraft.world.item.TridentItem) return 2;
        if (i instanceof net.minecraft.world.item.TieredItem) return 3;
        if (s.isEdible()) return 4;
        return 5;
    }

    @Override
    public boolean stillValid(@NotNull Player p) { return true; }

    // Container that supports large stack sizes to avoid clamping to 64 inside SimpleContainer
    private static class LargeStackContainer extends SimpleContainer {
        public LargeStackContainer(int size) { super(size); }
        @Override public int getMaxStackSize() { return 32767; }
    }

    // Custom slot for guild storage that allows large stacks and blocks control slots
    private class StorageSlot extends Slot {
        private final int slotIndex;
        public StorageSlot(Container cont, int index, int x, int y) {
            super(cont, index, x, y);
            this.slotIndex = index;
        }
        @Override public boolean mayPlace(@NotNull ItemStack s) {
            return slotIndex != SLOT_BACK && slotIndex != SLOT_NEXT && slotIndex != SLOT_FILTER && slotIndex != SLOT_LOG && slotIndex != SLOT_UPGRADE;
        }
        @Override public boolean mayPickup(@NotNull Player p) {
            return slotIndex != SLOT_BACK && slotIndex != SLOT_NEXT && slotIndex != SLOT_FILTER && slotIndex != SLOT_LOG && slotIndex != SLOT_UPGRADE;
        }
        @Override public int getMaxStackSize() { return 32767; }
        @Override public int getMaxStackSize(@NotNull ItemStack stack) { return storageLimit(stack); }
    }
}


