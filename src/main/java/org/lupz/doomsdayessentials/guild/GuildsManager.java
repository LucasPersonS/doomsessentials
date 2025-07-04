package org.lupz.doomsdayessentials.guild;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * Stores all guilds and wars for a single world. Persisted via Minecraft's
 * SavedData system (i.e. saves alongside chunks & player data).
 */
public class GuildsManager extends SavedData {

    private static final String DATA_NAME = "guilds";

    private final Map<String, Guild> guilds = new HashMap<>();
    private final List<War> activeWars = new ArrayList<>();
    private final Map<String, Long> territoryWarCooldowns = new HashMap<>();
    /** Pending invitations: player UUID -> guild name */
    private final java.util.Map<java.util.UUID, String> pendingInvites = new java.util.HashMap<>();

    // ---------------------------------------------------------------------
    // Construction / loading
    // ---------------------------------------------------------------------

    public GuildsManager() {}

    private GuildsManager(CompoundTag tag) {
        read(tag);
    }

    public static GuildsManager load(CompoundTag tag) {
        return new GuildsManager(tag);
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag guildsList = new ListTag();
        for (Guild guild : guilds.values()) {
            CompoundTag gtag = new CompoundTag();
            gtag.putString("name", guild.getName());
            gtag.putString("tag", guild.getTag());

            // Leader UUID stored separately so we know who is leader when loading
            guild.getMembers().stream()
                    .filter(m -> m.getRank() == GuildMember.Rank.LEADER)
                    .findFirst()
                    .ifPresent(leader -> gtag.putUUID("leader", leader.getPlayerUUID()));

            if (guild.getTotemPosition() != null) {
                gtag.putInt("totemX", guild.getTotemPosition().getX());
                gtag.putInt("totemY", guild.getTotemPosition().getY());
                gtag.putInt("totemZ", guild.getTotemPosition().getZ());
            }
            if (guild.getBasePosition() != null) {
                gtag.putInt("baseX", guild.getBasePosition().getX());
                gtag.putInt("baseY", guild.getBasePosition().getY());
                gtag.putInt("baseZ", guild.getBasePosition().getZ());
            }

            ListTag membersList = new ListTag();
            for (GuildMember m : guild.getMembers()) {
                CompoundTag mtag = new CompoundTag();
                mtag.putUUID("uuid", m.getPlayerUUID());
                mtag.putString("rank", m.getRank().name());
                mtag.putLong("joinTimestamp", m.getJoinTimestamp());
                membersList.add(mtag);
            }
            gtag.put("members", membersList);

            // Allies list
            if (!guild.getAllies().isEmpty()) {
                net.minecraft.nbt.ListTag allyList = new net.minecraft.nbt.ListTag();
                for (String ally : guild.getAllies()) {
                    allyList.add(net.minecraft.nbt.StringTag.valueOf(ally));
                }
                gtag.put("allies", allyList);
            }
            guildsList.add(gtag);
        }
        tag.put("guilds", guildsList);

        CompoundTag cooldowns = new CompoundTag();
        territoryWarCooldowns.forEach(cooldowns::putLong);
        tag.put("warCooldowns", cooldowns);
        return tag;
    }

    private void read(CompoundTag tag) {
        if (!tag.contains("guilds")) return;
        ListTag guildsList = tag.getList("guilds", Tag.TAG_COMPOUND);
        for (int i = 0; i < guildsList.size(); i++) {
            CompoundTag gtag = guildsList.getCompound(i);
            String name = gtag.getString("name");
            String gtagTag = gtag.getString("tag");
            UUID leader = gtag.getUUID("leader");
            Guild guild = new Guild(name, gtagTag, leader);

            if (gtag.contains("totemX")) {
                guild.setTotemPosition(new BlockPos(gtag.getInt("totemX"), gtag.getInt("totemY"), gtag.getInt("totemZ")));
            }
            if (gtag.contains("baseX")) {
                guild.setBasePosition(new BlockPos(gtag.getInt("baseX"), gtag.getInt("baseY"), gtag.getInt("baseZ")));
            }

            ListTag membersList = gtag.getList("members", Tag.TAG_COMPOUND);
            for (int j = 0; j < membersList.size(); j++) {
                CompoundTag mtag = membersList.getCompound(j);
                UUID uuid = mtag.getUUID("uuid");
                if (uuid.equals(leader)) {
                    continue; // already added as leader
                }
                guild.addMember(uuid);
                GuildMember member = guild.getMember(uuid);
                if (member != null) {
                    member.setRank(GuildMember.Rank.valueOf(mtag.getString("rank")));
                    // joinTimestamp
                }
            }
            guilds.put(name, guild);

            // Allies – processed after all guilds added to avoid missing refs
            if (gtag.contains("allies")) {
                ListTag allyList = gtag.getList("allies", Tag.TAG_STRING);
                java.util.Set<String> allies = guild.getAllies();
                for (int k = 0; k < allyList.size(); k++) {
                    allies.add(allyList.getString(k));
                }
            }
        }
        if (tag.contains("warCooldowns")) {
            CompoundTag cooldowns = tag.getCompound("warCooldowns");
            for (String key : cooldowns.getAllKeys()) {
                territoryWarCooldowns.put(key, cooldowns.getLong(key));
            }
        }
    }

