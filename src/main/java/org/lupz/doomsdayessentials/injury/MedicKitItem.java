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
import org.lupz.doomsdayessentials.config.EssentialsConfig;
import org.lupz.doomsdayessentials.injury.capability.InjuryCapability;
import org.lupz.doomsdayessentials.injury.network.InjuryNetwork;
import org.lupz.doomsdayessentials.injury.network.UpdateDownedStatePacket;
import org.lupz.doomsdayessentials.professions.ProfissaoManager;
import org.lupz.doomsdayessentials.injury.InjuryEvents;
import org.lupz.doomsdayessentials.EssentialsMod;
import net.minecraft.world.phys.AABB;

public class MedicKitItem extends Item {
   public MedicKitItem(Item.Properties properties) {
      super(properties);
   }

   @Override
   public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
      ItemStack stack = player.getItemInHand(hand);
      if (!isMedico(player)) {
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
            double range = EssentialsConfig.MEDIC_HEAL_RADIUS.get();
            int hearts = EssentialsConfig.MEDIC_HEAL_AMOUNT.get().intValue();
            float healAmount = hearts; // Convert hearts to half-hearts

            AABB area = new AABB(player.blockPosition()).inflate(range);
            level.getEntitiesOfClass(Player.class, area).forEach(p -> {
                if (p != player && player.distanceToSqr(p) <= range * range) { // Check spherical radius & not self
                    InjuryHelper.getCapability(p).ifPresent(cap -> {
                        boolean healed = false;
                        if(cap.isDowned()) {
                            cap.setDowned(false, null);
                            cap.setDownedUntil(0L);
                            InjuryEvents.clearDownedSource(p.getUUID());
                            p.setHealth(p.getMaxHealth() * 0.5f); // Restore to half health
                            p.setPose(net.minecraft.world.entity.Pose.STANDING);
                            p.sendSystemMessage(Component.translatable("item.doomsdayessentials.medic_kit.revived_by_medic", player.getDisplayName()).withStyle(ChatFormatting.GREEN));
                            InjuryNetwork.sendToPlayer(new UpdateDownedStatePacket(false, 0L), (ServerPlayer) p);
                            player.sendSystemMessage(Component.translatable("item.doomsdayessentials.medic_kit.revived_player", p.getDisplayName()).withStyle(ChatFormatting.GREEN));
                            healed = true;
                        } else if (p.getHealth() < p.getMaxHealth()) {
                            float newHealth = Math.min(p.getMaxHealth(), p.getHealth() + healAmount);
                            p.setHealth(newHealth);
                            player.sendSystemMessage(Component.translatable("item.doomsdayessentials.medic_kit.healed_player", p.getDisplayName()).withStyle(ChatFormatting.GREEN));
                            healed = true;
                        }

                        if (healed) {
                            p.sendSystemMessage(Component.translatable("item.doomsdayessentials.medic_kit.healed_by_medic", player.getDisplayName()).withStyle(ChatFormatting.GREEN));
                        }
                    });
                }
            });

            if (level instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.HEART, player.getX(), player.getY() + 1.0, player.getZ(), 15, 0.5, 0.5, 0.5, 0.02);
            }

            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1.0F, 1.5F);

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
} 