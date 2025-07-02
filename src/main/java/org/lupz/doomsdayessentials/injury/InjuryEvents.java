package org.lupz.doomsdayessentials.injury;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import org.lupz.doomsdayessentials.EssentialsMod;
import org.lupz.doomsdayessentials.config.EssentialsConfig; // Assuming general config
import org.lupz.doomsdayessentials.injury.capability.InjuryCapability;
import org.lupz.doomsdayessentials.injury.capability.InjuryCapabilityProvider;
import org.lupz.doomsdayessentials.injury.network.InjuryNetwork;
import net.minecraft.world.damagesource.DamageSource;

@EventBusSubscriber(modid = EssentialsMod.MOD_ID)
public class InjuryEvents {
   private static final Map<UUID, InjuryDeathData> deadPlayerInjuries = new HashMap<>();
   private static final Map<UUID, DamageSource> downedPlayerOriginalSources = new HashMap<>();

   public static void clearDownedSource(UUID playerUUID) {
      downedPlayerOriginalSources.remove(playerUUID);
   }

   @SubscribeEvent
   public static void onAttachCapabilities(AttachCapabilitiesEvent<Entity> event) {
      if (event.getObject() instanceof Player && !event.getObject().getCapability(InjuryCapabilityProvider.INJURY_CAPABILITY).isPresent()) {
         event.addCapability(InjuryCapabilityProvider.INJURY_CAPABILITY_ID, new InjuryCapabilityProvider());
      }
   }

   @SubscribeEvent
   public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
      if (event.phase == Phase.END && !event.player.level().isClientSide && event.player instanceof ServerPlayer player) {

         if (player.getPersistentData().getBoolean("healingBedLock")) {
            int x = player.getPersistentData().getInt("healingBedX");
            int y = player.getPersistentData().getInt("healingBedY");
            int z = player.getPersistentData().getInt("healingBedZ");
            BlockPos bedPos = new BlockPos(x, y, z);

            if (player.position().distanceToSqr(bedPos.getX() + 0.5, bedPos.getY() + 0.5, bedPos.getZ() + 0.5) > 2.0) {
               player.teleportTo(bedPos.getX() + 0.5, bedPos.getY() + 0.5, bedPos.getZ() + 0.5);
            }
            player.setDeltaMovement(0, 0, 0);

            InjuryHelper.onHealingBed(player);
            return;
         }

         InjuryHelper.getCapability(player).ifPresent(cap -> {
            if (cap.isDowned()) {
               if (System.currentTimeMillis() > cap.getDownedUntil()) {
                  killPlayer(player);
                  return; 
               }
            }

            if (cap.getInjuryLevel() > 0) {
               InjuryHelper.applyEffects(player);
            }
         });
      }
   }

   @SubscribeEvent
   public static void onPlayerDeath(LivingDeathEvent event) {
      if (event.getEntity().level().isClientSide || !(event.getEntity() instanceof ServerPlayer player)) {
         return;
      }

      if (!EssentialsConfig.INJURY_SYSTEM_ENABLED.get()) {
         return;
      }

      if (player.getPersistentData().contains("doomsessentials_force_death")) {
         player.getPersistentData().remove("doomsessentials_force_death");
         InjuryHelper.getCapability(player).ifPresent(cap -> cap.setDowned(false, null));
         return;
      }

      if (InjuryHelper.getCapability(player).map(cap -> cap.isDowned()).orElse(false)) {
         return; 
      }

      event.setCanceled(true);
      InjuryHelper.downPlayer(player, event.getSource().getEntity() instanceof Player ? (Player) event.getSource().getEntity() : null);
      player.setHealth(1.0f);
   }

   @SubscribeEvent
   public static void onPlayerHurt(LivingHurtEvent event) {
      if (event.getEntity().level().isClientSide || !(event.getEntity() instanceof ServerPlayer player)) {
         return;
      }

      InjuryHelper.getCapability(player).ifPresent(cap -> {
         if (cap.isDowned()) {
            if (player.getPersistentData().contains("doomsessentials_force_death")) {
               return;
            }
            // Any damage taken while downed is fatal
            killPlayer(player);
            event.setCanceled(true);
         }
      });
   }

   @SubscribeEvent
   public static void onPlayerClone(PlayerEvent.Clone event) {
      if (event.isWasDeath()) {
         event.getOriginal().getCapability(InjuryCapabilityProvider.INJURY_CAPABILITY).ifPresent(oldCap -> {
            InjuryHelper.getCapability(event.getEntity()).ifPresent(newCap -> {
               newCap.setInjuryLevel(oldCap.getInjuryLevel());
               if (EssentialsConfig.RESET_INJURY_ON_DEATH.get()) {
                  newCap.setInjuryLevel(0);
               }
            });
         });
      }
   }

   @SubscribeEvent
   public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
      if (!event.getEntity().level().isClientSide) {
         InjuryHelper.revivePlayer(event.getEntity());
      }
   }

   @SubscribeEvent
   public static void onPlayerLogin(PlayerLoggedInEvent event) {
      if (!event.getEntity().level().isClientSide) {
         ServerPlayer player = (ServerPlayer) event.getEntity();
         InjuryHelper.getCapability(player).ifPresent(cap -> {
            InjuryNetwork.sendToPlayer(new UpdateInjuryLevelPacket(cap.getInjuryLevel()), player);
         });
      }
   }

   public static void killPlayer(ServerPlayer player) {
      InjuryHelper.getCapability(player).ifPresent(cap -> {
         if(cap.isDowned()) {
            player.getPersistentData().putBoolean("doomsessentials_force_death", true);
            player.hurt(player.damageSources().fellOutOfWorld(), Float.MAX_VALUE);
         }
      });
   }

   private static boolean isEnvironmentalOrSuicide(DamageSource source, Entity killer, Player player) {
      String deathSource = source.getMsgId();
      return deathSource.equals("outOfWorld") ||
              deathSource.equals("suicide") ||
              deathSource.equals("kill") ||
              (deathSource.equals("generic") && killer == null) ||
              killer == player ||
              deathSource.equals("drown") ||
              deathSource.equals("fall") ||
              deathSource.equals("lava") ||
              deathSource.equals("inFire") ||
              deathSource.equals("onFire") ||
              deathSource.equals("inWall") ||
              deathSource.equals("starve") ||
              deathSource.equals("cramming") ||
              deathSource.equals("flyIntoWall") ||
              deathSource.equals("hotFloor") ||
              deathSource.equals("sweetBerryBush") ||
              deathSource.equals("freeze") ||
              deathSource.equals("stalagmite");
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