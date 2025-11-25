package org.lupz.doomsdayessentials.event;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.EntityItemPickupEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lupz.doomsdayessentials.EssentialsMod;
import org.lupz.doomsdayessentials.command.VipCommand;

@Mod.EventBusSubscriber(modid = EssentialsMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class VipSkinGuardEvents {

    @SubscribeEvent
    public static void onPickup(EntityItemPickupEvent event) {
        Player p = event.getEntity();
        String tier = p instanceof net.minecraft.server.level.ServerPlayer sp ? VipCommand.getTier(sp) : null;
        if (tier != null && "dissoluto".equals(tier)) return;
        ItemStack stack = event.getItem().getItem();
        var key = net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(stack.getItem());
        if (key == null || key.getNamespace() == null || !"tacz".equals(key.getNamespace())) return;
        var tag = stack.getTag();
        if (tag == null) return;
        if (!tag.contains("GunId", net.minecraft.nbt.Tag.TAG_STRING)) return;
        String gunId = tag.getString("GunId");
        if (gunId == null) return;
        String gid = gunId.toLowerCase(java.util.Locale.ROOT);
        if (gid.startsWith("doomsday:kuronami")) {
            tag.putString("GunId", "tacz:ak47");
            stack.setTag(tag);
        }
    }
}
