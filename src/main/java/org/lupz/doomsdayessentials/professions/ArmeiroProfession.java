package org.lupz.doomsdayessentials.professions;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lupz.doomsdayessentials.EssentialsMod;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Armeiro - Weapon specialist
 * 
 * Passive: Engatilhado - 30% faster reload
 * Skill: Tiro de Supressão - For 10s, hitting enemies stuns them for 0.2s
 * Utility: Bancada Improvisada - Allows crafting of weapons, attachments, and ammo
 */
@Mod.EventBusSubscriber(modid = EssentialsMod.MOD_ID)
public final class ArmeiroProfession {

    private ArmeiroProfession() {}

    private static final String TAG_IS_ARMEIRO = "isArmeiro";
    private static final String TAG_SUPPRESSION_COOLDOWN = "armeiroSuppressionCooldown";
    private static final String TAG_SUPPRESSION_ACTIVE = "armeiroSuppressionActive";
    private static final String TAG_SUPPRESSION_END_TICK = "armeiroSuppressionEnd";

    // Track suppression fire state
    private static final Map<UUID, Long> ACTIVE_SUPPRESSION = new HashMap<>();

    public static void onBecome(Player player) {
        if (player.level().isClientSide) return;
        player.getPersistentData().putBoolean(TAG_IS_ARMEIRO, true);
        player.sendSystemMessage(Component.literal("§aVocê se tornou um Armeiro!"));
        player.sendSystemMessage(Component.literal("§7Passiva: Recarrega 30% mais rápido"));
        player.sendSystemMessage(Component.literal("§7Skill: Tiro de Supressão - Acertar inimigos causa stun de 0.2s por 10s"));
    }

    public static void onLeave(Player player) {
        if (player.level().isClientSide) return;
        
        // Explicitly set to false to ensure cleanup
        player.getPersistentData().putBoolean(TAG_IS_ARMEIRO, false);
        player.getPersistentData().remove(TAG_SUPPRESSION_COOLDOWN);
        player.getPersistentData().remove(TAG_SUPPRESSION_ACTIVE);
        player.getPersistentData().remove(TAG_SUPPRESSION_END_TICK);
        ACTIVE_SUPPRESSION.remove(player.getUUID());
        
        player.sendSystemMessage(Component.literal("§cVocê deixou de ser um Armeiro."));
    }

    public static boolean isArmeiro(Player player) {
        return player != null && player.getPersistentData().getBoolean(TAG_IS_ARMEIRO);
    }

    /**
     * Passive: Engatilhado - 30% faster reload
     * Implemented in ArmeiroTaczIntegration via GunReloadEvent
     */
    public static float getReloadSpeedMultiplier(Player player) {
        if (isArmeiro(player)) {
            return 1.3f; // 30% faster
        }
        return 1.0f;
    }

    /**
     * Skill: Tiro de Supressão
     * For 10s, hitting enemies causes a 0.2s stun
     */
    public static boolean useSuppressionFire(ServerPlayer player) {
        if (!isArmeiro(player)) {
            player.sendSystemMessage(Component.literal("§cVocê não é um Armeiro!"));
            return false;
        }

        int cd = player.getPersistentData().getInt(TAG_SUPPRESSION_COOLDOWN);
        if (cd > 0) {
            int seconds = cd / 20;
            player.sendSystemMessage(Component.literal("§eTiro de Supressão disponível em " + seconds + "s."));
            return false;
        }

        // Activate suppression fire for 10 seconds
        long expiresAt = player.level().getGameTime() + 200; // 10s
        player.getPersistentData().putBoolean(TAG_SUPPRESSION_ACTIVE, true);
        player.getPersistentData().putLong(TAG_SUPPRESSION_END_TICK, expiresAt);
        ACTIVE_SUPPRESSION.put(player.getUUID(), expiresAt);

        player.getPersistentData().putInt(TAG_SUPPRESSION_COOLDOWN, 20 * 30); // 30s cooldown
        player.sendSystemMessage(Component.literal("§aTiro de Supressão ativado!"));
        
        return true;
    }

    /**
     * Apply stun effect when hitting enemies during suppression fire
     */
    @SubscribeEvent
    public static void onDamage(LivingHurtEvent event) {
        if (!(event.getSource().getEntity() instanceof Player)) return;
        Player attacker = (Player) event.getSource().getEntity();
        if (!isArmeiro(attacker)) return;
        if (!(event.getEntity() instanceof LivingEntity)) return;
        LivingEntity victim = (LivingEntity) event.getEntity();

        Long suppressionEnd = ACTIVE_SUPPRESSION.get(attacker.getUUID());
        if (suppressionEnd == null) return;

        long now = attacker.level().getGameTime();
        if (now > suppressionEnd) {
            ACTIVE_SUPPRESSION.remove(attacker.getUUID());
            return;
        }

        // Apply 0.2s stun (4 ticks) as slowness V + weakness
        victim.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 4, 4)); // Slowness V for 0.2s
        victim.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 4, 4)); // Mining fatigue V for 0.2s
        victim.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 4, 1)); // Weakness II for 0.2s
    }

    /**
     * Tick handler for cooldowns and suppression fire
     */
    public static void tickArmeiro(Player player) {
        if (player.level().isClientSide) return;
        if (!isArmeiro(player)) return;

        var tag = player.getPersistentData();

        // Handle suppression cooldown
        if (tag.contains(TAG_SUPPRESSION_COOLDOWN)) {
            int cd = tag.getInt(TAG_SUPPRESSION_COOLDOWN);
            if (cd > 0) {
                cd--;
                tag.putInt(TAG_SUPPRESSION_COOLDOWN, cd);
            }
        }

        // Handle suppression fire timer
        if (tag.getBoolean(TAG_SUPPRESSION_ACTIVE)) {
            long endTick = tag.getLong(TAG_SUPPRESSION_END_TICK);
            long now = player.level().getGameTime();

            if (now >= endTick) {
                tag.putBoolean(TAG_SUPPRESSION_ACTIVE, false);
                ACTIVE_SUPPRESSION.remove(player.getUUID());
                player.sendSystemMessage(Component.literal("§cTiro de Supressão desativado."));
            }
        }
    }
}
