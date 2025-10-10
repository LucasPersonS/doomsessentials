package org.lupz.doomsdayessentials.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.util.GeckoLibUtil;
import software.bernie.geckolib.core.animation.AnimatableManager.ControllerRegistrar;

public class DummyEntity extends Mob implements GeoEntity {
	private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

	public DummyEntity(EntityType<? extends Mob> type, Level level) {
		super(type, level);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Mob.createMobAttributes()
				.add(Attributes.MAX_HEALTH, 20.0)
				.add(Attributes.MOVEMENT_SPEED, 0.0)
				.add(Attributes.FOLLOW_RANGE, 0.0);
	}

	@Override
	protected void registerGoals() {
		// Minimal: just look at nearby players so it's not totally static visually
		this.goalSelector.addGoal(0, new LookAtPlayerGoal(this, Player.class, 8f));
	}

	@Override
	public void registerControllers(ControllerRegistrar registrar) {
		// No animations for now
	}

	@Override
	public AnimatableInstanceCache getAnimatableInstanceCache() {
		return cache;
	}
} 