    // ---------------------------------------------------------------------
    // Static helper
    // ---------------------------------------------------------------------

    public static GuildsManager get(ServerLevel level) {
        DimensionDataStorage storage = level.getDataStorage();
        return storage.computeIfAbsent(GuildsManager::load, GuildsManager::new, DATA_NAME);
    }

    // ---------------------------------------------------------------------
    // Public API – guild helpers
    // ---------------------------------------------------------------------

    public @Nullable Guild getGuild(String name) {
        return guilds.get(name);
    }

    public @Nullable Guild getGuildByMember(UUID uuid) {
        return guilds.values().stream()
                .filter(g -> g.getMember(uuid) != null)
                .findFirst()
                .orElse(null);
    }

    public @Nullable Guild getGuildAt(BlockPos pos) {
        return guilds.values().stream()
                .filter(g -> g.isInTerritory(pos))
                .findFirst()
                .orElse(null);
    }

    public void createGuild(String name, String tag, UUID leaderUUID) {
        guilds.put(name, new Guild(name, tag, leaderUUID));
        setDirty();
    }

    // ---------------------------------------------------------------------
    // Guild modifications
    // ---------------------------------------------------------------------

    public boolean addMember(String guildName, UUID uuid) {
        Guild guild = guilds.get(guildName);
        if (guild == null) return false;
        int max = GuildConfig.MAX_GUILD_MEMBERS.get();
        if (max != 0 && guild.getMembers().size() >= max) return false;
        guild.addMember(uuid);
        setDirty();
        return true;
    }

    public boolean removeMember(String guildName, UUID uuid) {
        Guild guild = guilds.get(guildName);
        if (guild == null) return false;
        guild.removeMember(uuid);
        setDirty();
        return true;
    }

    // ---------------------------------------------------------------------
    // Invitation helpers (not persisted)
    // ---------------------------------------------------------------------

    public void invitePlayer(String guildName, UUID playerUUID) {
        pendingInvites.put(playerUUID, guildName);
    }

    public boolean hasInvite(UUID playerUUID, String guildName) {
        return guildName.equals(pendingInvites.get(playerUUID));
    }

    public boolean acceptInvite(UUID playerUUID) {
        String g = pendingInvites.remove(playerUUID);
        if (g == null) return false;
        return addMember(g, playerUUID);
    }

    public void clearInvitesForGuild(String guildName) {
        pendingInvites.entrySet().removeIf(e -> e.getValue().equals(guildName));
    }

    // ---------------------------------------------------------------------
    // Alliance helpers
    // ---------------------------------------------------------------------

    public boolean areAllied(String g1, String g2) {
        if (g1 == null || g2 == null) return false;
        if (g1.equals(g2)) return true; // same guild treated as allied
        Guild guild1 = guilds.get(g1);
        Guild guild2 = guilds.get(g2);
        if (guild1 == null || guild2 == null) return false;
        return guild1.isAlliedWith(g2) && guild2.isAlliedWith(g1);
    }

    public boolean addAlliance(String g1, String g2) {
        if (g1 == null || g2 == null) return false;
        if (g1.equals(g2)) return false;
        Guild guild1 = guilds.get(g1);
        Guild guild2 = guilds.get(g2);
        if (guild1 == null || guild2 == null) return false;

        int maxAllies = GuildConfig.MAX_ALLIANCES.get();
        if (maxAllies != 0) {
            if (guild1.getAllies().size() >= maxAllies) return false;
            if (guild2.getAllies().size() >= maxAllies) return false;
        }

        guild1.addAlly(g2);
        guild2.addAlly(g1);
        setDirty();
        return true;
    }

    public boolean removeAlliance(String g1, String g2) {
        Guild guild1 = guilds.get(g1);
        Guild guild2 = guilds.get(g2);
        if (guild1 == null || guild2 == null) return false;
        guild1.removeAlly(g2);
        guild2.removeAlly(g1);
        setDirty();
        return true;
    }

