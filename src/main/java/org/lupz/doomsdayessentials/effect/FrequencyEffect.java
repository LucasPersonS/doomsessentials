package org.lupz.doomsdayessentials.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/**
 * Grants immunity to radiation from Frequency Fields.
 */
public class FrequencyEffect extends MobEffect {

    public FrequencyEffect() {
        super(MobEffectCategory.BENEFICIAL, 0xFF0000); // red particles
    }

    // We do not need attribute modifiers or ticks – purely marker effect.

    // Icon is provided automatically by placing 'frequencia.png' in textures/mob_effect.
} 