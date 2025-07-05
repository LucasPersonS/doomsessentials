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

/**
 * Hammer item granted to Engineers. Right-click to spawn a temporary barrier.
 */
public class EngineerHammerItem extends Item {
    public EngineerHammerItem(Properties props) { super(props); }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (!level.isClientSide && player instanceof ServerPlayer sp) {
            EngenheiroProfession.trySpawnBarrier(sp);
        }
        return InteractionResultHolder.success(player.getItemInHand(hand));
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.doomsdayessentials.engineer_hammer.desc1").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.doomsdayessentials.engineer_hammer.desc2").withStyle(ChatFormatting.DARK_GREEN));
    }
} 