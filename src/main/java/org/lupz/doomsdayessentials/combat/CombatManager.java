package org.lupz.doomsdayessentials.combat;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.lupz.doomsdayessentials.network.PacketHandler;
import org.lupz.doomsdayessentials.network.packet.s2c.SyncCombatStatePacket;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Set;

/**
 * Tracks combat-tag state for each player. A player is considered "in combat"
 * for a certain
 * number of seconds after they deal or receive damage from another player.
 */
public class CombatManager {

    private static final CombatManager INSTANCE = new CombatManager();

    public static CombatManager get() {
        return INSTANCE;
    }

    // player UUID -> ticks remaining in combat
    private final Map<UUID, Integer> playersInCombat = new ConcurrentHashMap<>();
    private final Set<UUID> combatLoggers = ConcurrentHashMap.newKeySet();


    // Throttle broadcast to clients: send at most every 5 ticks (4 Hz)
    private int syncTickCounter = 0;

    private final Set<UUID> wantedPlayers = ConcurrentHashMap.newKeySet();
    private final Map<UUID, Long> wantedUntil = new ConcurrentHashMap<>();
    private final Map<UUID, Long> systemBountyCooldownUntil = new ConcurrentHashMap<>();
    private static final java.io.File WANTED_FILE = new java.io.File("wanted_players.json");
    private static final com.google.gson.Gson GSON = new com.google.gson.GsonBuilder().setPrettyPrinting().create();

