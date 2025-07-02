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
import net.minecraft.world.phys.AABB;
import org.lupz.doomsdayessentials.config.EssentialsConfig;
import org.lupz.doomsdayessentials.professions.ProfissaoManager;
import org.lupz.doomsdayessentials.professions.RastreadorProfession;
import org.lupz.doomsdayessentials.professions.capability.TrackerCapabilityProvider;
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
        if (!RastreadorProfession.isTracker(player)) {
            if (!level.isClientSide) {
                player.sendSystemMessage(Component.translatable("item.tracking_compass.not_tracker").withStyle(ChatFormatting.RED));
            }
            return InteractionResultHolder.fail(stack);
        }

        if (player.getCooldowns().isOnCooldown(this)) {
            return InteractionResultHolder.pass(stack);
        }

        if (level.isClientSide) {
            return InteractionResultHolder.success(stack);
        }

        ServerPlayer serverPlayer = (ServerPlayer) player;
        double radius = EssentialsConfig.TRACKER_COMPASS_RADIUS.get();
        int duration = EssentialsConfig.TRACKER_COMPASS_DURATION.get() * 20; // to ticks
        int cooldown = EssentialsConfig.TRACKER_COMPASS_COOLDOWN.get() * 20; // to ticks

        AABB area = new AABB(player.blockPosition()).inflate(radius);

        serverPlayer.getCapability(TrackerCapabilityProvider.TRACKER_CAPABILITY).ifPresent(cap -> {
            List<ServerPlayer> nearbyPlayers = level.getEntitiesOfClass(ServerPlayer.class, area,
                p -> p != serverPlayer && !cap.isWhitelisted(p.getUUID()));

            if (nearbyPlayers.isEmpty()) {
                serverPlayer.sendSystemMessage(Component.literal("Nenhum jogador encontrado por perto.").withStyle(ChatFormatting.YELLOW));
            } else {
                for (ServerPlayer target : nearbyPlayers) {
                    target.addEffect(new MobEffectInstance(MobEffects.GLOWING, duration, 0));
                }
                serverPlayer.sendSystemMessage(Component.literal("Revelados " + nearbyPlayers.size() + " jogadores próximos!").withStyle(ChatFormatting.GREEN));
            }
        });
        
        serverPlayer.getCooldowns().addCooldown(this, cooldown);
        return InteractionResultHolder.success(stack);
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