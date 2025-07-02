package org.lupz.doomsdayessentials.professions.items;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.lupz.doomsdayessentials.config.EssentialsConfig;
import org.lupz.doomsdayessentials.professions.RastreadorProfession;
import net.minecraft.world.item.Vanishable;

import javax.annotation.Nullable;
import java.util.List;

public class TrackingCompassItem extends Item implements Vanishable {

    public TrackingCompassItem(Properties props) {
        super(props);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide || !(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResultHolder.fail(stack);
        }

        if (player.getCooldowns().isOnCooldown(this)) {
            return InteractionResultHolder.fail(stack);
        }

        if (RastreadorProfession.useGlowAbility(serverPlayer)) {
            player.getCooldowns().addCooldown(this, EssentialsConfig.TRACKER_COMPASS_COOLDOWN.get());
            return InteractionResultHolder.success(stack);
        }

        return InteractionResultHolder.fail(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.doomsdayessentials.tracking_compass.lore1").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.doomsdayessentials.tracking_compass.lore2").withStyle(ChatFormatting.GOLD));
        super.appendHoverText(stack, level, tooltip, flag);
        tooltip.add(Component.translatable("item.doomsdayessentials.tracking_compass.desc1").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.doomsdayessentials.tracking_compass.desc2").withStyle(ChatFormatting.DARK_GREEN));
        tooltip.add(Component.literal("§7Use o Compasso para revelar jogadores próximos."));
    }
} 