    private static class UnjustCandidate {
        UUID attacker;
        boolean retaliated;
        long startMs;
        UnjustCandidate(UUID a, long t) { this.attacker = a; this.retaliated = false; this.startMs = t; }
    }
    private final Map<UUID, UnjustCandidate> unjustByVictim = new ConcurrentHashMap<>();
    private static class SavedInv {
        java.util.List<net.minecraft.world.item.ItemStack> items;
        java.util.List<net.minecraft.world.item.ItemStack> armor;
        java.util.List<net.minecraft.world.item.ItemStack> offhand;
    }
    private final Map<UUID, SavedInv> unjustSavedInv = new ConcurrentHashMap<>();
    private static class CurioSlotSnapshot {
        final String type;
        final int index;
        final net.minecraft.world.item.ItemStack stack;
        CurioSlotSnapshot(String t, int i, net.minecraft.world.item.ItemStack s){ this.type=t; this.index=i; this.stack=s; }
    }
    private final Map<UUID, java.util.List<CurioSlotSnapshot>> unjustSavedCurios = new ConcurrentHashMap<>();
    private static class CorpseRemovalMarker {
        final net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dim;
        final net.minecraft.core.BlockPos pos;
        int ticks;
        CorpseRemovalMarker(net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> d, net.minecraft.core.BlockPos p){ this.dim=d; this.pos=p; this.ticks=0; }
    }
    private final Map<UUID, CorpseRemovalMarker> corpseRemoval = new ConcurrentHashMap<>();
    private boolean corpseLoaded(){ try { return net.minecraftforge.fml.ModList.get().isLoaded("corpse"); } catch (Throwable t){ return false; } }
    private void tryRemoveNearbyCorpse(ServerPlayer p, CorpseRemovalMarker m){
        if (!corpseLoaded()) return;
        var level = p.server.getLevel(m.dim);
        if (level == null) return;
        double r = 6.0;
        net.minecraft.world.phys.AABB box = new net.minecraft.world.phys.AABB(m.pos.getX()-r, m.pos.getY()-r, m.pos.getZ()-r, m.pos.getX()+r, m.pos.getY()+r, m.pos.getZ()+r);
        java.util.List<net.minecraft.world.entity.Entity> list = level.getEntities(null, box);
        for (net.minecraft.world.entity.Entity e : list){
            String cn = e.getClass().getName();
            if (cn.equals("de.maxhenkel.corpse.entities.CorpseEntity")){
                e.discard();
            }
        }
    }
    private boolean curiosLoaded(){
        try { return net.minecraftforge.fml.ModList.get().isLoaded("curios"); } catch (Throwable t){ return false; }
    }
    private java.util.List<CurioSlotSnapshot> snapshotCurios(ServerPlayer p){
        java.util.List<CurioSlotSnapshot> list = new java.util.ArrayList<>();
        if (!curiosLoaded()) return list;
        try {
            Class<?> apiCls = Class.forName("top.theillusivec4.curios.api.CuriosApi");
            Object helper = apiCls.getMethod("getCuriosHelper").invoke(null);
            Object optHandler = helper.getClass().getMethod("getCuriosHandler", net.minecraft.world.entity.LivingEntity.class).invoke(helper, p);
            if (optHandler == null) return list;
            java.util.Optional<?> oh = (java.util.Optional<?>) optHandler;
            if (!oh.isPresent()) return list;
            Object handler = oh.get();
            Object curiosObj = handler.getClass().getMethod("getCurios").invoke(handler);
            java.util.Map<?,?> curiosMap = (java.util.Map<?,?>) curiosObj;
            for (java.util.Map.Entry<?,?> e : curiosMap.entrySet()) {
                String type = String.valueOf(e.getKey());
                Object stacksHandler = e.getValue();
                Object dyn = stacksHandler.getClass().getMethod("getStacks").invoke(stacksHandler);
                int slots = (int) dyn.getClass().getMethod("getSlots").invoke(dyn);
                for (int i=0;i<slots;i++){
                    net.minecraft.world.item.ItemStack st = (net.minecraft.world.item.ItemStack) dyn.getClass().getMethod("getStackInSlot", int.class).invoke(dyn, i);
                    if (st != null && !st.isEmpty()) list.add(new CurioSlotSnapshot(type, i, st.copy()));
                }
            }
        } catch (Throwable ignored) {}
        return list;
    }
    private void restoreCurios(ServerPlayer p, java.util.List<CurioSlotSnapshot> snaps){
        if (snaps == null || snaps.isEmpty()) return;
        if (!curiosLoaded()) {
            for (CurioSlotSnapshot s : snaps) p.getInventory().placeItemBackInInventory(s.stack.copy());
            return;
        }
        try {
            Class<?> apiCls = Class.forName("top.theillusivec4.curios.api.CuriosApi");
            Object helper = apiCls.getMethod("getCuriosHelper").invoke(null);
            Object optHandler = helper.getClass().getMethod("getCuriosHandler", net.minecraft.world.entity.LivingEntity.class).invoke(helper, p);
            if (optHandler == null) { for (CurioSlotSnapshot s : snaps) p.getInventory().placeItemBackInInventory(s.stack.copy()); return; }
            java.util.Optional<?> oh = (java.util.Optional<?>) optHandler;
            if (!oh.isPresent()) { for (CurioSlotSnapshot s : snaps) p.getInventory().placeItemBackInInventory(s.stack.copy()); return; }
            Object handler = oh.get();
            Object curiosObj = handler.getClass().getMethod("getCurios").invoke(handler);
            java.util.Map<?,?> curiosMap = (java.util.Map<?,?>) curiosObj;
            java.util.Map<String, java.util.List<CurioSlotSnapshot>> byType = new java.util.HashMap<>();
            for (CurioSlotSnapshot s : snaps) byType.computeIfAbsent(s.type, k->new java.util.ArrayList<>()).add(s);
            for (var entry : byType.entrySet()){
                String type = entry.getKey();
                Object stacksHandler = curiosMap.get(type);
                if (stacksHandler == null){ for (CurioSlotSnapshot s : entry.getValue()) p.getInventory().placeItemBackInInventory(s.stack.copy()); continue; }
                Object dyn = stacksHandler.getClass().getMethod("getStacks").invoke(stacksHandler);
                for (CurioSlotSnapshot s : entry.getValue()){
                    try {
                        dyn.getClass().getMethod("setStackInSlot", int.class, net.minecraft.world.item.ItemStack.class).invoke(dyn, s.index, s.stack.copy());
                    } catch (Throwable t){ p.getInventory().placeItemBackInInventory(s.stack.copy()); }
                }
            }
        } catch (Throwable t){ for (CurioSlotSnapshot s : snaps) p.getInventory().placeItemBackInInventory(s.stack.copy()); }
    }

    private CombatManager() {
        MinecraftForge.EVENT_BUS.register(this);
    }

    public boolean isInCombat(UUID uuid) {
        return playersInCombat.containsKey(uuid);
    }

    

    public int getRemainingTicks(UUID uuid) {
        return playersInCombat.getOrDefault(uuid, 0);
    }

    public void tagPlayer(ServerPlayer player) {
        playersInCombat.put(player.getUUID(), getDurationTicks());
        // No direct packet, state is synced in onServerTick
    }

    private int getDurationTicks() {
        return org.lupz.doomsdayessentials.config.EssentialsConfig.COMBAT_DURATION_SECONDS.get() * 20;
    }

    public void clearCombat(UUID uuid) {
        playersInCombat.remove(uuid);
    }

