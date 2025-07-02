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
import net.minecraft.world.item.enchantment.Enchantments;

import java.util.Comparator;
import java.util.List;
import javax.annotation.Nullable;

public class TrackingCompassItem extends Item {

    private static final String TAG_COOLDOWN = "trackerCooldown";
    private static final String TAG_TRACK_TARGET = "trackTarget";
    private static final String TAG_TRACK_TICKS = "trackTicks";

    public TrackingCompassItem(Properties props) {
        super(props);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        // If the player is not a tracker, immediately deny the use on the **server** side to avoid duplicated messages
        if (!RastreadorProfession.isTracker(player)) {
            if (!level.isClientSide) {
                player.sendSystemMessage(Component.translatable("item.tracking_compass.not_tracker").withStyle(ChatFormatting.RED));
            }
            return InteractionResultHolder.fail(stack);
        }

        if (level.isClientSide) return InteractionResultHolder.success(stack);

        ServerPlayer sPlayer = (ServerPlayer) player;
        int cdTicks = stack.getOrCreateTag().getInt(TAG_COOLDOWN);
        if (stack.getOrCreateTag().getInt(TAG_TRACK_TICKS) > 0) {
            player.sendSystemMessage(Component.literal("Já está rastreando um jogador.").withStyle(ChatFormatting.YELLOW));
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

        player.sendSystemMessage(Component.literal("Rastreando " + nearest.getName().getString() + " por 1 hora.").withStyle(ChatFormatting.GREEN));

        // store tracking target and time (1 hour = 3600s *20 = 72000)
        stack.getOrCreateTag().putUUID(TAG_TRACK_TARGET, nearest.getUUID());
        stack.getOrCreateTag().putInt(TAG_TRACK_TICKS, 72000);

        // add glint by adding empty enchantment list if none
        if (!stack.isEnchanted()) {
            stack.enchant(Enchantments.UNBREAKING, 1); // dummy low-level enchant to force glint
        }

        // rename compass
        stack.setHoverName(Component.literal("§eTracking: " + nearest.getName().getString()));

        // set cooldown for next track
        stack.getOrCreateTag().putInt(TAG_COOLDOWN, 72000); // 1 hour cooldown as well

        // set tracker target data on player for reward system
        player.getPersistentData().putUUID("currentTrackingTarget", nearest.getUUID());
        player.getPersistentData().putInt("currentTrackingTicks", 72000);

        return InteractionResultHolder.success(stack);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, net.minecraft.world.entity.Entity entity, int slot, boolean selected) {
        if (level.isClientSide || !(entity instanceof Player player)) return;
        if (!stack.hasTag()) return;

        var tag = stack.getTag();

        // handle cooldown decrement
        int cd = tag.getInt(TAG_COOLDOWN);
        if (cd > 0) {
            cd--;
            tag.putInt(TAG_COOLDOWN, cd);
            if (cd == 0) {
                player.sendSystemMessage(Component.literal("A bússola de rastreamento pode ser usada novamente.").withStyle(ChatFormatting.GREEN));
            }
        }

        // handle active tracking
        int trackTicks = tag.getInt(TAG_TRACK_TICKS);
        if (trackTicks > 0) {
            trackTicks--;
            tag.putInt(TAG_TRACK_TICKS, trackTicks);

            // Every 20 ticks, update player lodestone angle maybe; skipped for simplicity

            if (trackTicks == 0) {
                // Tracking finished
                tag.remove(TAG_TRACK_TARGET);
                stack.removeTagKey("Enchantments"); // remove dummy enchant, stops glint
                stack.resetHoverName();
                player.sendSystemMessage(Component.literal("Rastreamento expirou. A bússola voltou ao normal.").withStyle(ChatFormatting.YELLOW));

                // clear player tracking tags
                player.getPersistentData().remove("currentTrackingTarget");
                player.getPersistentData().remove("currentTrackingTicks");
            }
        }

        // also tick player's tracking time in case compass moved to other slot
        if (player.getPersistentData().contains("currentTrackingTicks")) {
            int pt = player.getPersistentData().getInt("currentTrackingTicks");
            if (pt > 0) {
                pt--;
                player.getPersistentData().putInt("currentTrackingTicks", pt);
            }
            if (pt == 0) {
                player.getPersistentData().remove("currentTrackingTarget");
                player.getPersistentData().remove("currentTrackingTicks");
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
        tooltip.add(Component.literal("§7Use o Compasso para brilhar um jogador."));
        tooltip.add(Component.literal("§7Segure o compasso na outra mão para rastrear."));
    }
} 