package org.lupz.doomsdayessentials.professions;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lupz.doomsdayessentials.EssentialsMod;
import org.lupz.doomsdayessentials.block.ModBlocks;
import org.lupz.doomsdayessentials.config.EssentialsConfig;
import org.lupz.doomsdayessentials.professions.items.ProfessionItems;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

import java.util.*;

/**
 * Engineer profession – can spawn a temporary 3x3 barrier of Molten Steel blocks with the Engineer Hammer.
 */
@Mod.EventBusSubscriber(modid = EssentialsMod.MOD_ID)
public final class EngenheiroProfession {

    private EngenheiroProfession() {}

    private static final String TAG_IS_ENGINEER = "isEngenheiro";
    private static final String TAG_BARRIER_COOLDOWN = "engineerBarrierCooldown";

    // Passive buffs (Resistance & Haste)
    private static final UUID RESISTANCE_UUID = UUID.fromString("7fd5315d-0f7c-4b8b-9f0a-7ab5e3a96fa8");
    private static final UUID HASTE_UUID = UUID.fromString("c1e0c7b4-5e6a-4580-a2b3-9f4a4f15dde1");

    /* ---------------------------------------------------------------------
       Profession join / leave
     --------------------------------------------------------------------- */
    public static void onBecome(Player player) {
        if (player.level().isClientSide) return;
        if (!EssentialsConfig.ENGENHEIRO_ENABLED.get()) {
            player.sendSystemMessage(Component.translatable("profession.engenheiro.disabled"));
            return;
        }
        if (!ProfissaoManager.canBecome("engenheiro")) {
            player.sendSystemMessage(Component.literal("§cO limite de Engenheiros foi atingido."));
            return;
        }

        player.getPersistentData().putBoolean(TAG_IS_ENGINEER, true);
        player.sendSystemMessage(Component.translatable("profession.engenheiro.become"));

        // Give hammer
        player.getInventory().add(new net.minecraft.world.item.ItemStack(ProfessionItems.ENGINEER_HAMMER.get()));

        applyBonuses(player);
    }

    public static void onLeave(Player player) {
        if (player.level().isClientSide) return;
        player.getPersistentData().putBoolean(TAG_IS_ENGINEER, false);
        player.sendSystemMessage(Component.translatable("profession.engenheiro.leave"));

        // Remove hammer
        player.getInventory().items.removeIf(s -> s.getItem() == ProfessionItems.ENGINEER_HAMMER.get());

        // Remove Resistance buff and Haste effect
        if (player.getAttribute(Attributes.ARMOR_TOUGHNESS) != null) {
            player.getAttribute(Attributes.ARMOR_TOUGHNESS).removeModifier(RESISTANCE_UUID);
        }
        player.removeEffect(MobEffects.DIG_SPEED);
    }

