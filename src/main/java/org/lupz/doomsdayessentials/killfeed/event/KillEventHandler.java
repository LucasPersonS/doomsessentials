package org.lupz.doomsdayessentials.killfeed.event;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import org.lupz.doomsdayessentials.EssentialsMod;
import org.lupz.doomsdayessentials.killfeed.network.packet.KillFeedPacket;
import org.lupz.doomsdayessentials.network.PacketHandler;
import net.minecraftforge.network.PacketDistributor;

import java.util.UUID;

@Mod.EventBusSubscriber(modid = EssentialsMod.MOD_ID)
public class KillEventHandler {

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (event.isCanceled()) {
            return;
        }
        
        if (event.getEntity().level().isClientSide()) {
            return;
        }

        if (!(event.getEntity() instanceof Player victim)) {
            return;
        }

        DamageSource source = event.getSource();
        Entity killer = source.getEntity();
        Entity directKiller = source.getDirectEntity();

        String victimName = victim.getDisplayName().getString();
        UUID victimUUID = victim.getUUID();
        String victimTypeId = ForgeRegistries.ENTITY_TYPES.getKey(victim.getType()).toString();

        UUID killerUUID = null;
        String killerName = null;
        String killerTypeId = "minecraft:player";

        ItemStack weapon = ItemStack.EMPTY;
        String weaponName = "SUICIDE";
        String weaponId = "minecraft:air";

        if (killer instanceof Player && killer != victim) {
            killerUUID = killer.getUUID();
            killerName = killer.getDisplayName().getString();
            killerTypeId = ForgeRegistries.ENTITY_TYPES.getKey(killer.getType()).toString();

            if (killer instanceof LivingEntity livingKiller) {
                weapon = livingKiller.getMainHandItem();
                if (!weapon.isEmpty()) {
                    weaponName = weapon.getDisplayName().getString();
                    ResourceLocation key = ForgeRegistries.ITEMS.getKey(weapon.getItem());
                    if (key != null) {
                        weaponId = key.toString();
                    }
                } else {
                    weaponName = "Fists";
                }
            }
        } else if (killer instanceof LivingEntity && killer != victim) {
            killerName = killer.getDisplayName().getString();
            killerTypeId = ForgeRegistries.ENTITY_TYPES.getKey(killer.getType()).toString();
            weaponName = killerName;
        } else {
            // Environmental or self-inflicted deaths
            killerName = null; // No specific killer
            killerTypeId = null; // No killer entity
            switch (source.getMsgId()) {
                case "fall" -> weaponName = "FALL";
                case "inFire", "onFire", "lava" -> weaponName = "FIRE";
                case "explosion", "explosion.player" -> weaponName = "EXPLOSION";
                case "outOfWorld" -> weaponName = "VOID";
                case "genericKill" -> weaponName = "/kill";
                default -> weaponName = "SUICIDE";
            }
        }
        
        KillFeedPacket packet = new KillFeedPacket(killerUUID, killerName, victimUUID, victimName, weaponName, weaponId, killerTypeId != null ? killerTypeId : victimTypeId);
        PacketHandler.CHANNEL.send(PacketDistributor.ALL.noArg(), packet);
    }
} 