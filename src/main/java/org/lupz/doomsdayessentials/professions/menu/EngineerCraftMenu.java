package org.lupz.doomsdayessentials.professions.menu;

import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import org.jetbrains.annotations.NotNull;
import org.lupz.doomsdayessentials.professions.shop.EngineerShopUtil;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class EngineerCraftMenu extends AbstractContainerMenu {
    private final CraftingContainer craftSlots = new TransientCraftingContainer(this, 3, 3);
    private final ResultContainer resultSlots = new ResultContainer();
    private final Player player;

    public EngineerCraftMenu(int windowId, Inventory inv) {
        super(ProfessionMenuTypes.ENGINEER_CRAFT.get(), windowId);
        this.player = inv.player;

        this.addSlot(new ResultSlot(inv.player, this.craftSlots, this.resultSlots, 0, 124, 35) {
            @Override
            public void onTake(@NotNull Player player, @NotNull ItemStack stack) {
                findRecipeFor(stack).ifPresent(recipe -> consumeIngredients(recipe.costs()));
                super.onTake(player, stack);
            }
        });
        
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 3; ++col) {
                this.addSlot(new Slot(this.craftSlots, col + row * 3, 30 + col * 18, 17 + row * 18));
            }
        }

        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                this.addSlot(new Slot(inv, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }

        for (int col = 0; col < 9; ++col) {
            this.addSlot(new Slot(inv, col, 8 + col * 18, 142));
        }
    }

    @Override
    public void slotsChanged(@NotNull Container container) {
        if (player.level().isClientSide()) {
            return;
        }
        ServerPlayer serverPlayer = (ServerPlayer) player;
        ItemStack resultStack = findMatchingRecipe();
        resultSlots.setItem(0, resultStack);
        serverPlayer.connection.send(new ClientboundContainerSetSlotPacket(this.containerId, this.getStateId(), 0, resultStack));
    }

    private Optional<EngineerShopUtil.Entry> findRecipeFor(ItemStack result) {
        if (result.isEmpty()) {
            return Optional.empty();
        }
        return EngineerShopUtil.getEntries().values().stream()
                .filter(entry -> {
                    ItemStack output = new ItemStack(ForgeRegistries.ITEMS.getValue(entry.outputId()), entry.outputCount());
                    return ItemStack.isSameItemSameTags(output, result);
                })
                .findFirst();
    }

    private void consumeIngredients(Map<ResourceLocation, Integer> costs) {
        for (Map.Entry<ResourceLocation, Integer> cost : costs.entrySet()) {
            ResourceLocation itemToConsume = cost.getKey();
            int amountToConsume = cost.getValue();

            for (int i = 0; i < this.craftSlots.getContainerSize(); ++i) {
                ItemStack stackInSlot = this.craftSlots.getItem(i);
                if (!stackInSlot.isEmpty() && ForgeRegistries.ITEMS.getKey(stackInSlot.getItem()).equals(itemToConsume)) {
                    int toRemove = Math.min(amountToConsume, stackInSlot.getCount());
                    stackInSlot.shrink(toRemove);
                    amountToConsume -= toRemove;

                    if (amountToConsume <= 0) {
                        break; // Done with this ingredient
                    }
                }
            }
        }
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        this.clearContainer(player, this.craftSlots);
    }

    private ItemStack findMatchingRecipe() {
        for (EngineerShopUtil.Entry entry : EngineerShopUtil.getEntries().values()) {
            if (matches(entry.costs())) {
                return new ItemStack(ForgeRegistries.ITEMS.getValue(entry.outputId()), entry.outputCount());
            }
        }
        return ItemStack.EMPTY;
    }

    private boolean matches(Map<ResourceLocation, Integer> costs) {
        Map<ResourceLocation, Integer> itemsInGrid = new HashMap<>();
        for (int i = 0; i < this.craftSlots.getContainerSize(); ++i) {
            ItemStack stackInSlot = this.craftSlots.getItem(i);
            if (!stackInSlot.isEmpty()) {
                ResourceLocation id = ForgeRegistries.ITEMS.getKey(stackInSlot.getItem());
                itemsInGrid.merge(id, stackInSlot.getCount(), Integer::sum);
            }
        }
        return itemsInGrid.equals(costs);
    }

    public void onTake(Player player, ItemStack stack) {
        // Vanilla had logic for remaining items (buckets, etc). We don't need that here.
        // It was consuming extra items.
    }

    @Override
    public boolean stillValid(@NotNull Player p) {
        return true;
    }

    @Override
    public @NotNull ItemStack quickMoveStack(@NotNull Player player, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack slotStack = slot.getItem();
            itemstack = slotStack.copy();
            if (index == 0) {
                if (!this.moveItemStackTo(slotStack, 10, 46, true)) {
                    return ItemStack.EMPTY;
                }
                slot.onQuickCraft(slotStack, itemstack);
            } else if (index >= 10 && index < 46) {
                if (!this.moveItemStackTo(slotStack, 1, 10, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.moveItemStackTo(slotStack, 10, 46, false)) {
                return ItemStack.EMPTY;
            }

            if (slotStack.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }

            if (slotStack.getCount() == itemstack.getCount()) {
                return ItemStack.EMPTY;
            }

            slot.onTake(player, slotStack);
        }
        return itemstack;
    }

    @Override
    public boolean canTakeItemForPickAll(@NotNull ItemStack stack, Slot slot) {
        return slot.container != this.resultSlots && super.canTakeItemForPickAll(stack, slot);
    }
} 