    public static void applyBonuses(Player player) {
        // Permanent Resistance via armor toughness (+2) and Haste I
        if (player.getAttribute(Attributes.ARMOR_TOUGHNESS) != null &&
                player.getAttribute(Attributes.ARMOR_TOUGHNESS).getModifier(RESISTANCE_UUID) == null) {
            player.getAttribute(Attributes.ARMOR_TOUGHNESS).addPermanentModifier(
                    new AttributeModifier(RESISTANCE_UUID, "Engineer resistance", 2.0, AttributeModifier.Operation.ADDITION));
        }
        if (!player.hasEffect(MobEffects.DIG_SPEED)) {
            player.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, Integer.MAX_VALUE, 0, true, false, false));
        }
    }

    public static boolean isEngineer(Player player) {
        return player.getPersistentData().getBoolean(TAG_IS_ENGINEER);
    }

    /* ---------------------------------------------------------------------
       Barrier ability (called by hammer item)
     --------------------------------------------------------------------- */
    private static final class Barrier {
        final List<List<BlockPos>> layers; // 0=y,1=y+1,2=y+2
        final ServerLevel level;
        final long removeAt;
        boolean removing = false;
        int removalIndex = 2; // start from top layer
        long nextRemoveTick;
        int placedLayers = 1; // first layer placed instantly
        long nextLayerTick;   // when to place the next layer

        Barrier(List<List<BlockPos>> layers, ServerLevel lvl, long now) {
            this.layers = layers;
            this.level = lvl;
            this.removeAt = now + 400; // 20s lifespan before removal start
            this.nextLayerTick = now + 10; // 0.5s per layer
            this.nextRemoveTick = this.removeAt; // schedule start of removal
        }
    }

    private static final List<Barrier> ACTIVE_BARRIERS = Collections.synchronizedList(new ArrayList<>());

    public static void trySpawnBarrier(ServerPlayer player) {
        if (!isEngineer(player)) {
            player.sendSystemMessage(Component.translatable("profession.engenheiro.not_engineer"));
            return;
        }

        int cd = player.getPersistentData().getInt(TAG_BARRIER_COOLDOWN);
        if (cd > 0) {
            int seconds = cd / 20;
            player.sendSystemMessage(Component.literal("§eA habilidade estará disponível em " + seconds + "s."));
            return;
        }

        // Calculate plane in front
        net.minecraft.core.Direction facing = player.getDirection();
        BlockPos base = player.blockPosition().relative(facing, 1);
        ServerLevel level = player.serverLevel();
        List<List<BlockPos>> layers = new ArrayList<>();

        for (int y = 0; y < 3; y++) {
            List<BlockPos> layerList = new ArrayList<>(9);
            for (int offset = -1; offset <= 1; offset++) {
                BlockPos pos = (facing == net.minecraft.core.Direction.NORTH || facing == net.minecraft.core.Direction.SOUTH)
                        ? base.offset(offset, y, 0)
                        : base.offset(0, y, offset);
                layerList.add(pos);
            }
            layers.add(layerList);
        }

        // Place first layer (y=0) if space available
        List<BlockPos> firstLayer = layers.get(0);
        List<BlockPos> actuallyPlaced = new ArrayList<>();
        for (BlockPos pos : firstLayer) {
            if (level.isEmptyBlock(pos)) {
                level.setBlock(pos, ModBlocks.MOLTEN_STEEL_BLOCK.get().defaultBlockState(), 3);
                actuallyPlaced.add(pos);
            }
        }

        if (actuallyPlaced.isEmpty()) {
            player.sendSystemMessage(Component.literal("§cNão há espaço para criar a barreira."));
            return;
        }

        // Play sound for first layer
        level.playSound(null, player.blockPosition(), SoundEvents.BEEHIVE_EXIT, SoundSource.MASTER, 1f, 1f);

        long now = level.getGameTime();
        ACTIVE_BARRIERS.add(new Barrier(layers, level, now));

        player.sendSystemMessage(Component.literal("§aBarreira criada!"));
        player.getPersistentData().putInt(TAG_BARRIER_COOLDOWN, EssentialsConfig.ENGENHEIRO_BARRIER_COOLDOWN_SECONDS.get() * 20);
    }

    /* ---------------------------------------------------------------------
       Tick handling – barrier expiry & cooldowns
     --------------------------------------------------------------------- */
    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        // Handle barriers
        for (Barrier barrier : new ArrayList<>(ACTIVE_BARRIERS)) {
            long worldTime = barrier.level.getGameTime();
            if (barrier.placedLayers < 3 && worldTime >= barrier.nextLayerTick) {
                List<BlockPos> layer = barrier.layers.get(barrier.placedLayers);
                for (BlockPos pos : layer) {
                    if (barrier.level.isEmptyBlock(pos)) {
                        barrier.level.setBlock(pos, ModBlocks.MOLTEN_STEEL_BLOCK.get().defaultBlockState(), 3);
                    }
                }
                // play sound once per layer
                if (!layer.isEmpty()) {
                    barrier.level.playSound(null, layer.get(0), SoundEvents.BEEHIVE_EXIT, SoundSource.MASTER, 1f, 1f);
                }
                barrier.placedLayers++;
                barrier.nextLayerTick = worldTime + 10;
            }
        }

        // handle removal layers
        for (Barrier barrier : new ArrayList<>(ACTIVE_BARRIERS)) {
            long t = barrier.level.getGameTime();
            if (!barrier.removing && t >= barrier.removeAt) {
                barrier.removing = true;
            }
            if (barrier.removing && t >= barrier.nextRemoveTick && barrier.removalIndex >= 0) {
                List<BlockPos> layer = barrier.layers.get(barrier.removalIndex);
                for (BlockPos pos : layer) {
                    if (barrier.level.getBlockState(pos).getBlock() == ModBlocks.MOLTEN_STEEL_BLOCK.get()) {
                        barrier.level.removeBlock(pos, false);
                    }
                }
                barrier.level.playSound(null, layer.get(0), SoundEvents.BEEHIVE_EXIT, SoundSource.MASTER, 1f, 0.8f);
                barrier.removalIndex--;
                barrier.nextRemoveTick = t + 10; // 0.5s per removal
            }
        }

        ACTIVE_BARRIERS.removeIf(b -> b.removing && b.removalIndex < 0);

        // Handle cooldowns for all engineers online
        if (event.getServer() == null) return;
        for (ServerPlayer p : event.getServer().getPlayerList().getPlayers()) {
            if (!isEngineer(p)) continue;
            var tag = p.getPersistentData();
            if (tag.contains(TAG_BARRIER_COOLDOWN)) {
                int cd = tag.getInt(TAG_BARRIER_COOLDOWN);
                if (cd > 0) {
                    cd--;
                    tag.putInt(TAG_BARRIER_COOLDOWN, cd);
                    if (cd == 0) {
                        p.sendSystemMessage(Component.translatable("profession.engenheiro.cooldown_ready"));
                    }
                }
            }
        }
    }
} 