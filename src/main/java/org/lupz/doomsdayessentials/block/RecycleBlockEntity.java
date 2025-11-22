package org.lupz.doomsdayessentials.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.client.Minecraft;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lupz.doomsdayessentials.menu.RecycleMenu;
import org.lupz.doomsdayessentials.recycler.RecycleRecipeManager;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.api.distmarker.Dist;

import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import org.lupz.doomsdayessentials.config.EssentialsConfig;

public class RecycleBlockEntity extends BlockEntity implements MenuProvider, Container {
    private final NonNullList<ItemStack> items = NonNullList.withSize(10, ItemStack.EMPTY); // 0-4 input, 5-9 output
    private static int getProcessMs() {
        int seconds = EssentialsConfig.RECYCLER_PROCESS_SECONDS.get();
        if (seconds < 1) seconds = 1;
        return seconds * 1000;
    }
    private boolean enabled = true;
    public boolean isEnabled(){return enabled;}
    public void toggle(){
        this.enabled = !this.enabled;
        setChanged();
        if (!enabled) {
            cancelTask();
            setRunning(false);
        } else {
            scheduleIfPossible();
        }
    }

    public RecycleBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.RECYCLE_BLOCK_ENTITY.get(), pos, state);
    }

    private static final ScheduledExecutorService SCHEDULER = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "RecyclerScheduler");
        t.setDaemon(true);
        return t;
    });
    private ScheduledFuture<?> scheduled;
    private long finishAtMs = 0L;

    private boolean hasInput() {
        for (int i = 0; i < 5; i++) if (!items.get(i).isEmpty()) return true;
        return false;
    }

    private boolean hasSpaceForOutput(java.util.List<ItemStack> stacks) {
        if (stacks.isEmpty()) return true;
        SimpleContainer dummy = new SimpleContainer(5);
        for(int i = 0; i < 5; i++) dummy.setItem(i, items.get(i+5).copy());

        for(ItemStack toOutput : stacks) {
            boolean foundSpace = false;
            for(int i = 0; i < 5; i++) {
                ItemStack existing = dummy.getItem(i);
                if (existing.isEmpty()) {
                    dummy.setItem(i, toOutput.copy());
                    foundSpace = true;
                    break;
                }
                if (ItemStack.isSameItemSameTags(existing, toOutput) && existing.getCount() + toOutput.getCount() <= existing.getMaxStackSize()) {
                    existing.grow(toOutput.getCount());
                    foundSpace = true;
                    break;
                }
            }
            if(!foundSpace) return false;
        }
        return true;
    }

    private boolean canProcess() {
        for (int i = 0; i < 5; i++) {
            ItemStack in = items.get(i);
            if (in.isEmpty()) continue;

            Optional<RecycleRecipeManager.Recipe> recipe = RecycleRecipeManager.getRecipe(in);
            if (recipe.isPresent()) {
                java.util.List<ItemStack> results = recipe.get().getOutputStacks();
                if (!results.isEmpty() && hasSpaceForOutput(results)) {
                    return true;
                }
            }
        }
        return false;
    }

    private void addOutput(java.util.List<ItemStack> stacks) {
        for(ItemStack toOutput : stacks){
            for (int i = 5; i < 10; i++) {
                ItemStack out = items.get(i);
                if (out.isEmpty()) {
                    items.set(i, toOutput.copy());
                    break; // Next item
                }
                if (ItemStack.isSameItemSameTags(out, toOutput) && out.getCount() + toOutput.getCount() <= out.getMaxStackSize()) {
                    out.grow(toOutput.getCount());
                    break; // Next item
                }
            }
        }
    }

    private void processOnce() {
        for (int i = 0; i < 5; i++) {
            ItemStack in = items.get(i);
            if (in.isEmpty()) continue;

            Optional<RecycleRecipeManager.Recipe> recipe = RecycleRecipeManager.getRecipe(in);
            if (recipe.isEmpty()) continue;

            java.util.List<ItemStack> results = recipe.get().getOutputStacks();
            if (results.isEmpty() || !hasSpaceForOutput(results)) continue;

            in.shrink(1);
            addOutput(results);
            return;
        }
    }

    private void setRunning(boolean run) {
        if (level == null) return;
        BlockState st = getBlockState();
        if (st.getValue(RecycleBlock.RUNNING) != run) {
            level.setBlock(worldPosition, st.setValue(RecycleBlock.RUNNING, run), 3);
        }
    }

    private void cancelTask() {
        if (scheduled != null) {
            scheduled.cancel(false);
            scheduled = null;
        }
        finishAtMs = 0L;
    }

    private void scheduleIfPossible() {
        if (level == null || level.isClientSide) return;
        if (!enabled) { setRunning(false); return; }
        if (scheduled != null && !scheduled.isDone()) return;
        if (!hasInput()) { setRunning(false); return; }
        if (!canProcess()) { setRunning(false); return; }
        setRunning(true);
        int ms = getProcessMs();
        finishAtMs = System.currentTimeMillis() + ms;
        scheduled = SCHEDULER.schedule(this::completeProcess, ms, TimeUnit.MILLISECONDS);
    }

    private void completeProcess() {
        if (level == null) { scheduled = null; return; }
        var srv = level.getServer();
        if (srv == null) { scheduled = null; return; }
        srv.execute(() -> {
            if (this.isRemoved()) { scheduled = null; return; }
            if (!enabled) { scheduled = null; setRunning(false); return; }
            if (!canProcess()) { scheduled = null; setRunning(false); return; }
            processOnce();
            setChanged();
            scheduled = null;
            finishAtMs = 0L;
            if (hasInput() && canProcess()) {
                scheduleIfPossible();
            } else {
                setRunning(false);
            }
        });
    }

    // ---------------------------------------------------------------------
    // Menu / container
    // ---------------------------------------------------------------------

    @Override
    public @NotNull AbstractContainerMenu createMenu(int windowId, @NotNull Inventory inv, @NotNull Player player) {
        return new RecycleMenu(windowId, inv, this);
    }

    @Override public @NotNull net.minecraft.network.chat.Component getDisplayName() { return net.minecraft.network.chat.Component.literal("Recycler"); }

    // ---------------------------------------------------------------------
    // Container implementation
    // ---------------------------------------------------------------------

    @Override public int getContainerSize() { return 10; }
    @Override public boolean isEmpty() { return items.stream().allMatch(ItemStack::isEmpty); }
    @Override public @NotNull ItemStack getItem(int index) { return items.get(index); }
    @Override public @NotNull ItemStack removeItem(int index, int count) { ItemStack res = ContainerHelper.removeItem(items, index, count); if(!res.isEmpty()) { setChanged(); scheduleIfPossible(); } return res; }
    @Override public @NotNull ItemStack removeItemNoUpdate(int index) { ItemStack res = items.get(index); items.set(index, ItemStack.EMPTY); scheduleIfPossible(); return res; }
    @Override public void setItem(int index, @NotNull ItemStack stack) { items.set(index, stack); if(stack.getCount() > getMaxStackSize()) stack.setCount(getMaxStackSize()); setChanged(); scheduleIfPossible(); }
    @Override public boolean stillValid(@NotNull Player player) { return true; }
    @Override public void clearContent() { items.clear(); scheduleIfPossible(); }

    // ---------------------------------------------------------------------
    // NBT
    // ---------------------------------------------------------------------
    @Override protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        net.minecraft.nbt.ListTag list = new net.minecraft.nbt.ListTag();
        for (ItemStack s : items) list.add(s.save(new CompoundTag()));
        tag.put("Items", list);
        tag.putBoolean("Enabled", enabled);
        tag.putLong("FinishAtMs", finishAtMs);
    }

    @Override public void load(CompoundTag tag) {
        super.load(tag);
        net.minecraft.nbt.ListTag list = tag.getList("Items", 10);
        for (int i = 0; i < items.size(); i++) items.set(i, ItemStack.EMPTY);
        for (int i = 0; i < list.size() && i < items.size(); i++) items.set(i, ItemStack.of(list.getCompound(i)));
        enabled = tag.contains("Enabled")? tag.getBoolean("Enabled") : true;
        finishAtMs = tag.contains("FinishAtMs") ? tag.getLong("FinishAtMs") : 0L;
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && !level.isClientSide) {
            if (enabled && hasInput() && canProcess()) {
                long now = System.currentTimeMillis();
                if (finishAtMs > now) {
                long delay = Math.max(1L, finishAtMs - now);
                scheduled = SCHEDULER.schedule(this::completeProcess, delay, TimeUnit.MILLISECONDS);
                setRunning(true);
            } else if (finishAtMs != 0L) {
                completeProcess();
            } else {
                scheduleIfPossible();
            }
            } else {
                setRunning(false);
            }
        }
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        cancelTask();
    }

}
