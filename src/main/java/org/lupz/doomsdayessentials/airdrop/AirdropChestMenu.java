package org.lupz.doomsdayessentials.airdrop;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;
import org.lupz.doomsdayessentials.entity.AirdropEntity;

/**
 * Chest-like menu for the airdrop that plays a close sound when the menu is closed.
 */
public class AirdropChestMenu extends ChestMenu {
    private final AirdropEntity airdrop;

    public AirdropChestMenu(int id, Inventory inv, Container cont, int rows, AirdropEntity airdrop) {
        super(MenuType.GENERIC_9x3, id, inv, cont, rows);
        this.airdrop = airdrop;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (!player.level().isClientSide && airdrop != null && !airdrop.isRemoved()) {
            airdrop.level().playSound(null, airdrop.getX(), airdrop.getY(), airdrop.getZ(), SoundEvents.CHEST_CLOSE, SoundSource.BLOCKS, 1.0f, 1.0f);
        }
    }
}
