package org.lupz.doomsdayessentials.event;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import org.lupz.doomsdayessentials.EssentialsMod;
import org.lupz.doomsdayessentials.item.BlockedItemManager;

/**
 * Listens to item use events and cancels them if the item is present in the blocked list maintained by {@link BlockedItemManager}.
 */
@Mod.EventBusSubscriber(modid = EssentialsMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ItemBlockerEvents {

    private ItemBlockerEvents() {}

    // ------------------------------------------------------------
    // Right-click with item in air
    // ------------------------------------------------------------
    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem e) {
        if (e.getLevel().isClientSide) return;
        ItemStack stack = e.getItemStack();
        if (stack.isEmpty()) return;
        if (isBlocked(stack)) {
            Player player = e.getEntity();
            player.sendSystemMessage(Component.literal("§cEste item está bloqueado."));
            e.setCanceled(true);
            e.setCancellationResult(InteractionResult.FAIL);
        }
    }

    // ------------------------------------------------------------
    // Right-click on block while holding item
    // ------------------------------------------------------------
    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock e) {
        if (e.getLevel().isClientSide) return;
        ItemStack stack = e.getItemStack();
        if (stack.isEmpty()) return;
        if (isBlocked(stack)) {
            Player player = e.getEntity();
            player.sendSystemMessage(Component.literal("§cEste item está bloqueado."));
            e.setCanceled(true);
            e.setCancellationResult(InteractionResult.FAIL);
        }
    }

    // ------------------------------------------------------------
    // Start to consume / use item (bow, food, etc.)
    // ------------------------------------------------------------
    @SubscribeEvent
    public static void onItemUseStart(LivingEntityUseItemEvent.Start e) {
        LivingEntity entity = e.getEntity();
        if (!(entity instanceof Player player)) return;
        if (entity.level().isClientSide) return;
        ItemStack stack = e.getItem();
        if (stack.isEmpty()) return;
        if (isBlocked(stack)) {
            player.sendSystemMessage(Component.literal("§cEste item está bloqueado."));
            e.setCanceled(true);
        }
    }

    // ------------------------------------------------------------
    // Helper
    // ------------------------------------------------------------
    private static boolean isBlocked(ItemStack stack) {
        ResourceLocation rl = ForgeRegistries.ITEMS.getKey(stack.getItem());
        if (rl == null) return false;
        return BlockedItemManager.get().isBlocked(rl.toString());
    }
} 