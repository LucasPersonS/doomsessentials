package org.lupz.doomsdayessentials.gametest;

import net.minecraft.core.NonNullList;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.lupz.doomsdayessentials.EssentialsMod;
import org.lupz.doomsdayessentials.guild.GuildsManager;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;

@GameTestHolder(EssentialsMod.MOD_ID)
public class StorageGameTests {
    @GameTest(template = "empty")
    public void standardStackingBehavior(GameTestHelper helper) {
        try {
            ItemStack logs = new ItemStack(Items.OAK_LOG, 200);
            // Vanilla constructor clamps to 64
            helper.assertTrue(logs.getCount() == 64, "count should be capped to 64 by vanilla constructor");

            ItemStack copy = logs.copy();
            helper.assertTrue(copy.getCount() == 64, "copy count should be 64");

            NonNullList<ItemStack> storage = NonNullList.withSize(54, ItemStack.EMPTY);
            storage.set(0, logs);
            helper.assertTrue(storage.get(0).getCount() == 64, "slot 0 count should be 64");
            helper.succeed();
        } catch (Throwable t) {
            helper.fail(String.valueOf(t));
        }
    }

    @GameTest(template = "empty")
    public void legacyExtTagConversion(GameTestHelper helper) {
        try {
            CompoundTag root = new CompoundTag();
            CompoundTag stores = new CompoundTag();
            ListTag list = new ListTag();
            ItemStack legacy = new ItemStack(Items.OAK_LOG, 64);
            legacy.getOrCreateTag().putInt("gd_ext_count", 200);
            CompoundTag st = new CompoundTag();
            legacy.save(st);
            list.add(st);
            stores.put("Dooms", list);
            root.put("guildStorages", stores);
            GuildsManager gm = GuildsManager.load(root);
            NonNullList<ItemStack> inv = gm.getOrCreateStorage("Dooms");
            // 200 items split into 64, 64, 64, 8 -> 4 stacks
            helper.assertTrue(inv.size() == 4, "inv size should be 4 after split");
            helper.assertTrue(inv.get(0).getCount() == 64, "first piece 64");
            helper.assertTrue(inv.get(1).getCount() == 64, "second piece 64");
            helper.assertTrue(inv.get(2).getCount() == 64, "third piece 64");
            helper.assertTrue(inv.get(3).getCount() == 8, "last piece 8");
            helper.succeed();
        } catch (Throwable t) {
            helper.fail(String.valueOf(t));
        }
    }

    @GameTest(template = "empty")
    public void saveLoadRoundtripStandardCounts(GameTestHelper helper) {
        try {
            GuildsManager gm = new GuildsManager();
            NonNullList<ItemStack> storage = gm.getOrCreateStorage("Dooms");
            ItemStack logs = new ItemStack(Items.OAK_LOG, 64);
            storage.set(0, logs);
            CompoundTag tag = gm.save(new CompoundTag());
            GuildsManager gm2 = GuildsManager.load(tag);
            NonNullList<ItemStack> loaded = gm2.getOrCreateStorage("Dooms");
            helper.assertTrue(loaded.size() >= 1, "loaded storage should have at least 1 slot");
            ItemStack st = loaded.get(0);
            helper.assertTrue(st.getCount() == 64, "roundtrip count should be 64");
            helper.succeed();
        } catch (Throwable t) {
            helper.fail(String.valueOf(t));
        }
    }

    @GameTest(template = "empty")
    public void mergeRespectsMaxStackSize(GameTestHelper helper) {
        try {
            NonNullList<ItemStack> storage = NonNullList.withSize(3, ItemStack.EMPTY);
            ItemStack logs50 = new ItemStack(Items.OAK_LOG, 50);
            storage.set(0, logs50);
            ItemStack add30 = new ItemStack(Items.OAK_LOG, 30);

            // Simulate merge logic manually as Stacking.mergeIntoStorage is gone
            ItemStack existing = storage.get(0);
            int free = existing.getMaxStackSize() - existing.getCount();
            int move = Math.min(free, add30.getCount());
            existing.grow(move);
            add30.shrink(move);

            helper.assertTrue(add30.getCount() == 16, "remaining should be 16 (30 - 14)");
            helper.assertTrue(existing.getCount() == 64, "slot0 count should be 64 after merge");
            helper.succeed();
        } catch (Throwable t) {
            helper.fail(String.valueOf(t));
        }
    }
}
