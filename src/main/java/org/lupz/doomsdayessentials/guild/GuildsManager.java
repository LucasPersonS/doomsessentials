package org.lupz.doomsdayessentials.guild;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;
import org.jetbrains.annotations.Nullable;
import org.lupz.doomsdayessentials.territory.ResourceGeneratorManager;

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
    private final java.util.Map<String, Long> guildProtectionUntil = new java.util.HashMap<>();
    /**
     * Pair-wise ban: attacker -> (defender -> until). Prevents re-invading a
     * specific defender.
     */
    private final java.util.Map<String, java.util.Map<String, Long>> pairBanUntil = new java.util.HashMap<>();
    /**
     * Pending alliance invites (not persisted): targetGuild -> set of inviterGuilds
     */
    private final java.util.Map<String, java.util.Set<String>> pendingAllianceInvites = new java.util.HashMap<>();
    /**
     * Persistent guild storage (global chest-like inventory) indexed by guild name.
     */
    private final java.util.Map<String, net.minecraft.core.NonNullList<net.minecraft.world.item.ItemStack>> guildStorages = new java.util.HashMap<>();
    /** Log of storage changes per guild (append-only ring). */
    private final java.util.Map<String, java.util.List<StorageLogEntry>> storageLogs = new java.util.HashMap<>();
    /** Guild resources (balances) keyed by guild -> (resourceId -> amount). */
    private final java.util.Map<String, java.util.Map<String, Integer>> guildResources = new java.util.HashMap<>();
    private final java.util.Map<String, java.util.Map<String, Integer>> guildMail = new java.util.HashMap<>();
    /**
     * Preferred timezone ID for storage logs (applies globally). Defaults to
     * America/Sao_Paulo.
     */
    private String storageLogTimeZone = "America/Sao_Paulo";

    public static class StorageLogEntry {
        public long ts;
        public java.util.UUID actor;
        public String actorName = ""; // display name captured at log time
        public String action; // add/remove/move
        public String itemId;
        public int amount;
        public int page;
        public int slot;

        public StorageLogEntry() {
        }

        public StorageLogEntry(long ts, java.util.UUID actor, String actorName, String action, String itemId,
                int amount, int page, int slot) {
            this.ts = ts;
            this.actor = actor;
            this.actorName = actorName == null ? "" : actorName;
            this.action = action;
            this.itemId = itemId;
            this.amount = amount;
            this.page = page;
            this.slot = slot;
        }

        public net.minecraft.nbt.CompoundTag toTag() {
            net.minecraft.nbt.CompoundTag t = new net.minecraft.nbt.CompoundTag();
            t.putLong("ts", ts);
            if (actor != null) {
                t.putUUID("actor", actor);
            }
            t.putString("actorName", actorName == null ? "" : actorName);
            t.putString("action", action);
            t.putString("itemId", itemId);
            t.putInt("amount", amount);
            t.putInt("page", page);
            t.putInt("slot", slot);
            return t;
        }

        public static StorageLogEntry fromTag(net.minecraft.nbt.CompoundTag t) {
            StorageLogEntry e = new StorageLogEntry();
            e.ts = t.getLong("ts");
            e.actor = t.contains("actor") ? t.getUUID("actor") : null;
            e.actorName = t.contains("actorName") ? t.getString("actorName") : ""; // backward compat
            e.action = t.getString("action");
            e.itemId = t.getString("itemId");
            e.amount = t.getInt("amount");
            e.page = t.getInt("page");
            e.slot = t.getInt("slot");
            return e;
        }
    }

    // ---------------------------------------------------------------------
    // Construction / loading
    // ---------------------------------------------------------------------

    public GuildsManager() {
    }

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
            gtag.putInt("storageLevel", guild.getStorageLevel());
            gtag.putInt("protectionLevel", guild.getProtectionLevel());

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

        // Persist guild protection
        if (!guildProtectionUntil.isEmpty()) {
            CompoundTag prot = new CompoundTag();
            guildProtectionUntil.forEach(prot::putLong);
            tag.put("guildProtection", prot);
        }

        // Persist pair bans
        if (!pairBanUntil.isEmpty()) {
            CompoundTag pairs = new CompoundTag();
            for (var e : pairBanUntil.entrySet()) {
                CompoundTag inner = new CompoundTag();
                e.getValue().forEach(inner::putLong);
                pairs.put(e.getKey(), inner);
            }
            tag.put("pairBans", pairs);
        }
        // Persist storages
        if (!guildStorages.isEmpty()) {
            CompoundTag stores = new CompoundTag();
            for (var e : guildStorages.entrySet()) {
                net.minecraft.nbt.ListTag list = new net.minecraft.nbt.ListTag();
                for (net.minecraft.world.item.ItemStack stack : e.getValue()) {
                    CompoundTag s = new CompoundTag();
                    // clamp to 64/1 and remove any legacy ext tag
                    int limit = Math.max(1, Math.min(64, stack.getMaxStackSize()));
                    net.minecraft.world.item.ItemStack toSave = stack.copy();
                    if (toSave.hasTag() && toSave.getTag().isEmpty())
                        toSave.setTag(null);
                    toSave.setCount(Math.min(Math.max(0, toSave.getCount()), limit));
                    toSave.save(s);
                    list.add(s);
                }
                stores.put(e.getKey(), list);
            }
            tag.put("guildStorages", stores);
            try {
                org.lupz.doomsdayessentials.EssentialsMod.LOGGER.info("StorageSave: guilds=" + guildStorages.size());
            } catch (Throwable ignored) {
            }
        }
        // Persist guild resources
        if (!guildResources.isEmpty()) {
            CompoundTag res = new CompoundTag();
            for (var e : guildResources.entrySet()) {
                CompoundTag inner = new CompoundTag();
                for (var r : e.getValue().entrySet()) {
                    inner.putInt(r.getKey(), r.getValue());
                }
                res.put(e.getKey(), inner);
            }
            tag.put("guildResources", res);
        }
        if (!storageLogs.isEmpty()) {
            CompoundTag logs = new CompoundTag();
            for (var e : storageLogs.entrySet()) {
                net.minecraft.nbt.ListTag list = new net.minecraft.nbt.ListTag();
                for (StorageLogEntry le : e.getValue())
                    list.add(le.toTag());
                logs.put(e.getKey(), list);
            }
            tag.put("storageLogs", logs);
        }
        if (!guildMail.isEmpty()) {
            CompoundTag mails = new CompoundTag();
            for (var e : guildMail.entrySet()) {
                CompoundTag inner = new CompoundTag();
                for (var m : e.getValue().entrySet()) inner.putInt(m.getKey(), m.getValue());
                mails.put(e.getKey(), inner);
            }
            tag.put("guildMail", mails);
        }
        // Persist log timezone
        tag.putString("storageLogTimeZone", storageLogTimeZone == null ? "America/Sao_Paulo" : storageLogTimeZone);
        return tag;
    }

    private void read(CompoundTag tag) {
        if (tag.contains("guilds")) {
            ListTag guildsList = tag.getList("guilds", Tag.TAG_COMPOUND);
            for (int i = 0; i < guildsList.size(); i++) {
                CompoundTag gtag = guildsList.getCompound(i);
                String name = gtag.getString("name");
                String gtagTag = gtag.getString("tag");
                UUID leader = gtag.getUUID("leader");
                Guild guild = new Guild(name, gtagTag, leader);
                if (gtag.contains("storageLevel")) {
                    guild.setStorageLevel(gtag.getInt("storageLevel"));
                }
                if (gtag.contains("protectionLevel")) {
                    guild.setProtectionLevel(gtag.getInt("protectionLevel"));
                }

                if (gtag.contains("totemX")) {
                    guild.setTotemPosition(
                            new BlockPos(gtag.getInt("totemX"), gtag.getInt("totemY"), gtag.getInt("totemZ")));
                }
                if (gtag.contains("baseX")) {
                    guild.setBasePosition(
                            new BlockPos(gtag.getInt("baseX"), gtag.getInt("baseY"), gtag.getInt("baseZ")));
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
        }
        if (tag.contains("pairBans")) {
            CompoundTag pairs = tag.getCompound("pairBans");
            for (String key : pairs.getAllKeys()) {
                territoryWarCooldowns.put(key, pairs.getLong(key));
            }
        }
        // Load log timezone
        if (tag.contains("storageLogTimeZone")) {
            storageLogTimeZone = tag.getString("storageLogTimeZone");
        }
        if (tag.contains("guildProtection")) {
            CompoundTag prot = tag.getCompound("guildProtection");
            for (String k : prot.getAllKeys()) {
                guildProtectionUntil.put(k, prot.getLong(k));
            }
        }
        if (tag.contains("pairBans")) {
            CompoundTag pairs = tag.getCompound("pairBans");
            for (String atk : pairs.getAllKeys()) {
                CompoundTag inner = pairs.getCompound(atk);
                java.util.Map<String, Long> map = new java.util.HashMap<>();
                for (String def : inner.getAllKeys()) {
                    map.put(def, inner.getLong(def));
                }
                pairBanUntil.put(atk, map);
            }
        }
        if (tag.contains("guildStorages")) {
            CompoundTag stores = tag.getCompound("guildStorages");
            for (String gname : stores.getAllKeys()) {
                net.minecraft.nbt.ListTag list = stores.getList(gname, Tag.TAG_COMPOUND);
                java.util.List<net.minecraft.world.item.ItemStack> result = new java.util.ArrayList<>();
                for (int i = 0; i < list.size(); i++) {
                    net.minecraft.world.item.ItemStack st = net.minecraft.world.item.ItemStack.of(list.getCompound(i));
                    try {
                        int limit = Math.max(1, Math.min(64, st.getMaxStackSize()));
                        int total = Math.max(0, st.getCount());
                        // strip legacy tag
                        if (st.hasTag()) {
                            st.getTag().remove("gd_ext_count");
                            if (st.hasTag() && st.getTag().isEmpty())
                                st.setTag(null);
                        }
                        while (total > 0) {
                            net.minecraft.world.item.ItemStack piece = st.copy();
                            int put = Math.min(limit, total);
                            piece.setCount(put);
                            result.add(piece);
                            total -= put;
                        }
                    } catch (Throwable ignored) {
                        result.add(st);
                    }
                }
                Guild g = guilds.get(gname);
                int pages = (g == null ? 5 : (4 + g.getStorageLevel()));
                int baseCap = 54 * pages;
                int cap = Math.max(baseCap, result.size());
                net.minecraft.core.NonNullList<net.minecraft.world.item.ItemStack> inv = net.minecraft.core.NonNullList
                        .withSize(cap, net.minecraft.world.item.ItemStack.EMPTY);
                int abs = 0;
                for (net.minecraft.world.item.ItemStack piece : result) {
                    while (abs < cap) {
                        int rel = abs % 54;
                        if (rel != 45 && rel != 53 && rel != 46 && rel != 47 && rel != 52 && rel != 50)
                            break;
                        abs++;
                    }
                    if (abs >= cap)
                        break;
                    inv.set(abs, piece);
                    abs++;
                }
                guildStorages.put(gname, inv);
            }
            try {
                org.lupz.doomsdayessentials.EssentialsMod.LOGGER.info("StorageLoad: guilds=" + guildStorages.size());
            } catch (Throwable ignored) {
            }
        }
        if (tag.contains("storageLogs")) {
            CompoundTag logs = tag.getCompound("storageLogs");
            for (String gname : logs.getAllKeys()) {
                net.minecraft.nbt.ListTag list = logs.getList(gname, Tag.TAG_COMPOUND);
                java.util.List<StorageLogEntry> arr = new java.util.ArrayList<>();
                for (int i = 0; i < list.size(); i++)
                    arr.add(StorageLogEntry.fromTag(list.getCompound(i)));
                storageLogs.put(gname, arr);
            }
        }
        if (tag.contains("guildMail")) {
            CompoundTag mails = tag.getCompound("guildMail");
            for (String gname : mails.getAllKeys()) {
                CompoundTag inner = mails.getCompound(gname);
                java.util.Map<String, Integer> map = new java.util.HashMap<>();
                for (String id : inner.getAllKeys()) map.put(id, inner.getInt(id));
                guildMail.put(gname, map);
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

    public java.util.Set<String> getGuildNames() {
        return new java.util.HashSet<>(guilds.keySet());
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

    public int getProtectionLevel(String guildName) {
        Guild g = guilds.get(guildName);
        return g == null ? 0 : g.getProtectionLevel();
    }

    public int getProtectionPercent(String guildName) {
        int lvl = getProtectionLevel(guildName);
        return switch (lvl) {
            case 1 -> 20;
            case 2 -> 30;
            case 3 -> 40;
            case 4 -> 50;
            case 5 -> 55;
            case 6 -> 60;
            case 7 -> 65;
            default -> 0;
        };
    }

    public int getProtectionUpgradeCost(int nextLevel) {
        return switch (nextLevel) {
            case 1 -> 500;
            case 2 -> 700;
            case 3 -> 900;
            case 4 -> 1200;
            case 5 -> 1800;
            case 6 -> 2400;
            case 7 -> 3000;
            default -> Integer.MAX_VALUE;
        };
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
        if (guild == null)
            return false;
        int max = GuildConfig.MAX_GUILD_MEMBERS.get();
        if (max != 0 && guild.getMembers().size() >= max)
            return false;
        guild.addMember(uuid);
        setDirty();
        return true;
    }

    public boolean removeMember(String guildName, UUID uuid) {
        Guild guild = guilds.get(guildName);
        if (guild == null)
            return false;
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
        if (g == null)
            return false;
        return addMember(g, playerUUID);
    }

    /** Returns the pending invite guild name for the player, or null if none. */
    public @org.jetbrains.annotations.Nullable String getInviteFor(UUID playerUUID) {
        return pendingInvites.get(playerUUID);
    }

    public void clearInvitesForGuild(String guildName) {
        pendingInvites.entrySet().removeIf(e -> e.getValue().equals(guildName));
    }

    // ---------------------------------------------------------------------
    // Alliance helpers
    // ---------------------------------------------------------------------

    public boolean areAllied(String g1, String g2) {
        if (g1 == null || g2 == null)
            return false;
        if (g1.equals(g2))
            return true; // same guild treated as allied
        Guild guild1 = guilds.get(g1);
        Guild guild2 = guilds.get(g2);
        if (guild1 == null || guild2 == null)
            return false;
        return guild1.isAlliedWith(g2) && guild2.isAlliedWith(g1);
    }

    public boolean addAlliance(String g1, String g2) {
        if (g1 == null || g2 == null)
            return false;
        if (g1.equals(g2))
            return false;
        Guild guild1 = guilds.get(g1);
        Guild guild2 = guilds.get(g2);
        if (guild1 == null || guild2 == null)
            return false;

        int maxAllies = GuildConfig.MAX_ALLIANCES.get();
        if (maxAllies != 0) {
            if (guild1.getAllies().size() >= maxAllies)
                return false;
            if (guild2.getAllies().size() >= maxAllies)
                return false;
        }

        guild1.addAlly(g2);
        guild2.addAlly(g1);
        setDirty();
        return true;
    }

    // Alliance invites (ephemeral)
    // -----------------------------------------------------------

    public void sendAllianceInvite(String fromGuild, String toGuild) {
        pendingAllianceInvites.computeIfAbsent(toGuild, k -> new java.util.HashSet<>()).add(fromGuild);
    }

    public boolean hasAllianceInvite(String toGuild, String fromGuild) {
        return pendingAllianceInvites.getOrDefault(toGuild, java.util.Collections.emptySet()).contains(fromGuild);
    }

    public boolean acceptAllianceInvite(String toGuild, String fromGuild) {
        java.util.Set<String> set = pendingAllianceInvites.get(toGuild);
        if (set == null || !set.remove(fromGuild))
            return false;
        if (set.isEmpty())
            pendingAllianceInvites.remove(toGuild);
        return addAlliance(toGuild, fromGuild);
    }

    /**
     * Returns a copy of pending alliance invites for the given guild (guild names),
     * or empty set.
     */
    public java.util.Set<String> getAllianceInvitesFor(String toGuild) {
        java.util.Set<String> set = pendingAllianceInvites.get(toGuild);
        if (set == null)
            return java.util.Collections.emptySet();
        return new java.util.HashSet<>(set);
    }

    public boolean removeAlliance(String g1, String g2) {
        Guild guild1 = guilds.get(g1);
        Guild guild2 = guilds.get(g2);
        if (guild1 == null || guild2 == null)
            return false;
        guild1.removeAlly(g2);
        guild2.removeAlly(g1);
        setDirty();
        return true;
    }

    public boolean endWar(String g1, String g2) {
        if (g1 == null || g2 == null)
            return false;
        final boolean[] removed = { false };
        activeWars.removeIf(w -> {
            boolean matches = (w.getAttackingGuildName().equals(g1) && w.getDefendingGuildName().equals(g2)) ||
                    (w.getAttackingGuildName().equals(g2) && w.getDefendingGuildName().equals(g1));
            if (matches)
                removed[0] = true;
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
        if (getWar(attackingGuild, defendingGuild) != null)
            return null;
        War war = new War(attackingGuild, defendingGuild);
        // Break any existing alliance between the two guilds
        removeAlliance(attackingGuild, defendingGuild);
        activeWars.add(war);
        // Play warning horn sound to both guilds members
        var server = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();
        if (server != null) {
            java.util.function.Consumer<GuildMember> play = (gm) -> {
                var p = server.getPlayerList().getPlayer(gm.getPlayerUUID());
                if (p != null) {
                    p.playNotifySound(net.minecraft.sounds.SoundEvents.RAID_HORN.get(),
                            net.minecraft.sounds.SoundSource.PLAYERS, 1f, 1f);
                }
            };
            guilds.get(attackingGuild).getMembers().forEach(play);
            guilds.get(defendingGuild).getMembers().forEach(play);

            int protPct = getProtectionPercent(defendingGuild);
            if (protPct > 0) {
                for (GuildMember gmbr : guilds.get(defendingGuild).getMembers()) {
                    var p = server.getPlayerList().getPlayer(gmbr.getPlayerUUID());
                    if (p != null) {
                        p.connection.send(new net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket(net.minecraft.network.chat.Component.literal("§aProteção Ativa")));
                        p.connection.send(new net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket(net.minecraft.network.chat.Component.literal("§7Cofre protegido em §e" + protPct + "%")));
                    }
                }
            }
        }
        territoryWarCooldowns.put(defendingGuild, System.currentTimeMillis());
        setDirty();
        return war;
    }

    public @Nullable War getWar(String g1, String g2) {
        return activeWars.stream()
                .filter(w -> !w.isWarOver()
                        && ((w.getAttackingGuildName().equals(g1) && w.getDefendingGuildName().equals(g2))
                                || (w.getAttackingGuildName().equals(g2) && w.getDefendingGuildName().equals(g1))))
                .findFirst().orElse(null);
    }

    public boolean isTerritoryUnderWar(String guildName) {
        return activeWars.stream().anyMatch(w -> !w.isWarOver() && w.getDefendingGuildName().equals(guildName));
    }

    public boolean canStartWarInTerritory(String territoryGuildName) {
        Long last = territoryWarCooldowns.get(territoryGuildName);
        if (last == null)
            return true;
        long cooldownMs = GuildConfig.WAR_COOLDOWN_HOURS.get() * 60L * 60L * 1000L;
        return System.currentTimeMillis() - last >= cooldownMs;
    }

    /**
     * Returns an active {@link War} that involves the given guild, or {@code null}
     * if the guild is
     * not currently participating in any war.
     */
    public @org.jetbrains.annotations.Nullable War getActiveWarForGuild(String guildName) {
        if (guildName == null)
            return null;
        return activeWars.stream()
                .filter(w -> !w.isWarOver()
                        && (w.getAttackingGuildName().equals(guildName) || w.getDefendingGuildName().equals(guildName)))
                .findFirst()
                .orElse(null);
    }

    // ---------------------------------------------------------------------
    // Permission helpers
    // ---------------------------------------------------------------------

    public boolean canModifyTerritory(UUID playerUUID, String territoryGuildName) {
        Guild playerGuild = getGuildByMember(playerUUID);
        if (playerGuild == null)
            return false;

        if (playerGuild.getName().equals(territoryGuildName))
            return true;

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
        if (playerGuild.getName().equals(territoryGuildName)) return true;
        War war = getWar(playerGuild.getName(), territoryGuildName);
        if (war == null) return false;
        return !war.isPlayerLockedOut(playerUUID);
    }

    // ---------------------------------------------------------------------
    // Tick helpers (cleanup finished wars)
    // ---------------------------------------------------------------------

    public void serverTick() {
        java.util.List<War> finished = new java.util.ArrayList<>();
        for (War w : activeWars) {
            if (w.isWarOver())
                finished.add(w);
        }
        if (!finished.isEmpty()) {
            long now = System.currentTimeMillis();
            long weekMs = GuildConfig.WAR_COOLDOWN_HOURS.get() * 60L * 60L * 1000L;
            for (War w : finished) {
                // Timeout -> attackers failed; set pair ban
                setDefenderShield(w.getDefendingGuildName(), now + weekMs);
                setPairBan(w.getAttackingGuildName(), w.getDefendingGuildName(), now + weekMs);
                // Also ban allies of attacker from invading this defender during the period
                Guild atk = guilds.get(w.getAttackingGuildName());
                if (atk != null) {
                    for (String ally : atk.getAllies()) {
                        setPairBan(ally, w.getDefendingGuildName(), now + weekMs);
                    }
                }
            }
            activeWars.removeAll(finished);
            setDirty();
        }
    }

    // Helper for iteration without exposing internal map
    public java.util.Collection<Guild> getAllGuilds() {
        return java.util.Collections.unmodifiableCollection(guilds.values());
    }

    public boolean deleteGuild(String name) {
        Guild g = guilds.remove(name);
        if (g == null)
            return false;

        // clear invites that reference it
        pendingInvites.entrySet().removeIf(e -> e.getValue().equals(name));

        // break alliances & wars that involve it
        guilds.values().forEach(other -> other.removeAlly(name));
        activeWars.removeIf(w -> w.getAttackingGuildName().equals(name) ||
                w.getDefendingGuildName().equals(name));

        // unclaim resource-generator areas
        ResourceGeneratorManager.get().getGeneratorsForGuild(name)
                .forEach(d -> ResourceGeneratorManager.get().unclaimArea(d.areaName));

        // remove storage
        guildStorages.remove(name);
        storageLogs.remove(name);

        setDirty();
        return true;
    }

    // -------------------------------------------------------------------------------------
    // Guild storage API
    // -------------------------------------------------------------------------------------

    public net.minecraft.core.NonNullList<net.minecraft.world.item.ItemStack> getOrCreateStorage(String guildName) {
        if (guildName == null)
            return net.minecraft.core.NonNullList.create();
        net.minecraft.core.NonNullList<net.minecraft.world.item.ItemStack> inv = guildStorages.get(guildName);
        if (inv == null) {
            Guild g = guilds.get(guildName);
            int pages = (g == null ? 5 : (4 + g.getStorageLevel()));
            inv = net.minecraft.core.NonNullList.withSize(54 * pages, net.minecraft.world.item.ItemStack.EMPTY);
            guildStorages.put(guildName, inv);
        }
        return inv;
    }

    public void setStorageSize(String guildName, int size) {
        net.minecraft.core.NonNullList<net.minecraft.world.item.ItemStack> cur = getOrCreateStorage(guildName);
        if (cur.size() == size)
            return;
        net.minecraft.core.NonNullList<net.minecraft.world.item.ItemStack> resized = net.minecraft.core.NonNullList
                .withSize(size, net.minecraft.world.item.ItemStack.EMPTY);
        for (int i = 0; i < Math.min(size, cur.size()); i++)
            resized.set(i, cur.get(i));
        guildStorages.put(guildName, resized);
        setDirty();
    }

    public void logStorageChange(String guild, java.util.UUID actor, String action, String itemId, int amount, int page,
            int slot) {
        // Backward-compatible entry point (no name)
        logStorageChangeWithName(guild, actor, "", action, itemId, amount, page, slot);
    }

    public void logStorageChangeWithName(String guild, java.util.UUID actor, String actorName, String action,
            String itemId, int amount, int page, int slot) {
        java.util.List<StorageLogEntry> list = storageLogs.computeIfAbsent(guild, k -> new java.util.ArrayList<>());
        list.add(new StorageLogEntry(System.currentTimeMillis(), actor, actorName, action, itemId, amount, page, slot));
        if (list.size() > 1000)
            list.remove(0);
        setDirty();
    }

    public java.util.List<StorageLogEntry> getStorageLogs(String guild) {
        return java.util.Collections
                .unmodifiableList(storageLogs.getOrDefault(guild, java.util.Collections.emptyList()));
    }

    // Log timezone API
    public java.time.ZoneId getLogZoneId() {
        try {
            return java.time.ZoneId.of(storageLogTimeZone == null || storageLogTimeZone.isEmpty() ? "America/Sao_Paulo"
                    : storageLogTimeZone);
        } catch (Exception e) {
            return java.time.ZoneId.of("America/Sao_Paulo");
        }
    }

    public void setLogTimeZone(String zoneId) {
        if (zoneId == null || zoneId.isEmpty())
            return;
        try {
            java.time.ZoneId.of(zoneId);
            storageLogTimeZone = zoneId;
            setDirty();
        } catch (Exception ignored) {
        }
    }

    public java.util.Map<String, Integer> getGuildMail(String guildName) {
        return guildMail.computeIfAbsent(guildName, k -> new java.util.HashMap<>());
    }

    public void addToGuildMail(String guildName, String itemId, int amount) {
        if (guildName == null || itemId == null || amount <= 0) return;
        java.util.Map<String, Integer> mail = getGuildMail(guildName);
        mail.merge(itemId, amount, Integer::sum);
        setDirty();
    }

    /** Collects all mail items into the player's inventory; returns total items given. */
    public int collectGuildMail(net.minecraft.server.level.ServerPlayer player, String guildName) {
        long cdMin = org.lupz.doomsdayessentials.guild.GuildConfig.MAIL_COLLECT_COOLDOWN_MINUTES.get();
        if (cdMin > 0) {
            var tag = player.getPersistentData();
            long last = tag.getLong("de_mail_collect");
            long now = System.currentTimeMillis();
            long cdMs = cdMin * 60L * 1000L;
            if (now - last < cdMs) {
                long rem = cdMs - (now - last);
                long min = rem / 60000L; long sec = (rem % 60000L) / 1000L;
                player.sendSystemMessage(net.minecraft.network.chat.Component.literal("§eAguarde " + min + "m" + sec + "s para coletar novamente."));
                return 0;
            }
            tag.putLong("de_mail_collect", now);
        }
        java.util.Map<String, Integer> mail = guildMail.get(guildName);
        if (mail == null || mail.isEmpty()) return 0;
        int given = 0;
        java.util.List<String> ids = new java.util.ArrayList<>(mail.keySet());
        for (String id : ids) {
            int amt = mail.getOrDefault(id, 0);
            if (amt <= 0) continue;
            net.minecraft.world.item.Item itemReg = net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(net.minecraft.resources.ResourceLocation.tryParse(id));
            if (itemReg == null || itemReg == net.minecraft.world.level.block.Blocks.AIR.asItem()) continue;
            int remaining = amt;
            while (remaining > 0) {
                int stackSize = Math.min(itemReg.getMaxStackSize(), remaining);
                net.minecraft.world.item.ItemStack stack = new net.minecraft.world.item.ItemStack(itemReg, stackSize);
                if (!player.getInventory().add(stack)) {
                    player.drop(stack, false);
                }
                remaining -= stackSize;
            }
            given += amt;
            try {
                logStorageChangeWithName(guildName, player.getUUID(), player.getName().getString(), "mail_collect", id, amt, 0, 0);
            } catch (Throwable ignored) {}
            mail.remove(id);
        }
        if (given > 0) setDirty();
        return given;
    }

    public int withdrawFromGuildStorage(net.minecraft.server.level.ServerPlayer player, String guildName, String itemId, int amount) {
        if (guildName == null || itemId == null || amount <= 0) return 0;
        net.minecraft.core.NonNullList<net.minecraft.world.item.ItemStack> storage = getOrCreateStorage(guildName);
        net.minecraft.resources.ResourceLocation rl = net.minecraft.resources.ResourceLocation.tryParse(itemId);
        net.minecraft.world.item.Item item = net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(rl);
        if (item == null || item == net.minecraft.world.level.block.Blocks.AIR.asItem()) return 0;
        int remaining = amount;
        int removed = 0;
        for (int i = 0; i < storage.size() && remaining > 0; i++) {
            net.minecraft.world.item.ItemStack st = storage.get(i);
            if (st.isEmpty() || st.getItem() != item) continue;
            int take = Math.min(remaining, st.getCount());
            st.shrink(take);
            remaining -= take;
            removed += take;
            if (st.getCount() <= 0) storage.set(i, net.minecraft.world.item.ItemStack.EMPTY);
        }
        if (removed <= 0) return 0;
        int giveLeft = removed;
        while (giveLeft > 0) {
            int stackSize = Math.min(item.getMaxStackSize(), giveLeft);
            net.minecraft.world.item.ItemStack stack = new net.minecraft.world.item.ItemStack(item, stackSize);
            if (!player.getInventory().add(stack)) player.drop(stack, false);
            giveLeft -= stackSize;
        }
        logStorageChangeWithName(guildName, player.getUUID(), player.getName().getString(), "withdraw", itemId, removed, 0, 0);
        setDirty();
        return removed;
    }

    // -------------------------------------------------------------------------------------
    // Additional helpers for new rules
    // -------------------------------------------------------------------------------------

    public boolean isGuildProtected(String guildName) {
        Long until = guildProtectionUntil.get(guildName);
        return until != null && System.currentTimeMillis() < until;
    }

    public boolean isPairBanned(String attacker, String defender) {
        java.util.Map<String, Long> inner = pairBanUntil.get(attacker);
        if (inner == null)
            return false;
        Long until = inner.get(defender);
        return until != null && System.currentTimeMillis() < until;
    }

    public void setDefenderShield(String guildName, long until) {
        guildProtectionUntil.put(guildName, until);
        territoryWarCooldowns.put(guildName, until); // align territory cooldown with shield expiry baseline
        setDirty();
    }

    public void setPairBan(String attacker, String defender, long until) {
        pairBanUntil.computeIfAbsent(attacker, k -> new java.util.HashMap<>()).put(defender, until);
        setDirty();
    }

    public void onAttackerVictory(net.minecraft.server.level.ServerLevel level, String attacker, String defender) {
        long now = System.currentTimeMillis();
        long weekMs = GuildConfig.WAR_COOLDOWN_HOURS.get() * 60L * 60L * 1000L;
        setDefenderShield(defender, now + weekMs);
        endWar(attacker, defender);
        int target = GuildConfig.PLUNDER_ITEM_COUNT.get();
        int protPct = getProtectionPercent(defender);
        int availGen = 0;
        var genMgr = org.lupz.doomsdayessentials.territory.ResourceGeneratorManager.get();
        var gens = genMgr.getGeneratorsForGuild(defender);
        if (gens != null) {
            for (var g : gens) {
                for (org.lupz.doomsdayessentials.territory.ResourceAreaData.LootEntry e : g.lootEntries) {
                    availGen += Math.max(0, e.stored);
                }
            }
        }
        int availSt = getStorageItemCount(defender);
        int availBank = org.lupz.doomsdayessentials.guild.GuildResourceBank.get(level).getTotal(defender);
        int totalAvail = Math.max(0, availGen + availSt + availBank);
        int capByProt = (int) Math.floor(totalAvail * (100 - protPct) / 100.0);
        target = Math.min(target, capByProt);
        java.util.Map<String, Integer> details = new java.util.HashMap<>();
        java.util.Map<String, Integer> gen = org.lupz.doomsdayessentials.territory.ResourceGeneratorManager.get().plunderDetailed(defender, attacker, target);
        int movedGen = 0; for (Integer v : gen.values()) movedGen += v;
        for (var e : gen.entrySet()) details.merge(e.getKey(), e.getValue(), Integer::sum);
        int remain = Math.max(0, target - movedGen);
        if (remain > 0) {
            java.util.Map<String, Integer> stor = plunderFromGuildStorageDetailed(defender, attacker, remain);
            for (var e : stor.entrySet()) details.merge(e.getKey(), e.getValue(), Integer::sum);
            int movedStor = 0; for (Integer v : stor.values()) movedStor += v;
            remain = Math.max(0, remain - movedStor);
        }
        if (remain > 0) {
            java.util.Map<String, Integer> bank = org.lupz.doomsdayessentials.guild.GuildResourceBank.get(level).plunderDetailed(defender, attacker, remain);
            for (var e : bank.entrySet()) details.merge(e.getKey(), e.getValue(), Integer::sum);
        }
        int moved = 0;
        for (Integer v : details.values()) moved += v;
        var server = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();
        if (server != null) {
            net.minecraft.network.chat.Component msg = net.minecraft.network.chat.Component.literal(
                "§cCofre destruído! §e" + moved + " itens saqueados de " + defender + " para " + attacker + ".");
            server.getPlayerList().broadcastSystemMessage(msg, false);
            java.util.List<net.minecraft.server.level.ServerPlayer> players = server.getPlayerList().getPlayers();
            java.util.List<String> lines = new java.util.ArrayList<>();
            int shown = 0;
            for (var e : details.entrySet()) {
                if (shown >= 8) break;
                lines.add("§7- §f" + e.getKey() + " §ex" + e.getValue());
                shown++;
            }
            for (var p : players) {
                Guild g = getGuildByMember(p.getUUID());
                if (g == null) continue;
                String gn = g.getName();
                if (attacker.equals(gn) || defender.equals(gn)) {
                    p.connection.send(new net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket(net.minecraft.network.chat.Component.literal("§cSaque Concluído")));
                    for (String s : lines) p.sendSystemMessage(net.minecraft.network.chat.Component.literal(s));
                }
            }
            for (var e : details.entrySet()) {
                logStorageChangeWithName(defender, null, attacker, "plunder_out", e.getKey(), e.getValue(), 0, 0);
                logStorageChangeWithName(attacker, null, defender, "plunder_in", e.getKey(), e.getValue(), 0, 0);
            }
            if (protPct > 0) {
                int protectedCount = Math.max(0, totalAvail - target);
                net.minecraft.network.chat.Component protMsg = net.minecraft.network.chat.Component.literal(
                        "§aProteção salvou §e" + protectedCount + " itens/recursos (" + protPct + "%).");
                server.getPlayerList().broadcastSystemMessage(protMsg, false);
            }
        }
        territoryWarCooldowns.put(defender, now);
        setDirty();
    }

    public int getOnlineCount(ServerLevel level, String guildName) {
        if (guildName == null)
            return 0;
        int count = 0;
        for (var p : level.getServer().getPlayerList().getPlayers()) {
            Guild g = getGuildByMember(p.getUUID());
            if (g != null && guildName.equals(g.getName()))
                count++;
        }
        return count;
    }

    // -------------------------------------------------------------------------------------
    // Storage capacity helpers (by item count, not slots)
    // -------------------------------------------------------------------------------------

    public int getStorageCapacityItems(String guildName) {
        Guild g = guilds.get(guildName);
        int pages = (g == null ? 5 : (4 + g.getStorageLevel()));
        return 54 * pages;
    }

    /**
     * Returns the current total item count stored across all pages of the global
     * storage.
     */
    public int getStorageItemCount(String guildName) {
        var inv = getOrCreateStorage(guildName);
        int total = 0;
        for (net.minecraft.world.item.ItemStack s : inv)
            if (!s.isEmpty())
                total += s.getCount();
        return total;
    }

    public java.util.Map<String, Integer> plunderFromGuildStorageDetailed(String defenderGuild, String attackerGuild, int totalCount) {
        java.util.Map<String, Integer> movedById = new java.util.HashMap<>();
        if (totalCount <= 0) return movedById;
        var src = getOrCreateStorage(defenderGuild);
        var dst = getOrCreateStorage(attackerGuild);
        java.util.List<Integer> idxs = new java.util.ArrayList<>();
        for (int i = 0; i < src.size(); i++) {
            if (!src.get(i).isEmpty()) idxs.add(i);
        }
        if (idxs.isEmpty()) return movedById;
        java.util.Collections.shuffle(idxs);
        int remaining = totalCount;
        for (int i : idxs) {
            if (remaining <= 0) break;
            net.minecraft.world.item.ItemStack st = src.get(i);
            if (st.isEmpty()) continue;
            int take = Math.min(st.getCount(), remaining);
            if (take <= 0) continue;
            String id = net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(st.getItem()).toString();
            int canPut = getPutCapacity(dst, st.getItem(), take);
            int toMail = take - canPut;
            // Remove full take from defender
            st.shrink(take);
            if (st.getCount() <= 0) src.set(i, net.minecraft.world.item.ItemStack.EMPTY);
            // Deposit into attacker storage up to capacity
            int putLeft = canPut;
            for (int j = 0; j < dst.size() && putLeft > 0; j++) {
                net.minecraft.world.item.ItemStack cur = dst.get(j);
                if (!cur.isEmpty() && cur.getItem() == st.getItem() && cur.getCount() < cur.getMaxStackSize()) {
                    int space = cur.getMaxStackSize() - cur.getCount();
                    int add = Math.min(space, putLeft);
                    cur.grow(add);
                    putLeft -= add;
                }
            }
            for (int j = 0; j < dst.size() && putLeft > 0; j++) {
                net.minecraft.world.item.ItemStack cur = dst.get(j);
                if (cur.isEmpty()) {
                    int add = Math.min(st.getItem().getMaxStackSize(), putLeft);
                    dst.set(j, new net.minecraft.world.item.ItemStack(st.getItem(), add));
                    putLeft -= add;
                }
            }
            if (toMail > 0) {
                addToGuildMail(attackerGuild, id, toMail);
                logStorageChangeWithName(attackerGuild, null, defenderGuild, "plunder_mail", id, toMail, 0, 0);
            }
            movedById.merge(id, take, Integer::sum);
        }
        setDirty();
        return movedById;
    }

    private int getPutCapacity(net.minecraft.core.NonNullList<net.minecraft.world.item.ItemStack> dst, net.minecraft.world.item.Item item, int desired) {
        int maxStack = item.getMaxStackSize();
        int space = 0;
        for (int j = 0; j < dst.size(); j++) {
            net.minecraft.world.item.ItemStack cur = dst.get(j);
            if (!cur.isEmpty() && cur.getItem() == item && cur.getCount() < cur.getMaxStackSize()) {
                space += (cur.getMaxStackSize() - cur.getCount());
                if (space >= desired) return desired;
            }
        }
        for (int j = 0; j < dst.size(); j++) {
            net.minecraft.world.item.ItemStack cur = dst.get(j);
            if (cur.isEmpty()) {
                space += maxStack;
                if (space >= desired) return desired;
            }
        }
        return Math.min(space, desired);
    }

    /** Increases storage level by one and resizes storage to add one page. */
    public boolean upgradeStorageLevel(String guildName) {
        Guild g = guilds.get(guildName);
        if (g == null)
            return false;
        int cur = g.getStorageLevel();
        g.setStorageLevel(cur + 1);
        int pages = 4 + g.getStorageLevel();
        setStorageSize(guildName, 54 * pages);
        setDirty();
        return true;
    }
}
