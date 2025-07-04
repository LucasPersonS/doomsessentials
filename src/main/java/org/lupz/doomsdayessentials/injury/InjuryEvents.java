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
import org.lupz.doomsdayessentials.injury.network.UpdateInjuryLevelPacket;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraftforge.eventbus.api.EventPriority;

@EventBusSubscriber(modid = EssentialsMod.MOD_ID, bus = EventBusSubscriber.Bus.FORGE)
public class InjuryEvents {
   private static final Map<UUID, InjuryDeathData> deadPlayerInjuries = new HashMap<>();
   private static final Map<UUID, DamageSource> downedPlayerOriginalSources = new HashMap<>();
   private static final Map<UUID, ReviveData> reviveProgress = new HashMap<>();
   private static final int REVIVE_DURATION_TICKS = 600; // 30 seconds

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

            // Handle ongoing revive progress if this player is reviving someone
            ReviveData data = reviveProgress.get(player.getUUID());
            if (data != null) {
               ServerPlayer reviver = player;
               ServerPlayer target = reviver.server.getPlayerList().getPlayer(data.targetUUID);

               // Validate conditions
               if (target == null || !InjuryHelper.getCapability(target).map(InjuryCapability::isDowned).orElse(false)
                       || reviver.distanceToSqr(target) > 9
                       || !reviver.isUsingItem()) { // Stop if player released right-click
                  reviveProgress.remove(reviver.getUUID());
                  reviver.sendSystemMessage(net.minecraft.network.chat.Component.literal("§cReanimação cancelada."));
               } else {
                  data.increment();
                  if (data.ticks % 20 == 0) { // Update every second
                     int secondsLeft = Math.max(0, (REVIVE_DURATION_TICKS - data.ticks) / 20);
                     reviver.displayClientMessage(net.minecraft.network.chat.Component.literal("§eReanimando... " + secondsLeft + "s"), true);
                  }

                  if (data.isComplete()) {
                     // Complete revive
                     InjuryHelper.revivePlayer(target);
                     InjuryEvents.clearDownedSource(target.getUUID());
                     target.setHealth(Math.max(target.getMaxHealth() * 0.25f, 6f));

                     reviver.sendSystemMessage(net.minecraft.network.chat.Component.literal("§aVocê reviveu " + target.getDisplayName().getString()));
                     target.sendSystemMessage(net.minecraft.network.chat.Component.literal("§aVocê foi reanimado por " + reviver.getDisplayName().getString()));

                     if (reviver.level() instanceof net.minecraft.server.level.ServerLevel srv) {
                        srv.sendParticles(net.minecraft.core.particles.ParticleTypes.HEART, target.getX(), target.getY() + 1.0, target.getZ(), 10, 0.5, 0.5, 0.5, 0.02);
                     }

                     reviveProgress.remove(reviver.getUUID());
                  }
               }
            }
         });
      }
   }

   // Run at the very beginning so other mods never see the (now-cancelled) death
   // and therefore don't spawn 'ghost' corpses/items. Skips if someone else has
   // already cancelled it for safety.
   @SubscribeEvent(priority = EventPriority.HIGHEST)
   public static void onPlayerDeath(LivingDeathEvent event) {
      if (event.isCanceled()) {
         return;
      }

      if (event.getEntity().level().isClientSide || !(event.getEntity() instanceof ServerPlayer player)) {
         return;
      }

      if (!EssentialsConfig.INJURY_SYSTEM_ENABLED.get()) {
         return;
      }

      if (player.getPersistentData().contains("doomsessentials_force_death")) {
         // Ignore further death processing while waiting for respawn
         return;
      }

      // If the player is already downed this is the finishing blow handled by our system.
      // Cancel vanilla death to avoid duplicate messages and process the kill ourselves.
      if (InjuryHelper.getCapability(player).map(InjuryCapability::isDowned).orElse(false)) {
         event.setCanceled(true);
         killPlayer(player, event.getSource());
         // Mark that we've already sent a death message for this forced-death sequence
         player.getPersistentData().putBoolean("doomsessentials_death_msg", true);
         return;
      }

      // If this death is part of a forced sequence and a message was already sent, cancel to avoid duplicates
      if (player.getPersistentData().getBoolean("doomsessentials_death_msg")) {
         event.setCanceled(true);
         return;
      }

      // Cancel the vanilla death and enter the downed state instead.
      event.setCanceled(true);
      InjuryHelper.downPlayer(player, event.getSource().getEntity() instanceof Player ? (Player) event.getSource().getEntity() : null);
      player.setHealth(1.0f);
   }

   @SubscribeEvent
   public static void onPlayerHurt(LivingHurtEvent event) {
      if (event.getEntity().level().isClientSide || !(event.getEntity() instanceof ServerPlayer player)) {
         return;
      }

      // If we're in the middle of a forced death sequence ignore additional damage events
      if (player.getPersistentData().getBoolean("doomsessentials_force_death")) {
          return;
      }

      InjuryHelper.getCapability(player).ifPresent(cap -> {
         // (early lethal-to-downed conversion removed; handled in onPlayerDeath now)

         if (cap.isDowned()) {
            if (player.getPersistentData().contains("doomsessentials_force_death")) {
               return;
            }
            // Instead of instant death, subtract from the configurable downed health pool.
            float remaining = cap.getDownedHealth() - event.getAmount();
            cap.setDownedHealth(remaining);

            // Cancel the actual damage so we control the finishing blow logic
            event.setCanceled(true);

            if (remaining <= 0.0f) {
               // Health pool depleted – finalize the player preserving attacker info
               killPlayer(player, event.getSource());
            } else {
               // Optional: you could add feedback here (sound/particles) to indicate progress
            }
         }
      });
   }

   @SubscribeEvent
   public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
      if (!event.getEntity().level().isClientSide) {
         Player player = event.getEntity();
         System.out.println("[DEBG] onPlayerRespawn START: player=" + player.getName().getString());
         InjuryHelper.getCapability(player).ifPresent(cap -> {
            System.out.println("[DEBG] onPlayerRespawn INITIAL: level=" + cap.getInjuryLevel());
         });

         InjuryHelper.revivePlayer(player);
         player.getPersistentData().remove("doomsessentials_force_death");
         player.getPersistentData().remove("doomsessentials_already_killed");
         player.getPersistentData().remove("doomsessentials_death_msg"); // clear duplicate-message flag on respawn

         // Force-restore the injury level from persistent data
         if (player instanceof ServerPlayer serverPlayer) {
            int savedLevel = 0;
            
            // Try to get from player's persistent data first
            if (serverPlayer.getPersistentData().contains("doomsessentials_injury_level_copy")) {
                savedLevel = serverPlayer.getPersistentData().getInt("doomsessentials_injury_level_copy");
                System.out.println("[DEBG] onPlayerRespawn RESTORE_FROM_PERSISTENT: level=" + savedLevel);
            }
            
            // If we found a saved level, force-set it
            if (savedLevel > 0) {
                InjuryHelper.setInjuryLevel(serverPlayer, savedLevel);
                System.out.println("[DEBG] onPlayerRespawn FORCED_RESTORE: level=" + savedLevel);
            }
         }

         // Sync injury level to the client immediately after respawn
         if (player instanceof ServerPlayer serverPlayer) {
            InjuryHelper.getCapability(serverPlayer).ifPresent(cap -> {
               System.out.println("[DEBG] onPlayerRespawn BEFORE_SYNC: level=" + cap.getInjuryLevel());
               InjuryNetwork.sendToPlayer(new UpdateInjuryLevelPacket(cap.getInjuryLevel()), serverPlayer);

               // Re-apply any injury effects now so they are active right after respawn
               if (cap.getInjuryLevel() > 0) {
                  InjuryHelper.applyEffects(serverPlayer);
               }

               // Fallback: if level is 0 but we have saved copy tag, restore
               if (cap.getInjuryLevel() == 0 && serverPlayer.getPersistentData().contains("doomsessentials_injury_level_copy")) {
                  int saved = serverPlayer.getPersistentData().getInt("doomsessentials_injury_level_copy");
                  cap.setInjuryLevel(saved);
                  System.out.println("[DEBG] onPlayerRespawn FALLBACK_RESTORE: saved=" + saved);
                  InjuryNetwork.sendToPlayer(new UpdateInjuryLevelPacket(saved), serverPlayer);
               }

               System.out.println("[DEBG] respawn: level = " +
                    InjuryHelper.getCapabilityUnwrap(serverPlayer).getInjuryLevel());
            });
         }
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

   @SubscribeEvent
   public static void onPlayerClone(PlayerEvent.Clone event) {
      // Revive capabilities on the ORIGINAL (dead) player so we can access them here
      event.getOriginal().reviveCaps();
      System.out.println("[DEBG] onPlayerClone START: original=" + event.getOriginal().getName().getString() + 
                        ", new=" + event.getEntity().getName().getString() + 
                        ", death=" + event.isWasDeath());

      // Use an array to store the injury level so we can access it from lambdas
      final int[] originalInjuryLevel = {0};
      event.getOriginal().getCapability(InjuryCapabilityProvider.INJURY_CAPABILITY).ifPresent(oldCap -> {
         originalInjuryLevel[0] = oldCap.getInjuryLevel();
         System.out.println("[DEBG] onPlayerClone ORIGINAL_CAP: level=" + oldCap.getInjuryLevel());
         event.getEntity().getCapability(InjuryCapabilityProvider.INJURY_CAPABILITY).ifPresent(newCap -> {
            newCap.deserializeNBT(oldCap.serializeNBT());
            System.out.println("[DEBG] clone: copied level = " + newCap.getInjuryLevel());

            // Copy the persistent injury tag as well
            if (event.getOriginal().getPersistentData().contains("doomsessentials_injury_level_copy")) {
                int saved = event.getOriginal().getPersistentData().getInt("doomsessentials_injury_level_copy");
                event.getEntity().getPersistentData().putInt("doomsessentials_injury_level_copy", saved);
                System.out.println("[DEBG] onPlayerClone copied persistent injury tag: " + saved);
            }

            // Immediately sync the injury level to the client so the HUD is correct right after respawn
            if (event.getEntity() instanceof ServerPlayer sp) {
               System.out.println("[DEBG] onPlayerClone SENDING_PACKET: level=" + newCap.getInjuryLevel());
               InjuryNetwork.sendToPlayer(new UpdateInjuryLevelPacket(newCap.getInjuryLevel()), sp);
            }
         });
      });
      
      // Check final state after clone
      if (event.getEntity() instanceof ServerPlayer sp) {
          InjuryHelper.getCapability(sp).ifPresent(cap -> {
              System.out.println("[DEBG] onPlayerClone FINAL: level=" + cap.getInjuryLevel());
              
              // If this was a death and the injury level is now 0 but was higher before, restore it
              if (event.isWasDeath() && cap.getInjuryLevel() == 0 && originalInjuryLevel[0] > 0) {
                  System.out.println("[DEBG] onPlayerClone EMERGENCY_RESTORE: level was reset to 0, restoring " + originalInjuryLevel[0]);
                  cap.setInjuryLevel(originalInjuryLevel[0]);
                  InjuryNetwork.sendToPlayer(new UpdateInjuryLevelPacket(originalInjuryLevel[0]), sp);
                  
                  // Also save to persistent data as backup
                  sp.getPersistentData().putInt("doomsessentials_injury_level_copy", originalInjuryLevel[0]);
              }
          });
      }
   }

   public static void killPlayer(ServerPlayer player, @javax.annotation.Nullable DamageSource originalSource) {
      // Prevent recursive calls or duplicate death handling
      if (player.getPersistentData().getBoolean("doomsessentials_force_death") ||
          player.getPersistentData().getBoolean("doomsessentials_already_killed")) {
         return;
      }

      // If the player is already in the process of dying / has zero health, avoid triggering a second death.
      if (player.isDeadOrDying()) {
          return;
      }

      InjuryHelper.getCapability(player).ifPresent(cap -> {
         if (cap.isDowned()) {
            // Clear downed flag first to avoid loops
            cap.setDowned(false, null);

            // Mark intentional kill and remember we've processed the kill to avoid duplicate messages
            player.getPersistentData().putBoolean("doomsessentials_force_death", true);
            player.getPersistentData().putBoolean("doomsessentials_already_killed", true);

            DamageSource src = originalSource != null ? originalSource : player.damageSources().fellOutOfWorld();

            // Avoid double-counting deaths: process injury increment only once per death sequence.
            if (!player.getPersistentData().getBoolean("doomsessentials_injury_processed")) {
               player.getPersistentData().putBoolean("doomsessentials_injury_processed", true);

               // Increment injury level *before* the actual death so it is copied correctly to the new player instance
               boolean pvpOnly = EssentialsConfig.INJURY_PVP_ONLY.get();
               boolean causedByPlayer = src.getEntity() instanceof Player && src.getEntity() != player;
               if (!pvpOnly || causedByPlayer) {
                  InjuryHelper.incrementInjuryLevel(player);
               }
            }

            // Persist the injury level through persistent data in case Clone event fails
            player.getPersistentData().putInt("doomsessentials_injury_level_copy", cap.getInjuryLevel());
            // Make sure the injury level is saved to persistent data
            System.out.println("[DEBG] killPlayer: saved injury level to persistent data: " + cap.getInjuryLevel());

            System.out.println("[DEBG] killPlayer: level now = " +
                 InjuryHelper.getCapabilityUnwrap(player).getInjuryLevel());

            // Lethal damage preserving killer attribution when possible
            player.hurt(src, Float.MAX_VALUE);
         }
      });
   }

   // Convenience overload
   public static void killPlayer(ServerPlayer player) {
      killPlayer(player, null);
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

   // ADD: Start reviving a downed teammate when right-clicking them
   @SubscribeEvent
   public static void onPlayerInteractEntity(net.minecraftforge.event.entity.player.PlayerInteractEvent.EntityInteract event) {
      if (event.getEntity().level().isClientSide()) return; // Server side only
      if (!(event.getTarget() instanceof Player target)) return;

      Player reviver = event.getEntity();
      // Ignore self-interaction
      if (reviver.getUUID().equals(target.getUUID())) return;

      InjuryHelper.getCapability(target).ifPresent(cap -> {
         if (cap.isDowned()) {
            // Begin revive process and remember which hand is being used
            reviveProgress.put(reviver.getUUID(), new ReviveData(target.getUUID(), event.getHand()));
            // Force the server to start tracking the right-click "use" action so we can detect release
            if (reviver instanceof net.minecraft.server.level.ServerPlayer sp) {
               sp.startUsingItem(event.getHand());
            }
            reviver.sendSystemMessage(net.minecraft.network.chat.Component.literal("§eReanimando " + target.getDisplayName().getString() + "..."));
            event.setCanceled(true); // Prevent other interactions
         }
      });
   }

   // Clean up revive data if the reviver logs out (or crashes)
   @SubscribeEvent
   public static void onPlayerLogout(net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent event) {
       reviveProgress.remove(event.getEntity().getUUID());
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

   private static class ReviveData {
      private final UUID targetUUID;
      private final net.minecraft.world.InteractionHand hand;
      private int ticks;

      private ReviveData(UUID targetUUID, net.minecraft.world.InteractionHand hand) {
         this.targetUUID = targetUUID;
         this.hand = hand;
         this.ticks = 0;
      }

      private void increment() {
         this.ticks++;
      }

      private boolean isComplete() {
         return this.ticks >= REVIVE_DURATION_TICKS;
      }
   }
} 