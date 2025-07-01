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
        if (!(event.getEntity() instanceof Player victim)) {
            return;
        }

        if (victim.level().isClientSide) {
            return;
        }

        DamageSource source = event.getSource();
        Entity killer = source.getEntity();

        boolean isMobKill = killer != null && !(killer instanceof Player) && killer != victim;
        
        String victimName = victim.getDisplayName().getString();
        UUID victimUUID = victim.getUUID();
        String entityTypeId = ForgeRegistries.ENTITY_TYPES.getKey((isMobKill ? killer : victim).getType()).toString();

        UUID killerUUID = null;
        String killerName = null;
        String killerCredit = null;

        if (killer instanceof Player) {
            killerUUID = killer.getUUID();
            killerName = killer.getDisplayName().getString();
        } else if (killer instanceof LivingEntity) {
            killerCredit = killer.getDisplayName().getString();
        }

        ItemStack weapon = ItemStack.EMPTY;
        String weaponName;

        if (isMobKill) {
            weaponName = killer.getDisplayName().getString();
        } else {
            if (killer instanceof LivingEntity living) {
                weapon = living.getMainHandItem();
            }
            weaponName = weapon.isEmpty() ? "Fists" : weapon.getDisplayName().getString();
        }

        String weaponId = "minecraft:air";

        if (!weapon.isEmpty()) {
            ResourceLocation key = ForgeRegistries.ITEMS.getKey(weapon.getItem());
            if (key != null) {
                weaponId = key.toString();
                if (weaponId.startsWith("tacz:")) {
                    CompoundTag tag = weapon.getTag();
                    if (tag != null && tag.contains("GunId", 8)) {
                        weaponName = tag.getString("GunId");
                    }
                }
            }
        }

        String damageType = source.getMsgId();
        if (killer == null || killer == victim) {
            killerName = null;
            killerUUID = null;
            switch (damageType) {
                case "fall" -> weaponName = "FALL";
                case "inFire", "onFire", "lava" -> weaponName = "FIRE";
                case "explosion", "explosion.player" -> weaponName = "EXPLOSION";
                case "outOfWorld" -> weaponName = "VOID";
                case "genericKill" -> weaponName = "/kill";
                default -> weaponName = "SUICIDE";
            }
            if (source.getDirectEntity() instanceof Player) {
                killer = source.getDirectEntity();
                killerUUID = killer.getUUID();
                killerName = killer.getDisplayName().getString();
            }
        }

        KillFeedPacket packet = new KillFeedPacket(killerUUID, killerName != null ? killerName : killerCredit, victimUUID, victimName, weaponName, weaponId, entityTypeId);
        PacketHandler.CHANNEL.send(PacketDistributor.ALL.noArg(), packet);
    }
} 