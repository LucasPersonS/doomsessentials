package org.lupz.doomsdayessentials.professions;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lupz.doomsdayessentials.EssentialsMod;

import java.util.*;

@Mod.EventBusSubscriber(modid = EssentialsMod.MOD_ID)
public final class CacadorProfession {

    private CacadorProfession() {}

    private static final String TAG_IS_HUNTER = "isCacador";

    private static final Map<UUID, MarkInfo> ACTIVE_MARKS = new HashMap<>();

    private record MarkInfo(UUID targetId, long expiresAt) {}

    public static void onBecome(Player player) {
        if (player.level().isClientSide) return;
        player.getPersistentData().putBoolean(TAG_IS_HUNTER, true);
        player.sendSystemMessage(Component.translatable("profession.cacador.become"));
    }

    public static void onLeave(Player player) {
        if (player.level().isClientSide) return;
        player.getPersistentData().remove(TAG_IS_HUNTER);
        ACTIVE_MARKS.remove(player.getUUID());
        player.sendSystemMessage(Component.translatable("profession.cacador.leave"));
    }

    public static boolean isHunter(Player player) {
        return player != null && player.getPersistentData().getBoolean(TAG_IS_HUNTER);
    }

    public static void useMarkSkill(ServerPlayer player) {
        if (!isHunter(player)) return;
        LivingEntity target = findLookTarget(player, 30.0);
        if (target == null) {
            player.sendSystemMessage(Component.translatable("profession.cacador.mark.no_target"));
            return;
        }
        long until = player.level().getGameTime() + 20L * 20L; // 20s
        ACTIVE_MARKS.put(player.getUUID(), new MarkInfo(target.getUUID(), until));
        player.sendSystemMessage(Component.translatable("profession.cacador.mark.applied", target.getName()));
    }

    private static LivingEntity findLookTarget(ServerPlayer player, double range) {
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        Vec3 end = eye.add(look.scale(range));
        AABB bb = player.getBoundingBox().expandTowards(look.scale(range)).inflate(1.0);
        LivingEntity best = null;
        double bestDist = Double.MAX_VALUE;
        for (LivingEntity e : player.level().getEntitiesOfClass(LivingEntity.class, bb, le -> le != player && le.isAlive())) {
            Vec3 to = e.getEyePosition().subtract(eye);
            double dot = to.normalize().dot(look);
            if (dot < 0.96) continue; // roughly within ~15 degrees cone
            double dist = to.lengthSqr();
            if (dist < bestDist && hasLineOfSight(player, e)) {
                bestDist = dist;
                best = e;
            }
        }
        return best;
    }

    private static boolean hasLineOfSight(ServerPlayer player, LivingEntity target) {
        HitResult res = player.level().clip(new ClipContext(player.getEyePosition(), target.getEyePosition(), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        return res.getType() == HitResult.Type.MISS;
    }

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        if (!(event.getSource().getEntity() instanceof Player p)) return;
        if (!isHunter(p)) return;
        MarkInfo info = ACTIVE_MARKS.get(p.getUUID());
        if (info == null) return;
        LivingEntity victim = (LivingEntity) event.getEntity();
        if (!victim.getUUID().equals(info.targetId)) return;
        long now = victim.level().getGameTime();
        if (now > info.expiresAt) {
            ACTIVE_MARKS.remove(p.getUUID());
            return;
        }
        event.setAmount(event.getAmount() * 1.3f);
    }

    @SubscribeEvent
    public static void onLivingDrops(LivingDropsEvent event) {
        if (!(event.getSource().getEntity() instanceof Player p)) return;
        if (!isHunter(p)) return;
        if (!(event.getEntity() instanceof Zombie)) return;
        for (var itemEntity : event.getDrops()) {
            var stack = itemEntity.getItem();
            int extra = Math.max(0, (int)Math.floor(stack.getCount() * 0.2));
            if (extra > 0) stack.grow(extra);
        }
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        // Cleanup expired marks
        long now = java.util.Optional.ofNullable(net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer())
                .map(s -> s.overworld().getGameTime()).orElse(0L);
        if (now == 0L) return;
        ACTIVE_MARKS.entrySet().removeIf(e -> now > e.getValue().expiresAt);
    }
} 