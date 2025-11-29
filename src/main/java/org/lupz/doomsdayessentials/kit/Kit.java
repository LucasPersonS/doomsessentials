package org.lupz.doomsdayessentials.kit;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a kit with a tier, cooldown type, and list of items.
 */
public class Kit {
    private final String tier;
    private final CooldownType cooldownType;
    private final List<String> itemSnbts;

    public Kit(String tier, CooldownType cooldownType, List<String> itemSnbts) {
        this.tier = tier;
        this.cooldownType = cooldownType;
        this.itemSnbts = new ArrayList<>(itemSnbts);
    }

    public String getTier() {
        return tier;
    }

    public CooldownType getCooldownType() {
        return cooldownType;
    }

    public List<String> getItemSnbts() {
        return new ArrayList<>(itemSnbts);
    }

    /**
     * Converts the stored SNBT strings to ItemStacks.
     */
    public List<ItemStack> getItems() {
        List<ItemStack> items = new ArrayList<>();
        for (String snbt : itemSnbts) {
            try {
                CompoundTag tag = TagParser.parseTag(snbt);
                ItemStack stack = ItemStack.of(tag);
                if (!stack.isEmpty()) {
                    items.add(stack);
                }
            } catch (Exception e) {
                // Skip invalid items
            }
        }
        return items;
    }

    /**
     * Creates a Kit from a list of ItemStacks.
     */
    public static Kit fromItems(String tier, CooldownType cooldownType, List<ItemStack> items) {
        List<String> snbts = new ArrayList<>();
        for (ItemStack stack : items) {
            if (!stack.isEmpty()) {
                CompoundTag tag = new CompoundTag();
                stack.save(tag);
                snbts.add(tag.toString());
            }
        }
        return new Kit(tier, cooldownType, snbts);
    }

    public enum CooldownType {
        DAILY("daily"),
        WEEKLY("weekly"),
        MONTHLY("monthly");

        private final String id;

        CooldownType(String id) {
            this.id = id;
        }

        public String getId() {
            return id;
        }

        public static CooldownType fromId(String id) {
            for (CooldownType type : values()) {
                if (type.id.equalsIgnoreCase(id)) {
                    return type;
                }
            }
            return null;
        }

        /**
         * Returns cooldown duration in milliseconds.
         */
        public long getCooldownMillis() {
            return switch (this) {
                case DAILY -> 24L * 60L * 60L * 1000L;
                case WEEKLY -> 7L * 24L * 60L * 60L * 1000L;
                case MONTHLY -> 30L * 24L * 60L * 60L * 1000L;
            };
        }
    }
}
