package org.lupz.doomsdayessentials.injury;

import java.util.Iterator;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import org.lupz.doomsdayessentials.config.ProfessionConfig;
import org.lupz.doomsdayessentials.injury.capability.InjuryCapability;
import org.lupz.doomsdayessentials.injury.network.InjuryNetwork;
import org.lupz.doomsdayessentials.injury.network.UpdateDownedStatePacket;
import org.lupz.doomsdayessentials.professions.ProfissaoManager;
import org.lupz.doomsdayessentials.injury.InjuryEvents;
import org.lupz.doomsdayessentials.EssentialsMod;

public class MedicKitItem extends Item {
   public MedicKitItem(Item.Properties properties) {
      super(properties);
   }

   @Override
   public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
      ItemStack stack = player.getItemInHand(hand);
      if (!this.isMedico(player)) {
         if (!level.isClientSide) {
            player.sendSystemMessage(Component.translatable("item.medic_kit.not_medico"));
         }

         return InteractionResultHolder.fail(stack);
      } else {
         player.startUsingItem(hand);
         return InteractionResultHolder.success(stack);
      }
   }

   @Override
   public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
      if (entity instanceof ServerPlayer player) {
         if (!level.isClientSide) {
            double range = ProfessionConfig.MEDICO_KIT_RANGE.get();
            int hearts = ProfessionConfig.MEDICO_KIT_HEAL_HEARTS.get();

            Player target = this.findNearestLowHealthAlly(player, range);
            if (target == null) {
                target = this.findNearestDownedPlayer(player, range);
                if (target == null) {
                    player.sendSystemMessage(Component.translatable("item.medic_kit.no_target"));
                    return stack;
                }
            }

            final Player finalTarget = target;
            InjuryHelper.getCapability(finalTarget).ifPresent(cap -> {
                if(cap.isDowned()) {
                    cap.setDowned(false, null);
                    cap.setDownedUntil(0L);
                    InjuryEvents.clearDownedSource(finalTarget.getUUID());
                    finalTarget.setHealth(finalTarget.getMaxHealth() * 0.5f); // Restore to half health
                    finalTarget.setPose(net.minecraft.world.entity.Pose.STANDING);
                    finalTarget.sendSystemMessage(Component.literal("Você foi revivido por um médico!").withStyle(ChatFormatting.GREEN));
                    InjuryNetwork.sendToPlayer(new UpdateDownedStatePacket(false, 0L), (ServerPlayer) finalTarget);
                } else {
                    float healAmount = hearts * 2.0f; // hearts to health points
                    float newHealth = Math.min(finalTarget.getMaxHealth(), finalTarget.getHealth() + healAmount);
                    finalTarget.setHealth(newHealth);
                }
            });


            if (level instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.HEART, target.getX(), target.getY() + 1.0, target.getZ(), 15, 0.5, 0.5, 0.5, 0.02);
            }

            level.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1.0F, 1.5F);

            player.sendSystemMessage(Component.literal("§aAlvo curado!"));
            if (target != player) {
                target.sendSystemMessage(Component.literal("§aVocê foi curado por um médico!"));
            }

            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }

            return stack;
         }
      }

      return stack;
   }

   @Override
   public UseAnim getUseAnimation(ItemStack stack) {
      return UseAnim.BOW;
   }

   @Override
   public int getUseDuration(ItemStack stack) {
      return 30;
   }

   @Override
   public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
      tooltip.add(Component.translatable("item.doomsdayessentials.medic_kit.lore1").withStyle(ChatFormatting.GRAY));
      tooltip.add(Component.translatable("item.doomsdayessentials.medic_kit.lore2").withStyle(ChatFormatting.GOLD));
      super.appendHoverText(stack, level, tooltip, flag);
      tooltip.add(Component.translatable("item.doomsdayessentials.medic_kit.desc1").withStyle(ChatFormatting.GRAY));
      tooltip.add(Component.translatable("item.doomsdayessentials.medic_kit.desc2").withStyle(ChatFormatting.DARK_GREEN));
   }

   private boolean isMedico(Player player) {
      String profession = ProfissaoManager.getProfession(player.getUUID());
      return profession != null && profession.equalsIgnoreCase("medico");
   }

   private Player findNearestDownedPlayer(Player source, double range) {
       Player nearest = null;
       double nearestDist = Double.MAX_VALUE;
       for(Player player : source.level().players()) {
           if (player != source && player.distanceToSqr(source) <= range * range) {
               boolean isDowned = InjuryHelper.getCapability(player).map(InjuryCapability::isDowned).orElse(false);
               if (isDowned) {
                   double dist = player.distanceToSqr(source);
                   if (dist < nearestDist) {
                       nearestDist = dist;
                       nearest = player;
                   }
               }
           }
       }
       return nearest;
   }

   private Player findNearestInjuredPlayer(Player source, double range) {
      Player nearest = null;
      double nearestDist = Double.MAX_VALUE;
      Iterator var7 = source.level().players().iterator();

      while(var7.hasNext()) {
         Player player = (Player)var7.next();
         if (player != source && player.distanceToSqr(source) <= range * range) {
            boolean isInjured = InjuryHelper.getCapability(player).map((cap) -> {
               return cap.getInjuryLevel() > 0;
            }).orElse(false);
            if (isInjured) {
               double dist = player.distanceToSqr(source);
               if (dist < nearestDist) {
                  nearestDist = dist;
                  nearest = player;
               }
            }
         }
      }

      return nearest;
   }

   private Player findNearestLowHealthAlly(Player source, double range) {
      Player nearest = null;
      double nearestDist = Double.MAX_VALUE;
      Iterator var7 = source.level().players().iterator();

      while(var7.hasNext()) {
         Player player = (Player)var7.next();
         if (player != source && player.distanceToSqr(source) <= range * range) {
            boolean isInjured = InjuryHelper.getCapability(player).map((cap) -> {
               return cap.getInjuryLevel() > 0;
            }).orElse(false);
            if (isInjured) {
               double dist = player.distanceToSqr(source);
               if (dist < nearestDist) {
                  nearestDist = dist;
                  nearest = player;
               }
            }
         }
      }

      return nearest;
   }
} 