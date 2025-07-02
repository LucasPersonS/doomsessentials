package org.lupz.doomsdayessentials.professions;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.lupz.doomsdayessentials.config.ProfessionConfig;

import java.util.UUID;

/**
 * Combatente – more durable & faster.
 */
public final class CombatenteProfession {
    private static final String TAG_IS_COMBATENTE = "isCombatente";
    private static final UUID HEALTH_MODIFIER_UUID = UUID.fromString("5e2b47c1-1d6a-4bbd-8c1d-7f7db0d9ce10");
    private static final UUID SPEED_MODIFIER_UUID = UUID.fromString("0f3c8e2e-2a2a-4d3e-9b9b-1a1a1b1b1c1c");

    private CombatenteProfession() {}

    public static void onBecome(Player player) {
        if (player.level().isClientSide) return;
        if (!ProfessionConfig.COMBATENTE_ENABLED.get()) {
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
} 