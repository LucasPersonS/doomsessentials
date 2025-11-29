package org.lupz.doomsdayessentials.lootbox.farming;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.monster.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lupz.doomsdayessentials.EssentialsMod;
import org.lupz.doomsdayessentials.lootbox.LootboxManager;
import org.lupz.doomsdayessentials.professions.ProfissaoManager;

import java.util.Random;

/**
 * Event handler para farming de fragmentos de lootbox.
 * Monitora kills de mobs e mineração de blocos.
 */
@Mod.EventBusSubscriber(modid = EssentialsMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class LootboxFarmingEvents {
    private static final Random RANDOM = new Random();

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer killer))
            return;

        LivingEntity victim = event.getEntity();

        // PvP
        if (victim instanceof ServerPlayer) {
            if (RANDOM.nextDouble() < 0.25) {
                LootboxFarmingManager.addFragments(killer, LootboxManager.R_EPICA, 1);
            }
            return;
        }

        // Boss mobs - 100% chance
        if (victim instanceof WitherBoss || victim instanceof EnderDragon) {
            LootboxFarmingManager.addFragments(killer, LootboxManager.R_EPICA, 1);
            return;
        }

        // Mobs raros - 35% chance
        if (victim instanceof WitherSkeleton || victim instanceof Ghast) {
            double chance = 0.35;
            if (isHunter(killer))
                chance *= 1.5;
            if (RANDOM.nextDouble() < chance) {
                LootboxFarmingManager.addFragments(killer, LootboxManager.R_RARA, 1);
            }
            return;
        }

        // Mobs médios - 25% chance
        if (victim instanceof EnderMan || victim instanceof Blaze) {
            double chance = 0.25;
            if (isHunter(killer))
                chance *= 1.5;
            if (RANDOM.nextDouble() < chance) {
                LootboxFarmingManager.addFragments(killer, LootboxManager.R_RARA, 1);
            }
            return;
        }

        // Mobs uncommon - 15% chance
        if (victim instanceof Creeper || victim instanceof Spider) {
            double chance = 0.15;
            if (isHunter(killer))
                chance *= 1.5;
            if (RANDOM.nextDouble() < chance) {
                LootboxFarmingManager.addFragments(killer, LootboxManager.R_INCOMUM, 1);
            }
            return;
        }

        // Mobs comuns - 10% chance
        if (victim instanceof Zombie || victim instanceof Skeleton) {
            double chance = 0.10;
            if (isHunter(killer))
                chance *= 1.5;
            if (RANDOM.nextDouble() < chance) {
                LootboxFarmingManager.addFragments(killer, LootboxManager.R_INCOMUM, 1);
            }
        }
    }

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        Player player = event.getPlayer();
        if (!(player instanceof ServerPlayer sp))
            return;

        Block block = event.getState().getBlock();

        // Netherite block - 30% chance
        if (block == Blocks.NETHERITE_BLOCK) {
            double chance = 0.30;
            if (isEngineer(sp))
                chance *= 1.5;
            if (RANDOM.nextDouble() < chance) {
                LootboxFarmingManager.addFragments(sp, LootboxManager.R_EPICA, 1);
            }
            return;
        }

        // Ancient Debris - 20% chance
        if (block == Blocks.ANCIENT_DEBRIS) {
            double chance = 0.20;
            if (isEngineer(sp))
                chance *= 1.5;
            if (RANDOM.nextDouble() < chance) {
                LootboxFarmingManager.addFragments(sp, LootboxManager.R_EPICA, 1);
            }
            return;
        }

        // Diamond Ore - 10% chance
        if (block == Blocks.DIAMOND_ORE || block == Blocks.DEEPSLATE_DIAMOND_ORE) {
            double chance = 0.10;
            if (isEngineer(sp))
                chance *= 1.5;
            if (RANDOM.nextDouble() < chance) {
                LootboxFarmingManager.addFragments(sp, LootboxManager.R_RARA, 1);
            }
            return;
        }

        // Gold/Redstone - 5% chance
        if (block == Blocks.GOLD_ORE || block == Blocks.DEEPSLATE_GOLD_ORE ||
                block == Blocks.REDSTONE_ORE || block == Blocks.DEEPSLATE_REDSTONE_ORE) {
            double chance = 0.05;
            if (isEngineer(sp))
                chance *= 1.5;
            if (RANDOM.nextDouble() < chance) {
                LootboxFarmingManager.addFragments(sp, LootboxManager.R_RARA, 1);
            }
            return;
        }

        // Coal/Iron - 15% chance
        if (block == Blocks.COAL_ORE || block == Blocks.DEEPSLATE_COAL_ORE ||
                block == Blocks.IRON_ORE || block == Blocks.DEEPSLATE_IRON_ORE) {
            double chance = 0.15;
            if (isEngineer(sp))
                chance *= 1.5;
            if (RANDOM.nextDouble() < chance) {
                LootboxFarmingManager.addFragments(sp, LootboxManager.R_INCOMUM, 1);
            }
            return;
        }

        // Pedra/Dirt/Cobblestone - 2% chance
        if (block == Blocks.STONE || block == Blocks.COBBLESTONE || block == Blocks.DIRT ||
                block == Blocks.DEEPSLATE || block == Blocks.COBBLED_DEEPSLATE) {
            double chance = 0.02;
            if (isEngineer(sp))
                chance *= 1.5;
            if (RANDOM.nextDouble() < chance) {
                LootboxFarmingManager.addFragments(sp, LootboxManager.R_INCOMUM, 1);
            }
        }
    }

    private static boolean isHunter(ServerPlayer player) {
        String profession = ProfissaoManager.getProfession(player.getUUID());
        return "cacador".equalsIgnoreCase(profession);
    }

    private static boolean isEngineer(ServerPlayer player) {
        String profession = ProfissaoManager.getProfession(player.getUUID());
        return "engenheiro".equalsIgnoreCase(profession);
    }
}
