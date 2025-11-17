package org.lupz.doomsdayessentials.professions;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import org.lupz.doomsdayessentials.config.EssentialsConfig;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import org.lupz.doomsdayessentials.professions.items.ProfessionItems;
import java.util.List;
import net.minecraft.ChatFormatting;
import org.lupz.doomsdayessentials.professions.capability.TrackerCapabilityProvider;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Rarity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class RastreadorProfession {
    private static final String TAG_IS_TRACKER = "isRastreador";
    private static final String TAG_TRAP_COOLDOWN = "rastreadorTrapCooldown";
    private static final String TAG_ZONE_COOLDOWN = "rastreadorZoneCooldown";
    
    // Active traps: position -> expiration time
    private static final Map<BlockPos, Long> ACTIVE_TRAPS = new HashMap<>();
    // Active zones: player UUID -> zone info
    private static final Map<UUID, ZoneInfo> ACTIVE_ZONES = new HashMap<>();
    
    private record ZoneInfo(BlockPos center, long expiresAt) {}

    private RastreadorProfession() {}

    public static void onBecome(Player player) {
        if (player.level().isClientSide) return;
        if (!EssentialsConfig.RASTREADOR_ENABLED.get()) {
            player.sendSystemMessage(Component.translatable("profession.rastreador.disabled"));
            return;
        }
        
        // Set tracker tag FIRST
        player.getPersistentData().putBoolean(TAG_IS_TRACKER, true);
        
        // Send confirmation message
        player.sendSystemMessage(Component.translatable("profession.rastreador.become"));

        // Give tracker compass
        player.getInventory().add(new ItemStack(ProfessionItems.TRACKING_COMPASS.get()));

        // Apply passive bonuses (night vision)
        applyBonuses(player);
    }

    public static void onLeave(Player player) {
        if (player.level().isClientSide) return;
        player.getPersistentData().putBoolean(TAG_IS_TRACKER, false);
        player.getPersistentData().remove(TAG_TRAP_COOLDOWN);
        player.getPersistentData().remove(TAG_ZONE_COOLDOWN);
        ACTIVE_ZONES.remove(player.getUUID());
        player.sendSystemMessage(Component.translatable("profession.rastreador.leave"));

        // Remove tracker compass from inventory
        player.getInventory().items.removeIf(item -> item.getItem() == ProfessionItems.TRACKING_COMPASS.get());

        // Remove passive effects
        MobEffectInstance effect = player.getEffect(MobEffects.NIGHT_VISION);
        if (effect != null) player.removeEffect(MobEffects.NIGHT_VISION);
    }

    public static boolean isTracker(Player player) {
        return player.getPersistentData().getBoolean(TAG_IS_TRACKER);
    }

    /**
     * Apply passive bonuses (called on become and on login/respawn).
     */
    public static void applyBonuses(Player player) {
        // Set the tag to ensure consistency with manager state
        player.getPersistentData().putBoolean(TAG_IS_TRACKER, true);
        
        if (!player.hasEffect(MobEffects.NIGHT_VISION)) {
            player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, Integer.MAX_VALUE, 0, true, false, false));
        }
    }

    /* -------------------------------------------------------------------------
     * Passive: Highlight rare loot within 15 blocks
     * ---------------------------------------------------------------------- */
    private static void highlightRareLoot(Player player) {
        if (player.level().isClientSide) return;
        double radius = 15.0;
        List<ItemEntity> items = player.level().getEntitiesOfClass(ItemEntity.class, 
            player.getBoundingBox().inflate(radius));
        
        for (ItemEntity item : items) {
            ItemStack stack = item.getItem();
            // Highlight rare, epic items or items with enchantments
            if (stack.getRarity() == Rarity.RARE || stack.getRarity() == Rarity.EPIC || 
                stack.isEnchanted() || stack.getRarity() == Rarity.UNCOMMON) {
                // Use Entity's built-in glowing flag
                item.setGlowingTag(true);
            } else {
                // Remove glowing from non-rare items
                item.setGlowingTag(false);
            }
        }
    }

    /* -------------------------------------------------------------------------
     * Active ability – Rede de Caça (Hunt Trap)
     * ---------------------------------------------------------------------- */
    public static boolean useTrapAbility(ServerPlayer player) {
        if (!EssentialsConfig.RASTREADOR_ENABLED.get()) {
            player.sendSystemMessage(Component.translatable("profession.rastreador.disabled"));
            return false;
        }

        // Check actual profession from manager, not just NBT tag
        String actualProfession = ProfissaoManager.getProfession(player.getUUID());
        if (actualProfession == null) {
            player.sendSystemMessage(Component.literal("§cVocê não selecionou uma profissão! /profissoes"));
            return false;
        }
        
        if (!"rastreador".equalsIgnoreCase(actualProfession)) {
            player.sendSystemMessage(Component.translatable("profession.rastreador.not_tracker"));
            return false;
        }

        int cd = player.getPersistentData().getInt(TAG_TRAP_COOLDOWN);
        if (cd > 0) {
            int seconds = cd / 20;
            player.sendSystemMessage(Component.literal("§eRede de Caça disponível em " + seconds + "s."));
            return false;
        }

        // Spawn trap entity at player's feet
        org.lupz.doomsdayessentials.entity.TrapEntity trap = new org.lupz.doomsdayessentials.entity.TrapEntity(
            org.lupz.doomsdayessentials.entity.ModEntities.TRAP.get(),
            player.level()
        );
        trap.setPos(player.getX(), player.getY(), player.getZ());
        trap.setOwnerUUID(player.getUUID());
        trap.setLifetime(600); // 30 seconds = 600 ticks
        player.level().addFreshEntity(trap);
        
        player.getPersistentData().putInt(TAG_TRAP_COOLDOWN, 20 * 20); // 20s cooldown
        player.sendSystemMessage(Component.literal("§aRede de Caça colocada!"));
        return true;
    }

    /* -------------------------------------------------------------------------
     * Active ability – Território Marcado (Marked Territory)
     * ---------------------------------------------------------------------- */
    private static final String TAG_GLOW_COOLDOWN = "rastreadorGlowCooldown";

    public static boolean useGlowAbility(ServerPlayer player) {
        if (!EssentialsConfig.RASTREADOR_ENABLED.get()) {
            player.sendSystemMessage(Component.translatable("profession.rastreador.disabled"));
            return false;
        }

        // Check actual profession from manager, not just NBT tag
        String actualProfession = ProfissaoManager.getProfession(player.getUUID());
        if (actualProfession == null) {
            player.sendSystemMessage(Component.literal("§cVocê não selecionou uma profissão! /profissoes"));
            return false;
        }
        
        if (!"rastreador".equalsIgnoreCase(actualProfession)) {
            player.sendSystemMessage(Component.translatable("profession.rastreador.not_tracker"));
            return false;
        }

        int cd = player.getPersistentData().getInt(TAG_GLOW_COOLDOWN);
        if (cd > 0) {
            int minutes = cd / 1200;
            int seconds = (cd / 20) % 60;
            player.sendSystemMessage(Component.translatable("profession.rastreador.cooldown", minutes, seconds));
            return false;
        }

        // Create Marked Territory zone - 15 block radius, reveals and slows enemies for 15s
        double radius = 15.0;
        int duration = 15 * 20; // 15s

        // Store zone info
        BlockPos center = player.blockPosition();
        long expiresAt = player.level().getGameTime() + duration;
        ACTIVE_ZONES.put(player.getUUID(), new ZoneInfo(center, expiresAt));

        final boolean[] found = {false};
        player.getCapability(TrackerCapabilityProvider.TRACKER_CAPABILITY).ifPresent(cap -> {
            java.util.List<ServerPlayer> nearbyPlayers = player.level().getEntitiesOfClass(ServerPlayer.class, 
                player.getBoundingBox().inflate(radius),
                p -> p != player && !cap.isWhitelisted(p.getUUID()));

            if (nearbyPlayers.isEmpty()) {
                player.sendSystemMessage(Component.literal("Território Marcado criado! Nenhum inimigo detectado.").withStyle(ChatFormatting.YELLOW));
            } else {
                for (ServerPlayer target : nearbyPlayers) {
                    target.addEffect(new MobEffectInstance(MobEffects.GLOWING, duration, 0));
                    target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, duration, 1)); // Slowness II
                }
                player.sendSystemMessage(Component.literal("§aTerritório Marcado! Revelados " + nearbyPlayers.size() + " inimigos!"));
                found[0] = true;
            }
        });

        // Set cooldown
        player.getPersistentData().putInt(TAG_GLOW_COOLDOWN, EssentialsConfig.RASTREADOR_SCAN_COOLDOWN_MINUTES.get() * 1200);

        return true;
    }

    public static void tickTracker(Player player) {
        if (player.level().isClientSide) return;

        // Safety check: ensure tag matches profession registry
        String actualProfession = ProfissaoManager.getProfession(player.getUUID());
        boolean tagSaysTracker = player.getPersistentData().getBoolean(TAG_IS_TRACKER);
        
        if (tagSaysTracker && !"rastreador".equalsIgnoreCase(actualProfession)) {
            // Force cleanup if mismatch detected
            player.getPersistentData().putBoolean(TAG_IS_TRACKER, false);
            MobEffectInstance effect = player.getEffect(MobEffects.NIGHT_VISION);
            if (effect != null && effect.getDuration() == Integer.MAX_VALUE) {
                player.removeEffect(MobEffects.NIGHT_VISION);
            }
            return;
        }

        if (isTracker(player)) {
            // Permanent Night Vision I
            if (!player.hasEffect(MobEffects.NIGHT_VISION)) {
                player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, Integer.MAX_VALUE, 0, true, false, false));
            }
        } else {
            // Remove effect if not tracker anymore
            MobEffectInstance effect = player.getEffect(MobEffects.NIGHT_VISION);
            if (effect != null && effect.getDuration() == Integer.MAX_VALUE) {
                player.removeEffect(MobEffects.NIGHT_VISION);
            }
        }

        if (!isTracker(player)) return;

        var tag = player.getPersistentData();
        
        // Passive: Highlight rare loot every 2 seconds (40 ticks)
        if (player.level().getGameTime() % 40 == 0) {
            highlightRareLoot(player);
        }
        
        // Handle trap cooldown
        if (tag.contains(TAG_TRAP_COOLDOWN)) {
            int cd = tag.getInt(TAG_TRAP_COOLDOWN);
            if (cd > 0) {
                cd--;
                tag.putInt(TAG_TRAP_COOLDOWN, cd);
            }
        }
        
        // Handle zone cooldown
        if (tag.contains(TAG_GLOW_COOLDOWN)) {
            int cd = tag.getInt(TAG_GLOW_COOLDOWN);
            if (cd > 0) {
                cd--;
                tag.putInt(TAG_GLOW_COOLDOWN, cd);
                if (cd == 0) {
                    player.sendSystemMessage(Component.translatable("profession.rastreador.cooldown_ready"));
                }
            }
        }
        
        // Process active traps
        if (!player.level().isClientSide && player instanceof ServerPlayer sp) {
            processTrapEffects(sp);
        }
        
        // Process active zones
        processZoneEffects(player);
    }
    
    private static void processTrapEffects(ServerPlayer player) {
        long now = player.level().getGameTime();
        
        // Clean up expired traps
        ACTIVE_TRAPS.entrySet().removeIf(entry -> now > entry.getValue());
        
        // Check if any players are standing on traps
        for (Map.Entry<BlockPos, Long> entry : ACTIVE_TRAPS.entrySet()) {
            BlockPos trapPos = entry.getKey();
            AABB trapArea = new AABB(trapPos).inflate(2.0);
            
            List<ServerPlayer> trapped = player.level().getEntitiesOfClass(ServerPlayer.class, trapArea);
            for (ServerPlayer target : trapped) {
                if (target.getUUID().equals(player.getUUID())) continue; // Don't trap yourself
                
                // Apply trapped effect: Slowness IV and weaken for 5s
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 3)); // 5s Slowness IV
                target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 0)); // 5s Weakness
                target.sendSystemMessage(Component.literal("§cVocê caiu em uma Rede de Caça!"));
                
                // Remove trap after triggering
                ACTIVE_TRAPS.remove(trapPos);
                break;
            }
        }
    }
    
    private static void processZoneEffects(Player player) {
        if (!(player instanceof ServerPlayer sp)) return;
        
        ZoneInfo zone = ACTIVE_ZONES.get(player.getUUID());
        if (zone == null) return;
        
        long now = player.level().getGameTime();
        if (now > zone.expiresAt) {
            ACTIVE_ZONES.remove(player.getUUID());
            return;
        }
        
        // Apply effects to enemies in zone every second
        if (now % 20 == 0) {
            double radius = 15.0;
            sp.getCapability(TrackerCapabilityProvider.TRACKER_CAPABILITY).ifPresent(cap -> {
                List<ServerPlayer> enemies = sp.level().getEntitiesOfClass(ServerPlayer.class,
                    new AABB(zone.center).inflate(radius),
                    p -> p != sp && !cap.isWhitelisted(p.getUUID()));
                
                for (ServerPlayer enemy : enemies) {
                    enemy.addEffect(new MobEffectInstance(MobEffects.GLOWING, 25, 0, true, false));
                    enemy.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 25, 1, true, false));
                }
            });
        }
    }
} 