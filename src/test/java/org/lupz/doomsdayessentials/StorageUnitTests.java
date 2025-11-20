package org.lupz.doomsdayessentials;

import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Assertions;
import org.lupz.doomsdayessentials.guild.GuildsManager;
import org.lupz.doomsdayessentials.guild.menu.GuildStorageMenu;

public class StorageUnitTests {
    @Test
    @Disabled("Requires Minecraft bootstrap; validated in GameTest")
    public void encodeDecodeCappedAt64() {
        ItemStack logs = new ItemStack(Items.OAK_LOG, 200);
        Assertions.assertEquals(64, logs.getCount());
        ItemStack copy = logs.copy();
        Assertions.assertEquals(64, copy.getCount());
    }

    @Test
    @Disabled("Requires Minecraft bootstrap; validated in GameTest")
    public void saveLoadRoundtripCappedAt64() {
        GuildsManager gm = new GuildsManager();
        NonNullList<ItemStack> storage = gm.getOrCreateStorage("Dooms");
        ItemStack logs = new ItemStack(Items.OAK_LOG, 500);
        storage.set(0, logs);
        CompoundTag tag = gm.save(new CompoundTag());
        GuildsManager gm2 = GuildsManager.load(tag);
        NonNullList<ItemStack> loaded = gm2.getOrCreateStorage("Dooms");
        Assertions.assertTrue(loaded.size() >= 1);
        Assertions.assertEquals(64, loaded.get(0).getCount());
    }

    @Test
    @Disabled("Requires Minecraft bootstrap; validated in GameTest")
    public void mergeCappedAt64PerSlot() {
        NonNullList<ItemStack> storage = NonNullList.withSize(3, ItemStack.EMPTY);
        ItemStack logs50 = new ItemStack(Items.OAK_LOG, 50);
        storage.set(0, logs50);
        ItemStack add30 = new ItemStack(Items.OAK_LOG, 30);
        ItemStack existing = storage.get(0);
        int free = existing.getMaxStackSize() - existing.getCount();
        int move = Math.min(free, add30.getCount());
        existing.grow(move);
        add30.shrink(move);
        Assertions.assertEquals(64, existing.getCount());
        Assertions.assertEquals(16, add30.getCount());
    }
}
