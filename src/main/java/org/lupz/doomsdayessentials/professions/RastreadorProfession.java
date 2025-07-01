package org.lupz.doomsdayessentials.professions;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.lupz.doomsdayessentials.config.ProfessionConfig;
import org.lupz.doomsdayessentials.professions.items.ProfessionItems;

public final class RastreadorProfession {
    private static final String TAG_IS_TRACKER = "isRastreador";

    private RastreadorProfession() {}

    public static void onBecome(Player player) {
        if (player.level().isClientSide) return;
        if (!ProfessionConfig.RASTREADOR_ENABLED.get()) {
            player.sendSystemMessage(Component.translatable("profession.rastreador.disabled"));
            return;
        }
        player.getPersistentData().putBoolean(TAG_IS_TRACKER, true);
        player.sendSystemMessage(Component.translatable("profession.rastreador.become"));

        // Give the tracking compass
        player.getInventory().add(new ItemStack(ProfessionItems.TRACKING_COMPASS.get()));
    }

    public static void onLeave(Player player) {
        if (player.level().isClientSide) return;
        player.getPersistentData().putBoolean(TAG_IS_TRACKER, false);
        player.sendSystemMessage(Component.translatable("profession.rastreador.leave"));

        player.getInventory().items.removeIf(stack -> stack.getItem() == ProfessionItems.TRACKING_COMPASS.get());
    }

    public static boolean isTracker(Player player) {
        return player.getPersistentData().getBoolean(TAG_IS_TRACKER);
    }
} 