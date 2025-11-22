package org.lupz.doomsdayessentials.gametest;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.gametest.GameTestHolder;

@GameTestHolder("dooms-disabled")
public class LootboxRemoveGameTests {

    @GameTest(template = "empty")
    public void removeItemByIndex(GameTestHelper helper) {
        try {
            org.lupz.doomsdayessentials.lootbox.LootboxManager.load();
            net.minecraft.world.item.ItemStack s = new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.DIAMOND);
            boolean okAdd = org.lupz.doomsdayessentials.lootbox.LootboxManager.addItem(org.lupz.doomsdayessentials.lootbox.LootboxManager.R_EPICA, s, 1.0);
            if (!okAdd) { helper.fail("addItem failed"); return; }
            java.util.List<net.minecraft.world.item.ItemStack> before = org.lupz.doomsdayessentials.lootbox.LootboxManager.getAllAsStacks(org.lupz.doomsdayessentials.lootbox.LootboxManager.R_EPICA);
            boolean okRem = org.lupz.doomsdayessentials.lootbox.LootboxManager.removeItem(org.lupz.doomsdayessentials.lootbox.LootboxManager.R_EPICA, before.size() - 1);
            java.util.List<net.minecraft.world.item.ItemStack> after = org.lupz.doomsdayessentials.lootbox.LootboxManager.getAllAsStacks(org.lupz.doomsdayessentials.lootbox.LootboxManager.R_EPICA);
            if (okRem && after.size() == Math.max(0, before.size() - 1)) helper.succeed(); else helper.fail("removeItem failed");
        } catch (Exception e) { helper.fail("Exception: " + e.getMessage()); }
    }
}
