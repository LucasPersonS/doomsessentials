package org.lupz.doomsdayessentials.event.eclipse.market;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * A special item representing a bundle of multiple items for market trades.
 * When used, it unpacks its contents into the player's inventory.
 */
public class BlackMarketBundleItem extends Item {
    public static final String TAG_ITEMS = "BMItems";
    public static final String TAG_ALIAS = "Alias";

    public BlackMarketBundleItem(Properties props){ super(props); }

    /** Create a bundle stack with embedded items. */
    public static ItemStack createBundle(String alias, List<ItemStackSpec> items){
        ItemStack stack = new ItemStack(org.lupz.doomsdayessentials.item.ModItems.BLACK_MARKET_BUNDLE.get());
        // Build NBT payload
        CompoundTag tag = stack.getOrCreateTag();
        ListTag list = new ListTag();
        for (ItemStackSpec spec : items){
            net.minecraft.world.item.Item item = net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(spec.id);
            if (item == null) continue;
            ItemStack s = new ItemStack(item, Math.max(1, spec.count));
            if (spec.nbt != null) s.setTag(spec.nbt.copy());
            CompoundTag sTag = new CompoundTag();
            s.save(sTag);
            list.add(sTag);
        }
        tag.put(TAG_ITEMS, list);
        if (alias != null) tag.putString(TAG_ALIAS, alias);
        stack.setTag(tag);
        return stack;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand){
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide){
            CompoundTag tag = stack.getTag();
            if (tag != null && tag.contains(TAG_ITEMS, Tag.TAG_LIST)){
                ListTag list = tag.getList(TAG_ITEMS, Tag.TAG_COMPOUND);
                for (int i=0;i<list.size();i++){
                    CompoundTag sTag = list.getCompound(i);
                    ItemStack s = ItemStack.of(sTag);
                    boolean added = player.addItem(s);
                    if (!added){
                        player.drop(s, false);
                    }
                }
                stack.shrink(1);
            }
        }
        return new InteractionResultHolder<>(InteractionResult.SUCCESS, stack);
    }

    @Override
    public Component getName(ItemStack stack){
        CompoundTag tag = stack.getTag();
        if (tag != null && tag.contains(TAG_ALIAS, Tag.TAG_STRING)){
            return Component.literal("Pacote: " + tag.getString(TAG_ALIAS));
        }
        return super.getName(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag){
        CompoundTag tag = stack.getTag();
        if (tag != null && tag.contains(TAG_ITEMS, Tag.TAG_LIST)){
            ListTag list = tag.getList(TAG_ITEMS, Tag.TAG_COMPOUND);
            int show = Math.min(5, list.size());
            for (int i=0;i<show;i++){
                ItemStack s = ItemStack.of(list.getCompound(i));
                tooltip.add(Component.literal(" - " + s.getCount() + "x " + s.getHoverName().getString()));
            }
            if (list.size() > show) tooltip.add(Component.literal("..."));
        }
    }
}
