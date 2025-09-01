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
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lupz.doomsdayessentials.EssentialsMod;
import org.lupz.doomsdayessentials.entity.ModEntities;
import org.lupz.doomsdayessentials.entity.SentryEntity;
import org.lupz.doomsdayessentials.professions.items.ProfessionItems;
import org.lupz.doomsdayessentials.professions.shop.EngineerConfig;
import org.slf4j.Logger;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.level.ClipContext;

import java.util.*;

@Mod.EventBusSubscriber(modid = EssentialsMod.MOD_ID)
public final class EngenheiroProfession {

    private EngenheiroProfession() {}

    private static final String TAG_IS_ENGINEER = "isEngenheiro";
    private static final String TAG_BARRIER_COOLDOWN = "engineerBarrierCooldown";
    private static final String TAG_TURRET_COOLDOWN = "engineerTurretCooldown";

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

        player.getInventory().add(new net.minecraft.world.item.ItemStack(ProfessionItems.ENGINEER_HAMMER.get()));

        applyBonuses(player);
    }

    public static void handleHammerUse(ServerPlayer player) {
        HitResult result = player.pick(5.0, 0, false);
        BlockPos pos;
        if (result.getType() == HitResult.Type.BLOCK) {
            pos = ((net.minecraft.world.phys.BlockHitResult) result).getBlockPos().relative(((net.minecraft.world.phys.BlockHitResult) result).getDirection());
        } else {
            pos = player.blockPosition().relative(player.getDirection());
        }
        useTurretSkillAt(player, pos);
    }

    public static void onLeave(Player player) {
        if (player.level().isClientSide) return;
        player.getPersistentData().remove(TAG_IS_ENGINEER);
        player.getPersistentData().remove(TAG_BARRIER_COOLDOWN);
        player.getPersistentData().remove(TAG_TURRET_COOLDOWN);
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

        // barrier logic kept for backward compatibility if needed (currently unused)
        ACTIVE_BARRIERS.removeIf(barrier -> {
            long now = barrier.level.getGameTime();
            long elapsed = now - barrier.startTime;
            if (elapsed >= 400) {
                for (BlockPos pos : barrier.positions) {
                    if (barrier.level.getBlockState(pos).is(Blocks.MANGROVE_WOOD)) {
                        barrier.level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
                    }
                }
                barrier.level.playSound(null, barrier.positions.get(0), SoundEvents.BEEHIVE_EXIT, SoundSource.MASTER, 1f, 0.8f);
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
} 