    public boolean endWar(String g1, String g2) {
        if (g1 == null || g2 == null) return false;
        final boolean[] removed = {false};
        activeWars.removeIf(w -> {
            boolean matches = (w.getAttackingGuildName().equals(g1) && w.getDefendingGuildName().equals(g2)) ||
                             (w.getAttackingGuildName().equals(g2) && w.getDefendingGuildName().equals(g1));
            if (matches) removed[0] = true;
            return matches;
        });
        if (removed[0]) {
            setDirty();
        }
        return removed[0];
    }

    // ---------------------------------------------------------------------
    // War helpers
    // ---------------------------------------------------------------------

    public War startWar(String attackingGuild, String defendingGuild) {
        if (getWar(attackingGuild, defendingGuild) != null) return null;
        War war = new War(attackingGuild, defendingGuild);
        // Break any existing alliance between the two guilds
        removeAlliance(attackingGuild, defendingGuild);
        activeWars.add(war);
        // Play warning horn sound to both guilds members
        var server=net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();
        if(server!=null){
            java.util.function.Consumer<GuildMember> play=(gm)->{
                var p=server.getPlayerList().getPlayer(gm.getPlayerUUID());
                if(p!=null){
                    p.playNotifySound(net.minecraft.sounds.SoundEvents.RAID_HORN.get(), net.minecraft.sounds.SoundSource.PLAYERS,1f,1f);
                }
            };
            guilds.get(attackingGuild).getMembers().forEach(play);
            guilds.get(defendingGuild).getMembers().forEach(play);
        }
        territoryWarCooldowns.put(defendingGuild, System.currentTimeMillis());
        setDirty();
        return war;
    }

    public @Nullable War getWar(String g1, String g2) {
        return activeWars.stream()
                .filter(w -> !w.isWarOver() && ((w.getAttackingGuildName().equals(g1) && w.getDefendingGuildName().equals(g2)) || (w.getAttackingGuildName().equals(g2) && w.getDefendingGuildName().equals(g1))))
                .findFirst().orElse(null);
    }

    public boolean isTerritoryUnderWar(String guildName) {
        return activeWars.stream().anyMatch(w -> !w.isWarOver() && w.getDefendingGuildName().equals(guildName));
    }

    public boolean canStartWarInTerritory(String territoryGuildName) {
        Long last = territoryWarCooldowns.get(territoryGuildName);
        if (last == null) return true;
        long cooldownMs = GuildConfig.WAR_COOLDOWN_HOURS.get() * 60L * 60L * 1000L;
        return System.currentTimeMillis() - last >= cooldownMs;
    }

    /**
     * Returns an active {@link War} that involves the given guild, or {@code null} if the guild is
     * not currently participating in any war.
     */
    public @org.jetbrains.annotations.Nullable War getActiveWarForGuild(String guildName) {
        if (guildName == null) return null;
        return activeWars.stream()
                .filter(w -> !w.isWarOver() && (w.getAttackingGuildName().equals(guildName) || w.getDefendingGuildName().equals(guildName)))
                .findFirst()
                .orElse(null);
    }

    // ---------------------------------------------------------------------
    // Permission helpers
    // ---------------------------------------------------------------------

    public boolean canModifyTerritory(UUID playerUUID, String territoryGuildName) {
        Guild playerGuild = getGuildByMember(playerUUID);
        if (playerGuild == null) return false;

        if (playerGuild.getName().equals(territoryGuildName)) return true;

        War war = getWar(playerGuild.getName(), territoryGuildName);
        if (war != null) {
            return !war.isPlayerLockedOut(playerUUID);
        }
        return false;
    }

    public boolean canEnterTerritory(UUID playerUUID, String territoryGuildName) {
        if (!isTerritoryUnderWar(territoryGuildName)) return true;
        Guild playerGuild = getGuildByMember(playerUUID);
        if (playerGuild == null) return false;
        War war = getWar(playerGuild.getName(), territoryGuildName);
        if (war == null) return false;
        return !war.isPlayerLockedOut(playerUUID);
    }

    // ---------------------------------------------------------------------
    // Tick helpers (cleanup finished wars)
    // ---------------------------------------------------------------------

    public void serverTick() {
        activeWars.removeIf(War::isWarOver);
    }

    // Helper for iteration without exposing internal map
    public java.util.Collection<Guild> getAllGuilds() {
        return java.util.Collections.unmodifiableCollection(guilds.values());
    }
} 