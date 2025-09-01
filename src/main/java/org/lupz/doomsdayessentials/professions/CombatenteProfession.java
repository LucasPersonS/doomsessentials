package org.lupz.doomsdayessentials.professions;

import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lupz.doomsdayessentials.EssentialsMod;
import org.lupz.doomsdayessentials.config.EssentialsConfig;

import java.util.UUID;

/**
 * Combatente – more durable & faster.
 */
@Mod.EventBusSubscriber(modid = EssentialsMod.MOD_ID)
public final class CombatenteProfession {
    private static final String TAG_IS_COMBATENTE = "isCombatente";
    private static final UUID HEALTH_MODIFIER_UUID = UUID.fromString("5e2b47c1-1d6a-4bbd-8c1d-7f7db0d9ce10");
    private static final UUID SPEED_MODIFIER_UUID = UUID.fromString("0f3c8e2e-2a2a-4d3e-9b9b-1a1a1b1b1c1c");

    private static final String TAG_ADRENALINE_COOLDOWN = "combatenteAdrenalineCooldown";
    private static final String TAG_ADRENALINE_END_TICK = "combatenteAdrenalineEnd";

    private CombatenteProfession() {}

    public static void onBecome(Player player) {
        if (player.level().isClientSide) return;
        if (!EssentialsConfig.COMBATENTE_ENABLED.get()) {
            player.sendSystemMessage(Component.translatable("profession.combatente.disabled"));
            return;
        }

        // Safety check: ensure the limit is respected even if called from elsewhere
        if (!org.lupz.doomsdayessentials.professions.ProfissaoManager.canBecomeCombatente()) {
            player.sendSystemMessage(Component.literal("§cO limite de Combatentes foi atingido. Não é possível se tornar um Combatente agora."));
            return;
        }

        player.getPersistentData().putBoolean(TAG_IS_COMBATENTE, true);
        player.sendSystemMessage(Component.translatable("profession.combatente.become"));

        applyBonuses(player);
    }

    public static void onLeave(Player player) {
        if (player.level().isClientSide) return;
        player.getPersistentData().putBoolean(TAG_IS_COMBATENTE, false);
        player.getPersistentData().remove(TAG_ADRENALINE_COOLDOWN);
        player.getPersistentData().remove(TAG_ADRENALINE_END_TICK);
        player.sendSystemMessage(Component.translatable("profession.combatente.leave"));

        if (player.getAttribute(Attributes.MAX_HEALTH) != null) {
            player.getAttribute(Attributes.MAX_HEALTH).removeModifier(HEALTH_MODIFIER_UUID);
            if (player.getHealth() > player.getMaxHealth()) {
                player.setHealth(player.getMaxHealth());
            }
        }

        if (player.getAttribute(Attributes.MOVEMENT_SPEED) != null) {
            player.getAttribute(Attributes.MOVEMENT_SPEED).removeModifier(SPEED_MODIFIER_UUID);
        }

        // Clear adrenaline effects if present
        MobEffectInstance spd = player.getEffect(MobEffects.MOVEMENT_SPEED);
        if (spd != null) player.removeEffect(MobEffects.MOVEMENT_SPEED);
        MobEffectInstance res = player.getEffect(MobEffects.DAMAGE_RESISTANCE);
        if (res != null) player.removeEffect(MobEffects.DAMAGE_RESISTANCE);
    }

    public static void applyBonuses(Player player) {
        if (player.getAttribute(Attributes.MAX_HEALTH) != null &&
                player.getAttribute(Attributes.MAX_HEALTH).getModifier(HEALTH_MODIFIER_UUID) == null) {
            player.getAttribute(Attributes.MAX_HEALTH).addPermanentModifier(
                    new AttributeModifier(HEALTH_MODIFIER_UUID, "Combatente bonus health", 10.0, AttributeModifier.Operation.ADDITION)); // +10 health = 5 hearts extra
            player.setHealth(player.getMaxHealth());
        }

        if (player.getAttribute(Attributes.MOVEMENT_SPEED) != null &&
                player.getAttribute(Attributes.MOVEMENT_SPEED).getModifier(SPEED_MODIFIER_UUID) == null) {
            player.getAttribute(Attributes.MOVEMENT_SPEED).addPermanentModifier(
                    new AttributeModifier(SPEED_MODIFIER_UUID, "Combatente bonus speed", 0.05, AttributeModifier.Operation.ADDITION));
        }
    }

    public static boolean activateAdrenaline(net.minecraft.server.level.ServerPlayer player) {
        if (!"combatente".equalsIgnoreCase(org.lupz.doomsdayessentials.professions.ProfissaoManager.getProfession(player.getUUID()))) {
            player.sendSystemMessage(Component.translatable("profession.combatente.not_combatente"));
            return false;
        }
        int cd = player.getPersistentData().getInt(TAG_ADRENALINE_COOLDOWN);
        if (cd > 0) {
            int seconds = cd / 20;
            player.sendSystemMessage(Component.literal("§eHabilidade disponível em " + seconds + "s."));
            return false;
        }
        // Apply Speed II and Resistance II for 10s
        int duration = 200;
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, duration, 1));
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, duration, 1));
        long endTick = player.level().getGameTime() + duration;
        player.getPersistentData().putLong(TAG_ADRENALINE_END_TICK, endTick);
        // Cooldown 60s
        player.getPersistentData().putInt(TAG_ADRENALINE_COOLDOWN, 1200);
        player.sendSystemMessage(Component.literal("§aAdrenalina de Combate ativada!"));
        return true;
    }

    @SubscribeEvent
    public static void onKill(LivingDeathEvent event) {
        if (!(event.getSource().getEntity() instanceof net.minecraft.server.level.ServerPlayer player)) return;
        if (!"combatente".equalsIgnoreCase(org.lupz.doomsdayessentials.professions.ProfissaoManager.getProfession(player.getUUID()))) return;
        long now = player.level().getGameTime();
        long end = player.getPersistentData().getLong(TAG_ADRENALINE_END_TICK);
        if (end <= now) return; // not active
        // Extend by 2s per kill
        long newEnd = Math.min(end + 40, now + 600); // cap to +30s total remaining
        player.getPersistentData().putLong(TAG_ADRENALINE_END_TICK, newEnd);
        // Refresh effects to match new remaining time (approximate)
        int remaining = (int) Math.max(0, newEnd - now);
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, remaining, 1, true, false));
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, remaining, 1, true, false));
    }
} 