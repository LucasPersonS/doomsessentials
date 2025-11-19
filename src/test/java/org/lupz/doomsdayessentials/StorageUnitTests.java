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
        ItemStack enc = GuildStorageMenu.Stacking.encodeForStorageStatic(logs);
        Assertions.assertEquals(64, enc.getCount());
        Assertions.assertEquals(64, GuildStorageMenu.Stacking.trueCountOfStatic(enc));
        ItemStack dec = GuildStorageMenu.Stacking.decodeFromStorageStatic(enc);
        Assertions.assertEquals(64, dec.getCount());
    }

    @Test
    @Disabled("Requires Minecraft bootstrap; validated in GameTest")
    public void saveLoadRoundtripCappedAt64() {
        GuildsManager gm = new GuildsManager();
        NonNullList<ItemStack> storage = gm.getOrCreateStorage("Dooms");
        ItemStack logs = new ItemStack(Items.OAK_LOG, 500);
        storage.set(0, GuildStorageMenu.Stacking.encodeForStorageStatic(logs));
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
        storage.set(0, GuildStorageMenu.Stacking.encodeForStorageStatic(logs50));
        ItemStack add30 = new ItemStack(Items.OAK_LOG, 30);
        ItemStack rem = GuildStorageMenu.Stacking.mergeIntoStorage(storage, add30);
        Assertions.assertTrue(rem.isEmpty());
        Assertions.assertEquals(64, GuildStorageMenu.Stacking.trueCountOfStatic(storage.get(0)));
    }
}
