package org.lupz.doomsdayessentials.professions.items;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.NotNull;
import org.lupz.doomsdayessentials.professions.EngenheiroProfession;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.world.item.TooltipFlag;
import javax.annotation.Nullable;
import java.util.List;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.InteractionResult;
import org.lupz.doomsdayessentials.professions.network.EngineerNetwork;
import org.lupz.doomsdayessentials.professions.network.UseEngineerHammerPacket;

/**
 * Hammer item granted to Engineers. Right-click to spawn a temporary barrier.
 */
public class EngineerHammerItem extends Item {
    public EngineerHammerItem(Properties p_41383_) {
        super(p_41383_);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (context.getLevel().isClientSide && context.getPlayer() != null) {
            EngineerNetwork.INSTANCE.sendToServer(new UseEngineerHammerPacket());
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.CONSUME;
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, java.util.List<net.minecraft.network.chat.Component> tooltip, net.minecraft.world.item.TooltipFlag flag) {
        tooltip.add(net.minecraft.network.chat.Component.translatable("tooltip.doomsdayessentials.engineer_hammer").withStyle(net.minecraft.ChatFormatting.GRAY));
        super.appendHoverText(stack, level, tooltip, flag);
    }
} 