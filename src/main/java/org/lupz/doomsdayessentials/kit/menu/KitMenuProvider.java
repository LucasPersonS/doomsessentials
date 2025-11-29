package org.lupz.doomsdayessentials.kit.menu;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lupz.doomsdayessentials.kit.Kit;

public class KitMenuProvider implements MenuProvider {
    private final Kit.CooldownType cooldownType;

    public KitMenuProvider(Kit.CooldownType cooldownType) {
        this.cooldownType = cooldownType;
    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.literal("Kits - " + capitalize(cooldownType.getId()));
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int windowId, @NotNull Inventory inv, @NotNull Player player) {
        return new KitMenu(windowId, inv, cooldownType);
    }

    private String capitalize(String str) {
        if (str == null || str.isEmpty())
            return str;
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }

    /**
     * Writes additional data to the packet buffer for client synchronization.
     */
    public void writeData(FriendlyByteBuf buf) {
        buf.writeUtf(cooldownType.getId());
    }

    /**
     * Opens the kit menu for a player with network synchronization.
     */
    public static void open(ServerPlayer player, Kit.CooldownType cooldownType) {
        KitMenuProvider provider = new KitMenuProvider(cooldownType);
        net.minecraftforge.network.NetworkHooks.openScreen(player, provider, buf -> buf.writeUtf(cooldownType.getId()));
    }
}
