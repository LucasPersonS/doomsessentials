package org.lupz.doomsdayessentials.guild;

import net.minecraft.core.BlockPos;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Core data model representing a guild/organization.
 */
public class Guild {
    private final String name;
    private final String tag;
    private final List<GuildMember> members = new ArrayList<>();

    private BlockPos totemPosition;
    private BlockPos basePosition;

    private final java.util.Set<String> allies = new java.util.HashSet<>();
    /** Upgrade level for organization global storage capacity (1..10). */
    private int storageLevel = 1;

    public Guild(String name, String tag, UUID leaderUUID) {
        this.name = name;
        this.tag = tag;
        this.members.add(new GuildMember(leaderUUID, GuildMember.Rank.LEADER));
    }

    // ---------------------------------------------------------------------
    // Accessors
    // ---------------------------------------------------------------------

    public String getName() {
        return name;
    }

    public String getTag() {
        return tag;
    }

    public List<GuildMember> getMembers() {
        return members;
    }

    public BlockPos getTotemPosition() {
        return totemPosition;
    }

    public void setTotemPosition(BlockPos totemPosition) {
        this.totemPosition = totemPosition;
    }

    public BlockPos getBasePosition() {
        return basePosition;
    }

    public void setBasePosition(BlockPos basePosition) {
        this.basePosition = basePosition;
    }

    public java.util.Set<String> getAllies() {
        return allies;
    }

    public boolean isAlliedWith(String guildName) {
        return allies.contains(guildName);
    }

    public void addAlly(String guildName) {
        allies.add(guildName);
    }

    public void removeAlly(String guildName) {
        allies.remove(guildName);
    }

    // ---------------------------------------------------------------------
    // Member helpers
    // ---------------------------------------------------------------------

    public void addMember(UUID uuid) {
        members.add(new GuildMember(uuid, GuildMember.Rank.MEMBER));
    }

    public GuildMember getMember(UUID uuid) {
        return members.stream().filter(m -> m.getPlayerUUID().equals(uuid)).findFirst().orElse(null);
    }

    public void removeMember(UUID uuid) {
        members.removeIf(m -> m.getPlayerUUID().equals(uuid));
    }

    // ---------------------------------------------------------------------
    // Territory helpers
    // ---------------------------------------------------------------------

    public boolean isInTerritory(BlockPos pos) {
        if (totemPosition == null) {
            return false;
        }
        int totemChunkX = totemPosition.getX() >> 4;
        int totemChunkZ = totemPosition.getZ() >> 4;
        int totemChunkY = totemPosition.getY() >> 4; // vertical section (16-block)

        int posChunkX = pos.getX() >> 4;
        int posChunkZ = pos.getZ() >> 4;
        int posChunkY = pos.getY() >> 4;

        int horizRadius = GuildConfig.TERRITORY_RADIUS_CHUNKS.get();
        int vertRadius = 3; // 3 chunks up/down (~48 blocks)

        return Math.abs(posChunkX - totemChunkX) <= horizRadius &&
               Math.abs(posChunkZ - totemChunkZ) <= horizRadius &&
               Math.abs(posChunkY - totemChunkY) <= vertRadius;
    }

    // ---------------------------------------------------------------------
    // Storage upgrade helpers
    // ---------------------------------------------------------------------

    public int getStorageLevel() {
        return storageLevel <= 0 ? 1 : Math.min(storageLevel, 10);
    }

    public void setStorageLevel(int level) {
        if (level < 1) level = 1; if (level > 10) level = 10;
        this.storageLevel = level;
    }
} 