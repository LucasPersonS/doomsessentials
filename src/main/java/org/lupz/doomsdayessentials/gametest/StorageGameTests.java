package org.lupz.doomsdayessentials.gametest;

import net.minecraft.core.NonNullList;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.lupz.doomsdayessentials.guild.GuildsManager;
import org.lupz.doomsdayessentials.guild.menu.GuildStorageMenu;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;

public class StorageGameTests {
    @GameTest(template = "minecraft:empty")
    public void stackingPreservesCounts(GameTestHelper helper) {
        try {
            ItemStack logs = new ItemStack(Items.OAK_LOG, 200);
            int limit = GuildStorageMenu.Stacking.storageLimitStatic(logs);
            helper.assertTrue(limit == 32767, "limit should be 32767");
            ItemStack enc = GuildStorageMenu.Stacking.encodeForStorageStatic(logs);
            helper.assertTrue(enc.getCount() == 200, "encoded count should stay 200");
            ItemStack dec = GuildStorageMenu.Stacking.decodeFromStorageStatic(enc);
            helper.assertTrue(dec.getCount() == 200, "decoded count should stay 200");
            NonNullList<ItemStack> storage = NonNullList.withSize(54, ItemStack.EMPTY);
            ItemStack rem = GuildStorageMenu.Stacking.placeIntoStorage(storage, logs);
            helper.assertTrue(rem.isEmpty(), "remaining should be empty");
            helper.assertTrue(storage.get(0).getCount() == 200, "slot 0 should have 200");
            helper.succeed();
        } catch (Throwable t) { helper.fail(t); }
    }

    @GameTest(template = "minecraft:empty")
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
            helper.assertTrue(inv.size() == 1, "inv size should be 1");
            ItemStack loaded = inv.get(0);
            helper.assertTrue(loaded.getCount() == 200, "loaded count should be 200");
            helper.assertTrue(!(loaded.hasTag() && loaded.getTag().contains("gd_ext_count")), "ext tag should be removed");
            helper.succeed();
        } catch (Throwable t) { helper.fail(t); }
    }

    @GameTest(template = "minecraft:empty")
    public void mergePlaceDefaultBehavior(GameTestHelper helper) {
        try {
            NonNullList<ItemStack> storage = NonNullList.withSize(54, ItemStack.EMPTY);
            storage.set(0, new ItemStack(Items.OAK_LOG, 50));
            ItemStack add = new ItemStack(Items.OAK_LOG, 150);
            ItemStack rem = GuildStorageMenu.Stacking.mergeIntoStorage(storage, add);
            helper.assertTrue(rem.isEmpty(), "merge remaining empty");
            helper.assertTrue(storage.get(0).getCount() == 200, "merged count should be 200");
            ItemStack many = new ItemStack(Items.OAK_LOG, 500);
            ItemStack rem2 = GuildStorageMenu.Stacking.placeIntoStorage(storage, many);
            helper.assertTrue(rem2.isEmpty(), "place remaining empty");
            helper.assertTrue(storage.get(1).getCount() == 500, "slot 1 should have 500");
            helper.succeed();
        } catch (Throwable t) { helper.fail(t); }
    }
}

