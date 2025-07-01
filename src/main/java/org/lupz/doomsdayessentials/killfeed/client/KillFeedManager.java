package org.lupz.doomsdayessentials.killfeed.client;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;

public class KillFeedManager {
    public static final long ENTRY_DURATION_MS = 10000L;
    public static final int MAX_ENTRIES = 10;
    private static final ConcurrentLinkedQueue<KillFeedEntry> ENTRIES = new ConcurrentLinkedQueue<>();

    public static void addEntry(UUID killerUUID, String killer, UUID victimUUID, String victim, String weaponName, String weaponId, String entityTypeId) {
        ItemStack weapon = new ItemStack(ForgeRegistries.ITEMS.getValue(ResourceLocation.parse(weaponId)));
        if (weapon.getItem() == Items.AIR && !weaponId.equals("minecraft:air")) {
             // This could be a custom weapon not in the registry, handle as needed
        }

        long creationTime = System.currentTimeMillis();
        KillFeedEntry entry = new KillFeedEntry(killerUUID, killer, victimUUID, victim, weaponName, weaponId, entityTypeId, creationTime, weapon);
        ENTRIES.add(entry);

        if (ENTRIES.size() > MAX_ENTRIES) {
            ENTRIES.poll();
        }
    }

    public static List<KillFeedEntry> getEntries() {
        long now = System.currentTimeMillis();
        ENTRIES.removeIf(entry -> now - entry.creationTime() > ENTRY_DURATION_MS);
        return List.copyOf(ENTRIES);
    }

    public record KillFeedEntry(
            UUID killerUUID,
            String killer,
            UUID victimUUID,
            String victim,
            String weaponName,
            String weaponId,
            String entityTypeId,
            long creationTime,
            ItemStack weapon
    ) {}
} 