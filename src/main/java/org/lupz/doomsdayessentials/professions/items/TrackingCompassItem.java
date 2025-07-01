package org.lupz.doomsdayessentials.professions.items;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.lupz.doomsdayessentials.config.ProfessionConfig;
import org.lupz.doomsdayessentials.professions.RastreadorProfession;
import net.minecraft.world.item.TooltipFlag;

import java.util.Comparator;
import java.util.List;
import javax.annotation.Nullable;

public class TrackingCompassItem extends Item {

    private static final String TAG_COOLDOWN = "trackerCooldown";

    public TrackingCompassItem(Properties props) {
        super(props);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) return InteractionResultHolder.success(stack);

        ServerPlayer sPlayer = (ServerPlayer) player;
        int cdTicks = stack.getOrCreateTag().getInt(TAG_COOLDOWN);
        if (!RastreadorProfession.isTracker(player)) {
            player.sendSystemMessage(Component.translatable("item.tracking_compass.not_tracker").withStyle(ChatFormatting.RED));
            return InteractionResultHolder.fail(stack);
        }

        if (cdTicks > 0) {
            int minutes = cdTicks / 1200;
            int seconds = (cdTicks / 20) % 60;
            player.sendSystemMessage(Component.literal("A bússola está recarregando: " + minutes + "m " + seconds + "s").withStyle(ChatFormatting.RED));
            return InteractionResultHolder.fail(stack);
        }

        // find nearest other player in same dimension
        ServerLevel sLevel = (ServerLevel) level;
        sPlayer.getServer().getPlayerList();
        ServerPlayer nearest = sLevel.getPlayers(p -> p != player)
                .stream()
                .min(Comparator.comparingDouble(p -> p.distanceTo(player)))
                .orElse(null);

        if (nearest == null) {
            player.sendSystemMessage(Component.literal("Nenhum jogador encontrado.").withStyle(ChatFormatting.YELLOW));
            return InteractionResultHolder.pass(stack);
        }

        player.sendSystemMessage(Component.literal("Rastreando " + nearest.getName().getString() + " em (" +
                (int) nearest.getX() + ", " + (int) nearest.getY() + ", " + (int) nearest.getZ() + ")").withStyle(ChatFormatting.GREEN));

        // set cooldown
        int cd = ProfessionConfig.RASTREADOR_COOLDOWN_MINUTES.get() * 1200;
        stack.getOrCreateTag().putInt(TAG_COOLDOWN, cd);
        return InteractionResultHolder.success(stack);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, net.minecraft.world.entity.Entity entity, int slot, boolean selected) {
        if (level.isClientSide || !(entity instanceof Player player)) return;
        if (!stack.hasTag()) return;
        int cd = stack.getTag().getInt(TAG_COOLDOWN);
        if (cd > 0) {
            cd--;
            stack.getTag().putInt(TAG_COOLDOWN, cd);
            if (cd == 0) {
                player.sendSystemMessage(Component.literal("A bússola de rastreamento pode ser usada novamente.").withStyle(ChatFormatting.GREEN));
            }
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.doomsdayessentials.tracking_compass.lore1").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.doomsdayessentials.tracking_compass.lore2").withStyle(ChatFormatting.GOLD));
        super.appendHoverText(stack, level, tooltip, flag);
        tooltip.add(Component.translatable("item.doomsdayessentials.tracking_compass.desc1").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.doomsdayessentials.tracking_compass.desc2").withStyle(ChatFormatting.DARK_GREEN));
    }
} 