    public void addCombatLogger(UUID uuid) {
        combatLoggers.add(uuid);
    }

    public boolean isCombatLogger(UUID uuid) {
        return combatLoggers.contains(uuid);
    }

    public void removeCombatLogger(UUID uuid) {
        combatLoggers.remove(uuid);
    }

    public Map<UUID, Integer> getPlayersInCombat() {
        return playersInCombat;
    }

    public int getDefaultDurationTicks() {
        return getDurationTicks();
    }

    // ---------------------------------------------------------------------
    // Wanted System
    // ---------------------------------------------------------------------

    public void addWanted(UUID uuid) {
        int secs = org.lupz.doomsdayessentials.config.EssentialsConfig.COMBAT_FUGITIVE_DURATION_SECONDS.get();
        addWanted(uuid, Math.max(60, secs));
    }

    public void addWanted(UUID uuid, int durationSeconds) {
        long now = System.currentTimeMillis();
        long until = now + durationSeconds * 1000L;
        boolean newlyAdded = wantedPlayers.add(uuid);
        wantedUntil.put(uuid, until);
        saveWantedList();
        syncWantedState();
        if (newlyAdded) {
            long cdUntil = systemBountyCooldownUntil.getOrDefault(uuid, 0L);
            int cdMinutes = org.lupz.doomsdayessentials.config.EssentialsConfig.AUTO_BOUNTY_COOLDOWN_MINUTES.get();
            if (now >= cdUntil) {
                boolean placed = org.lupz.doomsdayessentials.professions.bounty.BountyManager.placeBounty(null, uuid, org.lupz.doomsdayessentials.item.ModItems.SCRAPMETAL.get(), 30);
                if (placed) {
                    systemBountyCooldownUntil.put(uuid, now + cdMinutes * 60_000L);
                }
            }
        }
    }

    public void removeWanted(UUID uuid) {
        boolean changed = wantedPlayers.remove(uuid) | (wantedUntil.remove(uuid) != null);
        if (changed) {
            saveWantedList();
            syncWantedState();
        }
    }

    public boolean isWanted(UUID uuid) {
        return wantedPlayers.contains(uuid);
    }

    public Set<UUID> getWantedPlayers() {
        return wantedPlayers;
    }

    public int getWantedRemainingSeconds(UUID uuid) {
        Long until = wantedUntil.get(uuid);
        if (until == null) return 0;
        long now = System.currentTimeMillis();
        long ms = Math.max(0, until - now);
        return (int) (ms / 1000L);
    }

    private void syncWantedState() {
        PacketHandler.CHANNEL.send(net.minecraftforge.network.PacketDistributor.ALL.noArg(),
                new SyncCombatStatePacket(playersInCombat, wantedPlayers));
    }

    private void saveWantedList() {
        try (java.io.Writer writer = new java.io.FileWriter(WANTED_FILE)) {
            java.util.Map<String, Object> root = new java.util.HashMap<>();
            java.util.List<String> ids = wantedPlayers.stream().map(java.util.UUID::toString).toList();
            java.util.Map<String, Long> untils = new java.util.HashMap<>();
            wantedUntil.forEach((k,v) -> untils.put(k.toString(), v));
            root.put("players", ids);
            root.put("until", untils);
            GSON.toJson(root, writer);
        } catch (java.io.IOException e) {
            e.printStackTrace();
        }
    }

    public void loadWantedList() {
        if (!WANTED_FILE.exists()) return;
        try (java.io.Reader reader = new java.io.FileReader(WANTED_FILE)) {
            com.google.gson.JsonObject obj = com.google.gson.JsonParser.parseReader(reader).getAsJsonObject();
            java.util.List<String> ids = new java.util.ArrayList<>();
            if (obj.has("players") && obj.get("players").isJsonArray()) {
                obj.get("players").getAsJsonArray().forEach(e -> ids.add(e.getAsString()));
            }
            java.util.Map<String, Long> untils = new java.util.HashMap<>();
            if (obj.has("until") && obj.get("until").isJsonObject()) {
                for (var entry : obj.get("until").getAsJsonObject().entrySet()) {
                    untils.put(entry.getKey(), entry.getValue().getAsLong());
                }
            }
            for (String s : ids) {
                try {
                    java.util.UUID id = java.util.UUID.fromString(s);
                    wantedPlayers.add(id);
                    Long u = untils.get(s);
                    if (u != null) wantedUntil.put(id, u);
                } catch (Throwable ignored) {}
            }
        } catch (java.io.IOException e) {
            e.printStackTrace();
        }
    }

