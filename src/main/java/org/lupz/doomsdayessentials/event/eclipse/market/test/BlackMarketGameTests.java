package org.lupz.doomsdayessentials.event.eclipse.market.test;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.gametest.GameTestHolder;
import org.lupz.doomsdayessentials.EssentialsMod;
import org.lupz.doomsdayessentials.event.eclipse.market.BlackMarketBundleItem;
import org.lupz.doomsdayessentials.event.eclipse.market.ItemStackSpec;
import org.lupz.doomsdayessentials.item.ModItems;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.List;

@GameTestHolder(EssentialsMod.MOD_ID)
public class BlackMarketGameTests {

    @GameTest(template = "minecraft:empty")
    public void bundleUnpacksIntoInventory(GameTestHelper helper){
        Player p = helper.makeMockPlayer();
        var items = List.of(new ItemStackSpec(new ResourceLocation("minecraft", "carrot"), 2, null),
                new ItemStackSpec(new ResourceLocation("minecraft", "bread"), 1, null));
        ItemStack bundle = BlackMarketBundleItem.createBundle("test_bundle", items);
        p.setItemInHand(InteractionHand.MAIN_HAND, bundle.copy());
        bundle.getItem().use(helper.getLevel(), p, InteractionHand.MAIN_HAND);

        boolean hasCarrot = p.getInventory().countItem(ModItems.BLACK_MARKET_BUNDLE.get()) == 0 // bundle consumed
                && p.getInventory().countItem(net.minecraft.world.item.Items.CARROT) >= 2;
        if (hasCarrot) helper.succeed(); else helper.fail("Bundle did not unpack into inventory");
    }

    @GameTest(template = "minecraft:empty")
    public void loadManyTradesFromJson(GameTestHelper helper){
        try {
            // Generate a temporary large config
            java.nio.file.Path dir = net.minecraftforge.fml.loading.FMLPaths.CONFIGDIR.get().resolve("doomsdayessentials");
            java.nio.file.Files.createDirectories(dir);
            java.nio.file.Path file = dir.resolve("black_market.json");
            StringBuilder sb = new StringBuilder();
            sb.append("{\n  \"trades\": [\n");
            for (int i=0;i<200;i++){
                sb.append("    { \"alias\": \"t"+i+"\", \"buys\": [{ \"id\": \"minecraft:emerald\", \"count\": 1 }], \"sells\": [{ \"id\": \"minecraft:bread\", \"count\": 1 }], \"maxUses\": 64, \"xp\": 1, \"priceMultiplier\": 0.05 }\n");
                if (i<199) sb.append(",\n");
            }
            sb.append("  ]\n}");
            java.nio.file.Files.writeString(file, sb.toString());

            org.lupz.doomsdayessentials.event.eclipse.market.BlackMarketConfigManager.reload();
            var offers = org.lupz.doomsdayessentials.event.eclipse.market.BlackMarketConfigManager.toOffers(helper.getLevel(), new org.lupz.doomsdayessentials.event.eclipse.market.BlackMarketFilter(null, java.util.Set.of(), null));
            if (offers.size() >= 200) helper.succeed(); else helper.fail("Expected >=200 offers, got " + offers.size());
        } catch (Exception e){
            helper.fail("Exception generating large config: " + e.getMessage());
        }
    }
}
