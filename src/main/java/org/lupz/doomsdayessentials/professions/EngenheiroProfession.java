package org.lupz.doomsdayessentials.professions;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.event.AnvilUpdateEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lupz.doomsdayessentials.EssentialsMod;
import org.lupz.doomsdayessentials.entity.ModEntities;
import org.lupz.doomsdayessentials.entity.SentryEntity;
import org.lupz.doomsdayessentials.professions.shop.EngineerConfig;
import org.slf4j.Logger;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import java.util.*;

@Mod.EventBusSubscriber(modid = EssentialsMod.MOD_ID)
public final class EngenheiroProfession {

    private EngenheiroProfession() {}

    private static final String TAG_IS_ENGINEER = "isEngenheiro";
    private static final String TAG_BARRIER_COOLDOWN = "engineerBarrierCooldown";
    private static final String TAG_TURRET_COOLDOWN = "engineerTurretCooldown";
    private static final String TAG_WALL_COOLDOWN = "engineerWallCooldown";

    private static final UUID RESISTANCE_UUID = UUID.fromString("7fd5315d-0f7c-4b8b-9f0a-7ab5e3a96fa8");
    private static final UUID HASTE_UUID = UUID.fromString("c1e0c7b4-5e6a-4580-a2b3-9f4a4f15dde1");

    public static void onBecome(Player player) {
        if (player.level().isClientSide) return;
        if (!EngineerConfig.ENGENHEIRO_ENABLED.get()) {
            player.sendSystemMessage(Component.translatable("profession.engenheiro.disabled"));
            return;
        }
        if (!ProfissaoManager.canBecome("engenheiro")) {
            player.sendSystemMessage(Component.literal("§cO limite de Engenheiros foi atingido."));
            return;
        }

        player.getPersistentData().putBoolean(TAG_IS_ENGINEER, true);
        player.sendSystemMessage(Component.translatable("profession.engenheiro.become"));

        applyBonuses(player);
    }

    public static void onLeave(Player player) {
        if (player.level().isClientSide) return;
        player.getPersistentData().remove(TAG_IS_ENGINEER);
        player.getPersistentData().remove(TAG_BARRIER_COOLDOWN);
        player.getPersistentData().remove(TAG_TURRET_COOLDOWN);
        player.getPersistentData().remove(TAG_WALL_COOLDOWN);
        player.sendSystemMessage(Component.translatable("profession.engenheiro.leave"));
        removeBonuses(player);
    }

    public static boolean isEngineer(Player player) {
        return player.getPersistentData().getBoolean(TAG_IS_ENGINEER);
    }

    public static boolean useTurretSkill(ServerPlayer player) {
        return useTurretSkillAt(player, player.blockPosition().relative(player.getDirection()));
    }

    public static boolean useTurretSkillAt(ServerPlayer player, BlockPos pos) {
        if (!isEngineer(player)) return false;
        int cd = player.getPersistentData().getInt(TAG_TURRET_COOLDOWN);
        if (cd > 0) {
            int seconds = cd / 20;
            player.sendSystemMessage(Component.literal("§eSentinela disponível em " + seconds + "s."));
            return false;
        }
        Level level = player.level();
        SentryEntity sentry = new SentryEntity(ModEntities.SENTRY.get(), level);
        sentry.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        sentry.setOwner(player);
        level.addFreshEntity(sentry);
        player.getPersistentData().putInt(TAG_TURRET_COOLDOWN, 20 * 60); // 60s
        player.sendSystemMessage(Component.literal("§aTorreta implantada por 30s!"));
        return true;
    }
    
    private static final List<Barrier> ACTIVE_BARRIERS = new ArrayList<>();