    // ---------------------------------------------------------------------
    // Forge callbacks
    // ---------------------------------------------------------------------

    @SubscribeEvent
    public void onPlayerAttack(LivingAttackEvent event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer attacker))
            return;
        if (!(event.getEntity() instanceof ServerPlayer victim))
            return;

        // Don't tag players in creative or spectator mode
        if (attacker.isCreative() || attacker.isSpectator() || victim.isCreative() || victim.isSpectator()) {
            return;
        }

        // Check if either player is in an ARENA zone
        var attackerArea = AreaManager.get().getAreaAt(attacker.serverLevel(), attacker.blockPosition());
        var victimArea = AreaManager.get().getAreaAt(victim.serverLevel(), victim.blockPosition());
        if ((attackerArea != null && attackerArea.getType() == AreaType.ARENA) ||
                (victimArea != null && victimArea.getType() == AreaType.ARENA)) {
            return;
        }

        boolean wasVictimInCombat = isInCombat(victim.getUUID());
        tagPlayer(attacker);
        if (!org.lupz.doomsdayessentials.config.EssentialsConfig.COMBAT_ENTER_ON_ATTACK_ONLY.get()) {
            tagPlayer(victim);
        }
        if (!wasVictimInCombat) {
            unjustByVictim.putIfAbsent(victim.getUUID(), new UnjustCandidate(attacker.getUUID(), System.currentTimeMillis()));
        }
        UnjustCandidate c = unjustByVictim.get(attacker.getUUID());
        if (c != null && c.attacker.equals(victim.getUUID())) {
            c.retaliated = true;
        }
    }

    @SubscribeEvent
    public void onPlayerDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer victim)) return;
        boolean killerIsPlayer = event.getSource().getEntity() instanceof ServerPlayer;
        if (!killerIsPlayer) { unjustByVictim.remove(victim.getUUID()); return; }
        ServerPlayer killer = (ServerPlayer) event.getSource().getEntity();
        var area = org.lupz.doomsdayessentials.combat.AreaManager.get().getAreaAt(victim.serverLevel(), victim.blockPosition());
        boolean neutralZone = (area == null)
                || (area.getType() != org.lupz.doomsdayessentials.combat.AreaType.DANGER
                && area.getType() != org.lupz.doomsdayessentials.combat.AreaType.SAFE
                && area.getType() != org.lupz.doomsdayessentials.combat.AreaType.ARENA
                && area.getType() != org.lupz.doomsdayessentials.combat.AreaType.FREQUENCY
                && area.getType() != org.lupz.doomsdayessentials.combat.AreaType.RESOURCE
                && area.getType() != org.lupz.doomsdayessentials.combat.AreaType.PRISON);

        boolean victimWanted = isWanted(victim.getUUID());
        boolean acceptedHunt = org.lupz.doomsdayessentials.professions.bounty.BountyManager.isAcceptedHunt(killer.getUUID(), victim.getUUID());
        if (victimWanted || acceptedHunt) {
            unjustByVictim.remove(victim.getUUID());
            return;
        }

        UnjustCandidate c = unjustByVictim.get(victim.getUUID());
        if (c != null && c.attacker.equals(killer.getUUID()) && !c.retaliated) {
            addWanted(killer.getUUID());
            SavedInv si = new SavedInv();
            si.items = new java.util.ArrayList<>();
            for (net.minecraft.world.item.ItemStack s : victim.getInventory().items) si.items.add(s.copy());
            si.armor = new java.util.ArrayList<>();
            for (net.minecraft.world.item.ItemStack s : victim.getInventory().armor) si.armor.add(s.copy());
            si.offhand = new java.util.ArrayList<>();
            for (net.minecraft.world.item.ItemStack s : victim.getInventory().offhand) si.offhand.add(s.copy());
            unjustSavedInv.put(victim.getUUID(), si);
            victim.getPersistentData().putBoolean("keepInvUnjust", true);
            java.util.List<CurioSlotSnapshot> curios = snapshotCurios(victim);
            if (!curios.isEmpty()) unjustSavedCurios.put(victim.getUUID(), curios);
            corpseRemoval.put(victim.getUUID(), new CorpseRemovalMarker(victim.serverLevel().dimension(), victim.blockPosition()));
        } else if (area != null && area.getType() == AreaType.NEUTRAL && !isInCombat(victim.getUUID())) {
            addWanted(killer.getUUID());
            SavedInv si = new SavedInv();
            si.items = new java.util.ArrayList<>();
            for (net.minecraft.world.item.ItemStack s : victim.getInventory().items) si.items.add(s.copy());
            si.armor = new java.util.ArrayList<>();
            for (net.minecraft.world.item.ItemStack s : victim.getInventory().armor) si.armor.add(s.copy());
            si.offhand = new java.util.ArrayList<>();
            for (net.minecraft.world.item.ItemStack s : victim.getInventory().offhand) si.offhand.add(s.copy());
            unjustSavedInv.put(victim.getUUID(), si);
            victim.getPersistentData().putBoolean("keepInvUnjust", true);
            java.util.List<CurioSlotSnapshot> curios = snapshotCurios(victim);
            if (!curios.isEmpty()) unjustSavedCurios.put(victim.getUUID(), curios);
            corpseRemoval.put(victim.getUUID(), new CorpseRemovalMarker(victim.serverLevel().dimension(), victim.blockPosition()));
        } else if (neutralZone) {
            addWanted(killer.getUUID());
            corpseRemoval.put(victim.getUUID(), new CorpseRemovalMarker(victim.serverLevel().dimension(), victim.blockPosition()));
        }
        unjustByVictim.remove(victim.getUUID());
    }

    @SubscribeEvent
    public void onPlayerDrops(LivingDropsEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer sp)) return;
        if (!sp.getPersistentData().getBoolean("keepInvUnjust") && !unjustSavedInv.containsKey(sp.getUUID())) return;
        event.getDrops().clear();
    }

    @SubscribeEvent
    public void onClone(PlayerEvent.Clone event) {
        if (!(event.getOriginal() instanceof ServerPlayer orig) || !(event.getEntity() instanceof ServerPlayer fresh)) return;
        if (!event.isWasDeath()) return;
        SavedInv si = unjustSavedInv.remove(orig.getUUID());
        boolean flag = orig.getPersistentData().getBoolean("keepInvUnjust");
        orig.getPersistentData().remove("keepInvUnjust");
        if (si == null && !flag) return;
        fresh.getInventory().clearContent();
        if (si != null) {
            for (int i = 0; i < Math.min(fresh.getInventory().items.size(), si.items.size()); i++) {
                fresh.getInventory().items.set(i, si.items.get(i).copy());
            }
            for (int i = 0; i < Math.min(fresh.getInventory().armor.size(), si.armor.size()); i++) {
                fresh.getInventory().armor.set(i, si.armor.get(i).copy());
            }
            for (int i = 0; i < Math.min(fresh.getInventory().offhand.size(), si.offhand.size()); i++) {
                fresh.getInventory().offhand.set(i, si.offhand.get(i).copy());
            }
        }
        java.util.List<CurioSlotSnapshot> curios = unjustSavedCurios.remove(orig.getUUID());
        if (curios != null && !curios.isEmpty()) restoreCurios(fresh, curios);
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END)
            return;

        var server = ServerLifecycleHooks.getCurrentServer();
        if (server == null)
            return;

        for (UUID uuid : playersInCombat.keySet()) {
            playersInCombat.computeIfPresent(uuid, (k, v) -> v - 1);
        }

        playersInCombat.entrySet().removeIf(e -> e.getValue() <= 0);

        // Throttle broadcast: send only once every 5 ticks (~4 times per second)
        syncTickCounter++;
        if (syncTickCounter % 5 != 0)
            return;

        // Broadcast the updated combat state to all players
        PacketHandler.CHANNEL.send(net.minecraftforge.network.PacketDistributor.ALL.noArg(),
                new SyncCombatStatePacket(playersInCombat, wantedPlayers));

        if (!corpseRemoval.isEmpty()){
            java.util.Iterator<java.util.Map.Entry<UUID, CorpseRemovalMarker>> it = corpseRemoval.entrySet().iterator();
            while (it.hasNext()){
                var e = it.next();
                ServerPlayer p = server.getPlayerList().getPlayer(e.getKey());
                if (p != null){
                    tryRemoveNearbyCorpse(p, e.getValue());
                }
                e.getValue().ticks++;
                if (e.getValue().ticks >= 20) it.remove();
            }
        }
    }

    @SubscribeEvent
    public void onServerTickWantedCleanup(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        long now = System.currentTimeMillis();
        wantedPlayers.removeIf(uuid -> {
            Long until = wantedUntil.get(uuid);
            if (until != null && now >= until) {
                wantedUntil.remove(uuid);
                return true;
            }
            return false;
        });
    }
}
