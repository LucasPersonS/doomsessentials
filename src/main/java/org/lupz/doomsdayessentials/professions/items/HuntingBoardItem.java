package org.lupz.doomsdayessentials.professions.items;

import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkHooks;
import org.lupz.doomsdayessentials.professions.menu.BountyBoardMenuProvider;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;

import javax.annotation.Nullable;
import java.util.List;

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

	@Override
	public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
		tooltip.add(Component.translatable("item.doomsdayessentials.hunting_board.desc1").withStyle(ChatFormatting.GRAY));
		tooltip.add(Component.translatable("item.doomsdayessentials.hunting_board.desc2").withStyle(ChatFormatting.GOLD));
	}
} 