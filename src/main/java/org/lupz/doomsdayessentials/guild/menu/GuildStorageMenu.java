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
    private boolean allowControlAction = false;
    private static final int SLOT_BACK = 45;
    private static final int SLOT_NEXT = 53;
    private static final int SLOT_FILTER = 46; // toggle
    private static final int SLOT_SORT = 47;   // organize button
    private static final int SLOT_LOG = 52; // open logs
    private static final int SLOT_UPGRADE = 50; // lower right book
    public enum Filter { ALL, BLOCKS, ITEMS, TOOLS, WEAPONS, ARMOR, FOOD, POTIONS, ENCHANTED }
    private String searchText = "";
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
    public void setAllowControlAction(boolean allow) {
        this.allowControlAction = allow;
    }
    
    public void setSearchText(String text) {
        this.searchText = text.toLowerCase();
        rebuild();
        broadcastChanges();
    }
    
    public int getCurrentPage() {
        return page;
    }
    
    public int getMaxPages() {
        if (guild == null) return 1;
        GuildsManager gm = GuildsManager.get(((ServerPlayer)player).serverLevel());
        var storage = gm.getOrCreateStorage(guild.getName());
        return Math.max(1, (storage.size() + 53) / 54);
    }
    
    public int getUsedItems() {
        if (guild == null) return 0;
        GuildsManager gm = GuildsManager.get(((ServerPlayer)player).serverLevel());
        var storage = gm.getOrCreateStorage(guild.getName());
        int totalItems = 0;
        for (ItemStack stack : storage) {
            if (!stack.isEmpty()) {
                totalItems += trueCountOf(stack);
            }
        }
        return totalItems;
    }
    
    public int getMaxCapacity() {
        if (guild == null) return 5000;
        GuildsManager gm = GuildsManager.get(((ServerPlayer)player).serverLevel());
        return gm.getStorageCapacityItems(guild.getName());
    }
    
    public int getStorageLevel() {
        if (guild == null) return 1;
        return guild.getStorageLevel();
    }
    
    public Filter getFilter() {
        return filter;
    }
    private void rebuild() {
        if (!(player instanceof ServerPlayer sp) || guild == null) return;
        GuildsManager gm = GuildsManager.get(sp.serverLevel());
        var storage = gm.getOrCreateStorage(guild.getName());
        
        // If searching, build filtered and sorted view
        if (!searchText.isEmpty() || filter != Filter.ALL) {
            rebuildFilteredView(gm, storage);
        } else {
            // Normal pagination view
            int start = page * 54;
            java.util.Arrays.fill(mirrored, false);
            for (int i = 0; i < 54; i++) {
                ItemStack s = (start + i) < storage.size() ? storage.get(start + i) : ItemStack.EMPTY;
                view.setItem(i, decodeFromStorage(s));
                mirrored[i] = true;
            }
        }
    }
    
    private void rebuildFilteredView(GuildsManager gm, java.util.List<ItemStack> storage) {
        java.util.Arrays.fill(mirrored, false);
        // Clear view first
        for (int i = 0; i < 54; i++) {
            view.setItem(i, ItemStack.EMPTY);
        }
        
        // Collect all matching items
        java.util.List<ItemStack> filtered = new java.util.ArrayList<>();
        for (int i = 0; i < storage.size(); i++) {
            ItemStack s = storage.get(i);
            if (!s.isEmpty() && matchesFilter(s) && matchesSearch(s)) {
                ItemStack copy = decodeFromStorage(s);
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
                    item.getTag().remove("_storageIndex");
                }
                view.setItem(viewSlot, item);
                mirrored[viewSlot] = true;
                viewSlot++;
            }
        }
    }
    
    private boolean matchesSearch(ItemStack stack) {
        if (searchText.isEmpty()) return true;
        if (stack.isEmpty()) return false;
        
        String itemName = stack.getHoverName().getString().toLowerCase();
        String itemId = net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(stack.getItem()).toString().toLowerCase();
        
        return itemName.contains(searchText) || itemId.contains(searchText);
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
        // Control actions only execute when called from external buttons via packet
        if (allowControlAction && slotId == SLOT_BACK) {
            // Back to main menu if on first page; otherwise previous page
            if (page == 0) {
                sp.openMenu(new net.minecraft.world.SimpleMenuProvider((id, inv, p) -> new GuildMainMenu(id, inv), Component.literal("Organização")));
                return;
            } else {
                page--;
                rebuild();
                broadcastChanges();
                return;
            }
        }
        if (allowControlAction && slotId == SLOT_NEXT) {
            // Calculate max pages properly
            int maxPages = Math.max(1, (storage.size() + 53) / 54);
            if (page < maxPages - 1) {
                page++;
                rebuild();
                broadcastChanges();
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
            rebuild(); broadcastChanges(); return;
        }
        if (allowControlAction && slotId == SLOT_SORT) {
            sortEntireStorage(sp);
            rebuild(); broadcastChanges();
            return;
        }
        if (allowControlAction && slotId == SLOT_LOG) {
            sp.openMenu(new net.minecraft.world.SimpleMenuProvider((id, inv, p) -> new org.lupz.doomsdayessentials.guild.menu.GuildStorageLogMenu(id, inv, guild.getName()), Component.literal("HistÃ³rico do Cofre")));
            return;
        }
        if (allowControlAction && slotId == SLOT_UPGRADE) {
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
            // Update the guild object to reflect new level
            this.guild = gm.getGuildByMember(sp.getUUID());
            rebuild();
            broadcastChanges();
            return;
        }
        // Handle pickup (non-shift) with improved anti-dupe protection
        if (clickType == ClickType.PICKUP && slotId >= 0 && slotId < 54 && mirrored[slotId]) {
            // Validate storage state before any operations
            persistPage(gm, storage);
            
            ItemStack inView = view.getItem(slotId).copy(); // Work with copies to prevent reference issues
            ItemStack carried = this.getCarried().copy();
            int start2 = page * 54;
            int button = dragType; // 0 = left, 1 = right
            
            // Anti-dupe: Validate item counts
            if (!inView.isEmpty() && trueCountOf(inView) > storageLimit(inView)) {
                inView.setCount(storageLimit(inView));
            }
            if (!carried.isEmpty() && carried.getCount() > 64) {
                carried.setCount(64);
            }
            
            if (carried.isEmpty() && !inView.isEmpty()) {
                // Take from slot
                int toTake = (button == 1) ? ((inView.getCount() + 1) / 2) : Math.min(64, inView.getCount());
                if (toTake > 0) {
                    ItemStack taken = inView.copy();
                    taken.setCount(toTake);
                    stripExtTag(taken);
                    this.setCarried(taken);
                    inView.shrink(toTake);
                    view.setItem(slotId, inView.isEmpty() ? ItemStack.EMPTY : inView);
                    storage.set(start2 + slotId, encodeForStorage(inView));
                    gm.setDirty();
                    broadcastChanges();
                }
            } else if (!carried.isEmpty() && inView.isEmpty()) {
                // Place into empty slot
                int toPlace = (button == 1) ? 1 : carried.getCount();
                toPlace = Math.min(toPlace, storageLimit(carried));
                if (toPlace > 0) {
                    ItemStack placing = carried.copy();
                    placing.setCount(toPlace);
                    view.setItem(slotId, placing);
                    carried.shrink(toPlace);
                    this.setCarried(carried.isEmpty() ? ItemStack.EMPTY : carried);
                    storage.set(start2 + slotId, encodeForStorage(placing));
                    gm.setDirty();
                    broadcastChanges();
                }
            } else if (!carried.isEmpty() && !inView.isEmpty() && ItemStack.isSameItemSameTags(carried, inView)) {
                // Merge into slot
                int limit = storageLimit(inView);
                int free = limit - inView.getCount();
                int toAdd = (button == 1) ? 1 : Math.min(carried.getCount(), free);
                if (toAdd > 0) {
                    inView.grow(toAdd);
                    carried.shrink(toAdd);
                    view.setItem(slotId, inView);
                    this.setCarried(carried.isEmpty() ? ItemStack.EMPTY : carried);
                    storage.set(start2 + slotId, encodeForStorage(inView));
                    gm.setDirty();
                    broadcastChanges();
                }
            } else if (!carried.isEmpty() && !inView.isEmpty()) {
                // Swap items
                ItemStack temp = carried.copy();
                stripExtTag(temp);
                ItemStack viewCopy = inView.copy();
                stripExtTag(viewCopy);
                this.setCarried(viewCopy);
                view.setItem(slotId, temp);
                storage.set(start2 + slotId, encodeForStorage(temp));
                gm.setDirty();
                broadcastChanges();
            }
            
            // Anti-dupe: Enforce capacity limits after operation
            enforceCapacity(sp, gm, storage);
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
        // Shift-click handling with improved anti-dupe
        if (index < 54 && mirrored[index]) {
            // From storage => withdraw to player
            if (!(player instanceof ServerPlayer sp) || guild == null) return ItemStack.EMPTY;
            
            ItemStack slotStack = view.getItem(index).copy(); // Work with copy
            if (slotStack.isEmpty()) return ItemStack.EMPTY;
            
            // Anti-dupe: Validate count
            if (slotStack.getCount() > storageLimit(slotStack)) {
                slotStack.setCount(storageLimit(slotStack));
            }
            
            int toExtract = Math.min(slotStack.getCount(), 64);
            ItemStack extracted = slotStack.copy();
            extracted.setCount(toExtract);
            stripExtTag(extracted);
            
            if (player.getInventory().add(extracted)) {
                slotStack.shrink(toExtract);
                view.setItem(index, slotStack.isEmpty() ? ItemStack.EMPTY : slotStack);
                GuildsManager gm = GuildsManager.get(sp.serverLevel());
                var storage = gm.getOrCreateStorage(guild.getName());
                int start = page * 54;
                storage.set(start + index, encodeForStorage(slotStack));
                gm.setDirty();
                broadcastChanges();
                return ItemStack.EMPTY;
            }
        }
        
        // From player inventory => deposit to storage
        else if (index >= 54 && index < this.slots.size() && guild != null && player instanceof ServerPlayer sp) {
            Slot slot = this.slots.get(index);
            if (!slot.hasItem()) return ItemStack.EMPTY;
            ItemStack stack = slot.getItem();
            ItemStack copy = stack.copy();
            
            // Try to deposit into storage
            GuildsManager gm = GuildsManager.get(sp.serverLevel());
            var storage = gm.getOrCreateStorage(guild.getName());
            boolean changed = false;
            
            // First pass: merge with existing stacks
            for (int i = 0; i < 54; i++) {
                if (!mirrored[i]) continue;
                ItemStack viewStack = view.getItem(i);
                if (!viewStack.isEmpty() && ItemStack.isSameItemSameTags(viewStack, stack)) {
                    int limit = storageLimit(viewStack);
                    int free = limit - viewStack.getCount();
                    if (free > 0) {
                        int put = Math.min(free, stack.getCount());
                        viewStack.grow(put);
                        stack.shrink(put);
                        changed = true;
                        if (stack.isEmpty()) break;
                    }
                }
            }
            
            // Second pass: empty slots
            if (!stack.isEmpty()) {
                for (int i = 0; i < 54; i++) {
                    if (!mirrored[i]) continue;
                    ItemStack viewStack = view.getItem(i);
                    if (viewStack.isEmpty()) {
                        int put = Math.min(storageLimit(stack), stack.getCount());
                        ItemStack place = stack.copy();
                        place.setCount(put);
                        view.setItem(i, place);
                        stack.shrink(put);
                        changed = true;
                        if (stack.isEmpty()) break;
                    }
                }
            }
            
            if (changed) {
                persistPage(gm, storage);
                enforceCapacity(sp, gm, storage);
                gm.setDirty();
                broadcastChanges();
            }
            
            if (stack.isEmpty()) slot.set(ItemStack.EMPTY);
            else slot.setChanged();
            return copy;
        }
        
        return ItemStack.EMPTY;
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
        return idx >= 0 && idx < 54;
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
        // Build available fill indices (skip control slots on every page)
        java.util.List<Integer> validIndices = new java.util.ArrayList<>(storage.size());
        for (int abs = 0; abs < storage.size(); abs++) {
            int rel = abs % 54;
            if (isStorageIndex(rel)) validIndices.add(abs);
        }
        int capacity = validIndices.size();
        // Capacity pre-check: ensure enough slots when respecting per-item limits (e.g., unstackables=1)
        int requiredSlots = 0;
        for (java.util.Map.Entry<Key, Integer> e : totals.entrySet()) {
            ItemStack rep = reps.get(e.getKey());
            int limit = storageLimit(rep);
            if (limit <= 0) limit = 1;
            int cnt = Math.max(0, e.getValue());
            requiredSlots += (cnt + limit - 1) / limit;
        }
        if (requiredSlots > capacity) {
            sp.sendSystemMessage(Component.literal("§cOrganizar cancelado: itens demais para a capacidade atual (" + requiredSlots + "/" + capacity + ")."));
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
        // Prepare new storage filled only in valid indices
        net.minecraft.core.NonNullList<ItemStack> newStorage = net.minecraft.core.NonNullList.withSize(storage.size(), ItemStack.EMPTY);
        int pos = 0;
        for (Key k : keys) {
            int cnt = totals.getOrDefault(k, 0);
            ItemStack base = reps.get(k).copy();
            base.setCount(1); // ensure copy of NBT
            while (cnt > 0 && pos < capacity) {
                ItemStack toPut = base.copy();
                int limit = storageLimit(toPut);
                int put = Math.min(limit, cnt);
                toPut.setCount(put);
                int target = validIndices.get(pos++);
                newStorage.set(target, encodeForStorage(toPut));
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
        if (i instanceof net.minecraft.world.item.BlockItem) return 0;
        if (i instanceof net.minecraft.world.item.ArmorItem) return 1;
        if (i instanceof net.minecraft.world.item.SwordItem || i instanceof net.minecraft.world.item.BowItem || i instanceof net.minecraft.world.item.CrossbowItem || i instanceof net.minecraft.world.item.TridentItem) return 2;
        if (i instanceof net.minecraft.world.item.TieredItem) return 3;
        if (s.isEdible()) return 4;
        return 5;
    }
    private boolean matchesFilter(ItemStack s) {
        if (s == null || s.isEmpty()) return true;
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
            return true;
        }
        @Override public boolean mayPickup(@NotNull Player p) {
            return true;
        }
        @Override public int getMaxStackSize() { return 32767; }
        @Override public int getMaxStackSize(@NotNull ItemStack stack) { return storageLimit(stack); }
    }
}
