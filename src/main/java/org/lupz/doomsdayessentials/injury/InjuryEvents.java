package org.lupz.doomsdayessentials.injury;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import org.lupz.doomsdayessentials.EssentialsMod;
import org.lupz.doomsdayessentials.config.EssentialsConfig; // Assuming general config
import org.lupz.doomsdayessentials.injury.capability.InjuryCapabilityProvider;
import org.lupz.doomsdayessentials.injury.network.DisplayInjuryTitlePacket;
import org.lupz.doomsdayessentials.injury.network.InjuryNetwork;
import org.lupz.doomsdayessentials.injury.network.UpdateHealingProgressPacket;
import org.lupz.doomsdayessentials.injury.network.UpdateInjuryLevelPacket;
import org.lupz.doomsdayessentials.professions.MedicoProfession;

@EventBusSubscriber(modid = EssentialsMod.MOD_ID)
public class InjuryEvents {
   private static final Map<UUID, InjuryDeathData> deadPlayerInjuries = new HashMap<>();

   @SubscribeEvent
   public static void onAttachCapabilities(AttachCapabilitiesEvent<Entity> event) {
      if (event.getObject() instanceof Player player) {
         if (!player.getCapability(InjuryCapabilityProvider.INJURY_CAPABILITY).isPresent()) {
            InjuryCapabilityProvider provider = new InjuryCapabilityProvider();
            event.addCapability(InjuryCapabilityProvider.INJURY_CAPABILITY_ID, provider);
         }
      }
   }

   @SubscribeEvent
   public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
      if (event.phase == Phase.END && !event.player.level().isClientSide) {
         Player player = event.player;
         InjuryHelper.applyEffects(player);
         // Update medico profession cooldowns & messages
         MedicoProfession.tickMedico(player);
         if (isOnHealingBed(player)) {
            InjuryHelper.onHealingBed(player);
         } else {
            InjuryHelper.getCapability(player).ifPresent((cap) -> {
               if (cap.getHealCooldown() > 0) {
                  cap.setHealCooldown(0);
                  if (player instanceof ServerPlayer serverPlayer) {
                     InjuryNetwork.sendToPlayer(new UpdateHealingProgressPacket(-1.0F), serverPlayer);
                  }
               }
            });
         }
      }
   }

   private static boolean isOnHealingBed(Player player) {
      if (!player.isSleeping()) {
         return false;
      } else {
         BlockPos pos = player.getSleepingPos().orElse(null);
         if (pos == null) return false;
         BlockPos belowPos = pos.below();
         Level level = player.level();
         return level.getBlockState(pos).is(BlockTags.BEDS) && level.getBlockState(belowPos).is(Blocks.DIAMOND_BLOCK);
      }
   }

   @SubscribeEvent
   public static void onPlayerDeath(LivingDeathEvent event) {
      if (event.getEntity() instanceof ServerPlayer player) {
         if (!EssentialsConfig.INJURY_SYSTEM_ENABLED.get()) {
            return;
         }

         player.getCapability(InjuryCapabilityProvider.INJURY_CAPABILITY).ifPresent((cap) -> {
            int currentLevel = cap.getInjuryLevel();
            String deathSource = event.getSource().getMsgId();
            Entity killerEntity = event.getSource().getEntity();
            boolean pvpDeath = killerEntity instanceof Player && killerEntity != player;
            boolean isSuicide = deathSource.equals("outOfWorld") || deathSource.equals("suicide") || deathSource.equals("kill") || (deathSource.equals("generic") && killerEntity == null) || killerEntity == player || deathSource.equals("drown");
            boolean isMonster = killerEntity instanceof Monster;
            boolean shouldIncrement = pvpDeath || ((isSuicide || isMonster) && !EssentialsConfig.INJURY_PVP_ONLY.get());
            boolean showTitle = pvpDeath || isSuicide || isMonster;
            if (shouldIncrement) {
               int newDeathCount = cap.incrementDeathCount();
               if (newDeathCount >= EssentialsConfig.DEATHS_PER_INJURY_LEVEL.get()) {
                  currentLevel = Math.min(currentLevel + 1, EssentialsConfig.MAX_INJURY_LEVEL.get());
                  cap.setDeathCount(0);
               }
            }

            deadPlayerInjuries.put(player.getUUID(), new InjuryDeathData(currentLevel, cap.getDeathCount(), showTitle));
            EssentialsMod.LOGGER.info("Player {} died. Storing injury data for respawn. Level: {}, Deaths: {}, Death source: {}, Is suicide: {}", player.getName().getString(), currentLevel, cap.getDeathCount(), deathSource, isSuicide);
         });
      }
   }

   @SubscribeEvent
   public static void onPlayerClone(PlayerEvent.Clone event) {
      if (event.isWasDeath()) {
          event.getOriginal().getCapability(InjuryCapabilityProvider.INJURY_CAPABILITY).ifPresent(oldCap -> {
              event.getEntity().getCapability(InjuryCapabilityProvider.INJURY_CAPABILITY).ifPresent(newCap -> {
                  newCap.deserializeNBT(oldCap.serializeNBT());
              });
          });
      }
   }

   @SubscribeEvent
   public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
      if (event.getEntity() instanceof ServerPlayer player) {
         UUID playerUUID = player.getUUID();
         if (deadPlayerInjuries.containsKey(playerUUID)) {
            InjuryDeathData data = deadPlayerInjuries.remove(playerUUID);
            player.getCapability(InjuryCapabilityProvider.INJURY_CAPABILITY).ifPresent((cap) -> {
               cap.setInjuryLevel(data.getInjuryLevel());
               cap.setDeathCount(data.getDeathCount());
               InjuryNetwork.sendToPlayer(new UpdateInjuryLevelPacket(data.getInjuryLevel()), player);
               EssentialsMod.LOGGER.info("Restored injury data for player {}: Level {}, Deaths {}", player.getName().getString(), data.getInjuryLevel(), data.getDeathCount());
               if (data.shouldShowTitle()) {
                  InjuryNetwork.sendToPlayer(new DisplayInjuryTitlePacket(data.getInjuryLevel()), player);
               }
            });
         }
      }
   }

   // This was empty in the decompiled code.
   @SubscribeEvent
   public static void onItemUse(LivingEntityUseItemEvent.Finish event) {
      if (event.getEntity() instanceof ServerPlayer) {
          // Empty
      }
   }

   static class InjuryDeathData {
      private final int injuryLevel;
      private final int deathCount;
      private final boolean showTitle;

      public InjuryDeathData(int injuryLevel, int deathCount, boolean showTitle) {
         this.injuryLevel = injuryLevel;
         this.deathCount = deathCount;
         this.showTitle = showTitle;
      }

      public int getInjuryLevel() {
         return this.injuryLevel;
      }

      public int getDeathCount() {
         return this.deathCount;
      }

      public boolean shouldShowTitle() {
         return this.showTitle;
      }
   }
} 