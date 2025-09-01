package org.lupz.doomsdayessentials.professions.items;

import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkHooks;
import org.lupz.doomsdayessentials.professions.menu.BountyBoardMenuProvider;

public class HuntingBoardItem extends Item {
	public HuntingBoardItem(Properties props) { super(props); }

	@Override
	public InteractionResult useOn(UseOnContext context) {
		if (!context.getLevel().isClientSide && context.getPlayer() instanceof ServerPlayer sp) {
			NetworkHooks.openScreen(sp, new BountyBoardMenuProvider());
			return InteractionResult.SUCCESS;
		}
		return InteractionResult.CONSUME;
	}
} 