    private record Barrier(List<BlockPos> positions, Level level, long startTime) {}
    
    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        // Remove walls after 20 seconds
        ACTIVE_BARRIERS.removeIf(barrier -> {
            long now = barrier.level.getGameTime();
            long elapsed = now - barrier.startTime;
            if (elapsed >= 400) { // 20 seconds
                for (BlockPos pos : barrier.positions) {
                    if (barrier.level.getBlockState(pos).is(Blocks.COBBLESTONE)) {
                        barrier.level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
                    }
                }
                barrier.level.playSound(null, barrier.positions.get(0), SoundEvents.STONE_BREAK, SoundSource.BLOCKS, 1f, 0.8f);
                return true;
            }
            return false;
        });
    }

    public static void applyBonuses(Player player) {
        player.getAttributes().addTransientAttributeModifiers(createAttributeMap());
    }

    private static void removeBonuses(Player player) {
        AttributeInstance toughness = player.getAttribute(Attributes.ARMOR_TOUGHNESS);
        if (toughness != null) {
            toughness.removeModifier(RESISTANCE_UUID);
        }
        AttributeInstance haste = player.getAttribute(Attributes.ATTACK_SPEED);
        if (haste != null) {
            haste.removeModifier(HASTE_UUID);
        }
    }
    
    private static Multimap<Attribute, AttributeModifier> createAttributeMap() {
        Multimap<Attribute, AttributeModifier> map = HashMultimap.create();
        map.put(Attributes.ARMOR_TOUGHNESS, new AttributeModifier(RESISTANCE_UUID, "EngenheiroResistance", 2, AttributeModifier.Operation.ADDITION));
        map.put(Attributes.ATTACK_SPEED, new AttributeModifier(HASTE_UUID, "EngenheiroHaste", 0.1, AttributeModifier.Operation.ADDITION));
        return map;
    }

    /**
     * Passive: Place a wall facing the damage source when taking damage
     */
    @SubscribeEvent
    public static void onDamage(LivingHurtEvent event) {
        if (!(event.getEntity() instanceof net.minecraft.server.level.ServerPlayer player)) return;
        if (!isEngineer(player)) return;
        
        int cd = player.getPersistentData().getInt(TAG_WALL_COOLDOWN);
        if (cd > 0) return; // Wall on cooldown
        
        // Determine direction to damage source
        Direction facing;
        var damageSource = event.getSource();
        var sourceEntity = damageSource.getEntity();
        var sourcePos = damageSource.getSourcePosition();
        
        if (sourceEntity != null) {
            // Face the attacking entity
            double dx = sourceEntity.getX() - player.getX();
            double dz = sourceEntity.getZ() - player.getZ();
            facing = getDirectionFromOffset(dx, dz);
        } else if (sourcePos != null) {
            // Face the damage position
            double dx = sourcePos.x - player.getX();
            double dz = sourcePos.z - player.getZ();
            facing = getDirectionFromOffset(dx, dz);
        } else {
            // Fallback to player's facing direction
            facing = player.getDirection();
        }
        
        BlockPos startPos = player.blockPosition().relative(facing);
        Level level = player.level();
        
        // Build T-shaped wall: 3x3 main wall + 3x1 side wings 1 block behind
        List<BlockPos> wallBlocks = new ArrayList<>();
        Direction left = facing.getCounterClockWise();
        Direction back = facing.getOpposite();
        
        // Main front wall: 3 blocks wide × 3 blocks high (center)
        for (int y = 0; y < 3; y++) {
            wallBlocks.add(startPos.above(y)); // Center column
            wallBlocks.add(startPos.above(y).relative(left)); // Left column
            wallBlocks.add(startPos.above(y).relative(left.getOpposite())); // Right column
        }
        
        // Left wing: 1 block wide × 3 blocks high, positioned 1 block behind
        BlockPos leftWingBase = startPos.relative(back).relative(left, 2);
        for (int y = 0; y < 3; y++) {
            wallBlocks.add(leftWingBase.above(y));
        }
        
        // Right wing: 1 block wide × 3 blocks high, positioned 1 block behind
        BlockPos rightWingBase = startPos.relative(back).relative(left.getOpposite(), 2);
        for (int y = 0; y < 3; y++) {
            wallBlocks.add(rightWingBase.above(y));
        }
        
        // Place cobblestone wall blocks
        for (BlockPos pos : wallBlocks) {
            BlockState currentState = level.getBlockState(pos);
            if (currentState.isAir() || currentState.canBeReplaced()) {
                level.setBlock(pos, Blocks.COBBLESTONE.defaultBlockState(), 3);
            }
        }
        
        player.sendSystemMessage(Component.literal("§aMuro de proteção criado!"));
        player.getPersistentData().putInt(TAG_WALL_COOLDOWN, 20 * 30); // 30s cooldown
        
        // Schedule wall removal after 20 seconds
        ACTIVE_BARRIERS.add(new Barrier(wallBlocks, level, level.getGameTime()));
    }

    /**
     * Repair utility: Reduce repair costs
     * This would be implemented via AnvilUpdateEvent
     */
    @SubscribeEvent
    public static void onAnvilUpdate(AnvilUpdateEvent event) {
        if (event.getPlayer() == null) return;
        if (!isEngineer(event.getPlayer())) return;
        
        // Reduce repair cost by 50%
        if (event.getCost() > 0) {
            event.setCost((int) Math.ceil(event.getCost() * 0.5));
        }
        if (event.getMaterialCost() > 0) {
            event.setMaterialCost((int) Math.ceil(event.getMaterialCost() * 0.5));
        }
    }

    /**
     * Helper method to get direction from X/Z offset
     */
    private static Direction getDirectionFromOffset(double dx, double dz) {
        double angle = Math.toDegrees(Math.atan2(-dx, dz));
        if (angle < 0) angle += 360;
        
        // Convert angle to cardinal direction
        if (angle >= 315 || angle < 45) return Direction.SOUTH;
        if (angle >= 45 && angle < 135) return Direction.WEST;
        if (angle >= 135 && angle < 225) return Direction.NORTH;
        return Direction.EAST;
    }
    
    /**
     * Tick handler for wall cooldown
     */
    public static void tickEngineer(Player player) {
        if (player.level().isClientSide) return;
        if (!isEngineer(player)) return;
        
        var tag = player.getPersistentData();
        
        // Handle turret cooldown
        if (tag.contains(TAG_TURRET_COOLDOWN)) {
            int cd = tag.getInt(TAG_TURRET_COOLDOWN);
            if (cd > 0) {
                cd--;
                tag.putInt(TAG_TURRET_COOLDOWN, cd);
            }
        }
        
        // Handle wall cooldown
        if (tag.contains(TAG_WALL_COOLDOWN)) {
            int cd = tag.getInt(TAG_WALL_COOLDOWN);
            if (cd > 0) {
                cd--;
                tag.putInt(TAG_WALL_COOLDOWN, cd);
            }
        }
    }
}
