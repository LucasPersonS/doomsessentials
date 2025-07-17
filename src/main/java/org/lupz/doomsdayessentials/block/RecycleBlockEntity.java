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
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.client.Minecraft;
import org.lupz.doomsdayessentials.client.RecyclerLoopSound;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lupz.doomsdayessentials.menu.RecycleMenu;
import org.lupz.doomsdayessentials.recycler.RecycleRecipeManager;

import java.util.Optional;

public class RecycleBlockEntity extends BlockEntity implements MenuProvider, Container {
    private final NonNullList<ItemStack> items = NonNullList.withSize(10, ItemStack.EMPTY); // 0-4 input, 5-9 output
    private int progress = 0;
    private static final int MAX_PROGRESS = 200; // 10 seconds at 20 t/s
    private boolean enabled = true;
    public boolean isEnabled(){return enabled;}
    public void toggle(){
        this.enabled = !this.enabled;
        setChanged();
    }

    public RecycleBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.RECYCLE_BLOCK_ENTITY.get(), pos, state);
    }

    // ---------------------------------------------------------------------
    // Processing logic
    // ---------------------------------------------------------------------

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
                    return true; // Found a processable item
                }
            }
        }
        return false; // No item can be processed
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
            // We processed one item, so we're done for this cycle.
            // The tick method will reset progress and it will start again if there is more input.
            return;
        }
    }

    public static void tick(Level level, BlockPos pos, BlockState state, RecycleBlockEntity be) {
        if(level.isClientSide) {
            be.clientTick(level, pos, state, be);
            return;
        }

        boolean hasInput = be.hasInput();
        if (!hasInput) {
            be.progress = 0;
            if (state.getValue(RecycleBlock.RUNNING)) {
                level.setBlock(pos, state.setValue(RecycleBlock.RUNNING, false), 3);
            }
            return;
        }

        // The machine should only be "running" if it has a valid operation to perform.
        boolean canProcess = be.canProcess();
        boolean running = be.enabled && canProcess;

        if(state.getValue(RecycleBlock.RUNNING) != running){
            level.setBlock(pos, state.setValue(RecycleBlock.RUNNING, running), 3);
        }

        if (running) {
            be.progress++;
            if (be.progress >= MAX_PROGRESS) {
                be.progress = 0;
                be.processOnce();
                be.setChanged();
            }
        } else {
            be.progress = 0;
        }
    }

    private RecyclerLoopSound loopSound;
    private void clientTick(Level level, BlockPos pos, BlockState state, RecycleBlockEntity be){
        if(state.getValue(RecycleBlock.RUNNING)){
            if(loopSound == null || loopSound.isStopped()){
                if(loopSound != null) Minecraft.getInstance().getSoundManager().stop(loopSound);
                loopSound = new RecyclerLoopSound(pos);
                Minecraft.getInstance().getSoundManager().play(loopSound);
            }
        } else {
            if(loopSound != null){
                Minecraft.getInstance().getSoundManager().stop(loopSound);
                loopSound = null;
            }
        }
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
    @Override public @NotNull ItemStack removeItem(int index, int count) { ItemStack res = ContainerHelper.removeItem(items, index, count); if(!res.isEmpty()) setChanged(); return res; }
    @Override public @NotNull ItemStack removeItemNoUpdate(int index) { ItemStack res = items.get(index); items.set(index, ItemStack.EMPTY); return res; }
    @Override public void setItem(int index, @NotNull ItemStack stack) { items.set(index, stack); if(stack.getCount() > getMaxStackSize()) stack.setCount(getMaxStackSize()); setChanged(); }
    @Override public boolean stillValid(@NotNull Player player) { return true; }
    @Override public void clearContent() { items.clear(); }

    // ---------------------------------------------------------------------
    // NBT
    // ---------------------------------------------------------------------
    @Override protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        net.minecraft.nbt.ListTag list = new net.minecraft.nbt.ListTag();
        for (ItemStack s : items) list.add(s.save(new CompoundTag()));
        tag.put("Items", list);
        tag.putInt("Progress", progress);
        tag.putBoolean("Enabled", enabled);
    }

    @Override public void load(CompoundTag tag) {
        super.load(tag);
        net.minecraft.nbt.ListTag list = tag.getList("Items", 10);
        for (int i = 0; i < items.size(); i++) items.set(i, ItemStack.EMPTY);
        for (int i = 0; i < list.size() && i < items.size(); i++) items.set(i, ItemStack.of(list.getCompound(i)));
        progress = tag.getInt("Progress");
        enabled = tag.contains("Enabled")? tag.getBoolean("Enabled") : true;
    }

    // No per-instance ticker implementation; static tick used via lambda in block class.
} 