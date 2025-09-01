package org.lupz.doomsdayessentials.entity;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.authlib.GameProfile;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager.ControllerRegistrar;
import software.bernie.geckolib.util.GeckoLibUtil;
import net.minecraftforge.network.PacketDistributor;
import org.lupz.doomsdayessentials.network.SentryShootSoundPacket;
import org.lupz.doomsdayessentials.network.PacketHandler;

import java.io.File;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class SentryEntity extends Mob implements GeoEntity {
    private static final EntityDataAccessor<ItemStack> DATA_GUN = SynchedEntityData.defineId(SentryEntity.class, EntityDataSerializers.ITEM_STACK);

    private static final Gson GSON = new Gson();
    private static final Map<String, GunStats> GUN_CACHE = new ConcurrentHashMap<>();

    private record GunStats(float rpm, float bulletDamage) {}

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private @Nullable ServerPlayer owner;
    private int lifeTicks = 20 * 30; // 30s
    private ItemStack mountedGun = ItemStack.EMPTY;

    private GameProfile shooterProfile;
    private FakePlayer cachedShooter;
    private long nextShotTick;
    // Ammo/reload state
    private int sentryAmmo;
    private long reloadEndTick;
    private int bulletsPerShot = 1;

    public SentryEntity(EntityType<? extends Mob> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setNoGravity(true);
        this.setNoAi(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0)
                .add(Attributes.MOVEMENT_SPEED, 0.0)
                .add(Attributes.ARMOR, 2.0);
    }

    public void setOwner(ServerPlayer owner) {
        this.owner = owner;
    }

    public ItemStack getMountedGun() {
        if (level().isClientSide) {
            return this.getEntityData().get(DATA_GUN);
        }
        return mountedGun;
    }

    private void setMountedGun(ItemStack stack) {
        this.mountedGun = stack;
        this.getEntityData().set(DATA_GUN, stack.copy());
        this.nextShotTick = 0L;
        this.reloadEndTick = 0L;
        this.bulletsPerShot = Math.max(1, getBulletsPerShot(stack));
        this.sentryAmmo = getCurrentAmmo(stack);
    }

    public void mountGunFrom(ServerPlayer player) {
        ItemStack held = player.getMainHandItem();
        if (getMountedGun().isEmpty() && isTACZWeapon(held)) {
            setMountedGun(held.copy());
            held.shrink(1);
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) return;
        if (--lifeTicks <= 0) {
            dropMountedGun();
            this.discard();
            return;
        }
        LivingEntity target = findTarget();
        if (!mountedGun.isEmpty() && target != null) {
            Vec3 diff = target.getEyePosition().subtract(this.getEyePosition());
            float yaw = (float)(Math.toDegrees(Math.atan2(-diff.x, diff.z)));
            this.setYRot(yaw);
            this.yHeadRot = yaw;
            this.yBodyRot = yaw;

            aimAndFireAt(target);
        }
    }

    private void dropMountedGun() {
        if (!mountedGun.isEmpty()) {
            this.spawnAtLocation(mountedGun.copy());
            setMountedGun(ItemStack.EMPTY);
        }
    }

    private @Nullable LivingEntity findTarget() {
        double half = 2.5;
        AABB box = new AABB(getX() - half, getY() - 1, getZ() - half, getX() + half, getY() + 3, getZ() + half);
        return level().getEntitiesOfClass(LivingEntity.class, box, e -> e != this && e.isAlive() && this.hasLineOfSight(e) &&
                ((e instanceof Player p && (owner == null || !p.getUUID().equals(owner.getUUID()))) ||
                 (e instanceof net.minecraft.world.entity.Mob)))
                .stream().min(Comparator.comparingDouble(e -> e.distanceToSqr(this))).orElse(null);
    }

    private FakePlayer getShooter(ServerLevel sl) {
        if (cachedShooter == null) {
            UUID base = owner != null ? owner.getUUID() : this.getUUID();
            this.shooterProfile = new GameProfile(UUID.nameUUIDFromBytes(("sentry-" + base.toString()).getBytes()), "Sentry");
            this.cachedShooter = FakePlayerFactory.get(sl, shooterProfile);
        }
        return cachedShooter;
    }

    private String getGunKey(ItemStack gun) {
        CompoundTag tag = gun.getTag();
        if (tag != null) {
            for (String k : new String[]{"GunId","gunId","gun_id"}) {
                if (tag.contains(k)) {
                    String id = tag.getString(k);
                    if (!id.isEmpty()) return id;
                }
            }
        }
        var key = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(gun.getItem());
        return key != null ? key.toString() : "tacz:unknown";
    }

    private File findGunJson(String ns, String path) {
        // Primary location: default pack
        File f = new File("run/tacz/tacz_default_gun/data/" + ns + "/data/guns/" + path + "_data.json");
        if (f.exists()) return f;
        // Scan other packs under run/tacz/*/data/<ns>/data/guns/
        File packsRoot = new File("run/tacz");
        File[] subs = packsRoot.listFiles(File::isDirectory);
        if (subs != null) {
            for (File pack : subs) {
                File candidate = new File(new File(new File(new File(pack, "data"), ns), "data/guns"), path + "_data.json");
                if (candidate.exists()) return candidate;
            }
        }
        return null;
    }

    private JsonObject loadGunJsonFromPacks(String ns, String path) {
        if (!(level() instanceof ServerLevel sl)) return null;
        ResourceManager rm = sl.getServer().getResourceManager();
        for (String sub : new String[]{"guns/" + path + "_data.json", "data/guns/" + path + "_data.json"}) {
            ResourceLocation rl = ResourceLocation.fromNamespaceAndPath(ns, sub);
            try {
                Optional<Resource> res = rm.getResource(rl);
                if (res.isPresent()) {
                    try (InputStreamReader reader = new InputStreamReader(res.get().open(), StandardCharsets.UTF_8)) {
                        return GSON.fromJson(reader, JsonObject.class);
                    }
                }
            } catch (Exception ignored) {}
        }
        return null;
    }

    private Optional<SoundEvent> resolveShootSoundFromAssets(String ns, String gunPath) {
        if (!(level() instanceof ServerLevel sl)) return Optional.empty();
        ResourceManager rm = sl.getServer().getResourceManager();
        try {
            ResourceLocation rl = ResourceLocation.fromNamespaceAndPath(ns, "sounds.json");
            Optional<Resource> res = rm.getResource(rl);
            if (res.isPresent()) {
                try (InputStreamReader reader = new InputStreamReader(res.get().open(), StandardCharsets.UTF_8)) {
                    JsonObject sounds = GSON.fromJson(reader, JsonObject.class);
                    SoundEvent best = null;
                    int bestScore = -1;
                    for (Map.Entry<String, JsonElement> e : sounds.entrySet()) {
                        String key = e.getKey();
                        if (!key.contains("shoot")) continue;
                        int score = 0;
                        if (key.contains(gunPath)) score += 2;
                        if (key.contains("_shoot_3p") || key.endsWith(".shoot")) score += 3;
                        if (key.contains("3p")) score += 1;
                        if (score > bestScore) {
                            ResourceLocation seId = ResourceLocation.fromNamespaceAndPath(ns, key);
                            SoundEvent se = ForgeRegistries.SOUND_EVENTS.getValue(seId);
                            if (se != null) {
                                best = se;
                                bestScore = score;
                            }
                        }
                    }
                    return Optional.ofNullable(best);
                }
            }
        } catch (Exception ignored) {}
        return Optional.empty();
    }

    private Optional<SoundEvent> resolveShootSound(ItemStack gun) {
        CompoundTag tag = gun.getTag();
        if (tag != null) {
            for (String k : new String[]{"shoot_sound","fire_sound","sound"}) {
                if (tag.contains(k)) {
                    String id = tag.getString(k);
                    if (!id.isEmpty()) {
                        ResourceLocation rl = ResourceLocation.tryParse(id);
                        if (rl == null) rl = ResourceLocation.fromNamespaceAndPath("tacz", id);
                        SoundEvent se = ForgeRegistries.SOUND_EVENTS.getValue(rl);
                        if (se != null) return Optional.of(se);
                    }
                }
            }
            for (String k : new String[]{"GunId","gunId","gun_id"}) {
                if (tag.contains(k)) {
                    String gunId = tag.getString(k);
                    if (!gunId.isEmpty()) {
                        String[] parts = gunId.split(":", 2);
                        String ns = parts.length == 2 ? parts[0] : "tacz";
                        String path = parts.length == 2 ? parts[1] : parts[0];
                        for (String candidate : new String[]{path + ".shoot", path + "_shoot_3p", path + "_shoot"}) {
                            ResourceLocation rl = ResourceLocation.fromNamespaceAndPath(ns, candidate);
                            SoundEvent se = ForgeRegistries.SOUND_EVENTS.getValue(rl);
                            if (se != null) return Optional.of(se);
                        }
                        Optional<SoundEvent> heur = resolveShootSoundFromAssets(ns, path);
                        if (heur.isPresent()) return heur;
                    }
                }
            }
        }
        return Optional.empty();
    }

    private GunStats loadGunStats(String gunKey) {
        return GUN_CACHE.computeIfAbsent(gunKey, k -> {
            try {
                String[] parts = k.split(":", 2);
                String ns = parts.length == 2 ? parts[0] : "tacz";
                String path = parts.length == 2 ? parts[1] : parts[0];
                JsonObject root = loadGunJsonFromPacks(ns, path);
                if (root == null) {
                    // FS fallback (dev only)
                    File f = findGunJson(ns, path);
                    if (f != null) {
                        try (FileReader r = new FileReader(f)) {
                            root = GSON.fromJson(r, JsonObject.class);
                        }
                    }
                }
                float rpm = 240f;
                float dmg = 2f;
                if (root != null) {
                    if (root.has("rpm")) rpm = root.get("rpm").getAsFloat();
                    if (root.has("bullet")) {
                        JsonObject bullet = root.getAsJsonObject("bullet");
                        if (bullet.has("damage")) dmg = bullet.get("damage").getAsFloat();
                    }
                }
                return new GunStats(Math.max(1f, rpm), Math.max(0.5f, dmg));
            } catch (Exception ignored) {
                return new GunStats(240f, 2f);
            }
        });
    }

    private int getIntervalTicksFromGun(ItemStack gun) {
        CompoundTag tag = gun.getTag();
        if (tag != null) {
            for (String key : new String[]{"rpm","RPM","rate_of_fire","fire_rate","rof"}) {
                if (tag.contains(key)) {
                    float rpm = tag.getFloat(key);
                    if (rpm > 0) return Math.max(1, Math.round(1200f / rpm));
                }
            }
            for (String key : new String[]{"cooldown","cd","tick_interval"}) {
                if (tag.contains(key)) return Math.max(1, tag.getInt(key));
            }
        }
        GunStats st = loadGunStats(getGunKey(gun));
        return Math.max(1, Math.round(1200f / st.rpm));
    }

    private float getDamageFromGun(ItemStack gun) {
        CompoundTag tag = gun.getTag();
        if (tag != null) {
            for (String key : new String[]{"damage","Damage","gun_damage","dmg"}) {
                if (tag.contains(key)) {
                    float v = tag.getFloat(key);
                    if (v > 0.1f) return v;
                }
            }
        }
        GunStats st = loadGunStats(getGunKey(gun));
        return st.bulletDamage;
    }

    private Optional<SoundEvent> resolveShootSoundFromRegistryHeuristic(String ns, String gunPath) {
        // Try to find any tacz sound whose key contains the gun path and the word "shoot"
        SoundEvent best = null;
        int bestScore = -1;
        for (ResourceLocation key : ForgeRegistries.SOUND_EVENTS.getKeys()) {
            if (!ns.equals(key.getNamespace())) continue;
            String p = key.getPath();
            if (!p.contains("shoot")) continue;
            int score = 0;
            // prefer exact tokens
            if (p.contains(gunPath)) score += 2;
            if (p.contains(gunPath + "_shoot_3p") || p.endsWith(".shoot")) score += 3;
            if (p.contains("3p")) score += 1;
            if (score > bestScore) {
                bestScore = score;
                best = ForgeRegistries.SOUND_EVENTS.getValue(key);
            }
        }
        return Optional.ofNullable(best);
    }

    private void aimAndFireAt(LivingEntity target) {
        if (!(level() instanceof ServerLevel sl)) return;
        if (mountedGun.isEmpty()) return;

        FakePlayer fp = getShooter(sl);
        fp.setPos(this.getX(), this.getY() + 1.0, this.getZ());
        fp.getCooldowns().tick();

        Vec3 to = target.getEyePosition().subtract(fp.getEyePosition());
        double horiz = Math.sqrt(to.x * to.x + to.z * to.z);
        float yaw = (float)(Math.toDegrees(Math.atan2(-to.x, to.z)));
        float pitch = (float)(Math.toDegrees(Math.atan2(-to.y, horiz)));
        fp.setYRot(yaw);
        fp.setXRot(pitch);
        fp.yHeadRot = yaw;
        fp.yBodyRot = yaw;

        int interval = getIntervalTicksFromGun(mountedGun);
        long now = level().getGameTime();

        // Handle reload window (5 seconds)
        if (reloadEndTick > now) return;
        if (reloadEndTick != 0 && now >= reloadEndTick) {
            // Finish reload
            setCurrentAmmo(mountedGun, getMagazineCapacity(mountedGun));
            reloadEndTick = 0L;
        }

        if (now < nextShotTick) return;

        if (sentryAmmo <= 0) {
            // Start reload and wait 5 seconds (100 ticks)
            reloadEndTick = now + 100L;
            return;
        }

        		fp.setItemInHand(InteractionHand.MAIN_HAND, mountedGun);
		fp.swing(InteractionHand.MAIN_HAND, true);

        String soundId = "doomsdayessentials:scar_l_shoot_3p";

        if (this.hasLineOfSight(target)) {
            float dmg = getDamageFromGun(mountedGun);
            target.hurt(level().damageSources().mobAttack(this), dmg);
            if (!soundId.isEmpty()) {
                PacketHandler.CHANNEL.send(PacketDistributor.TRACKING_ENTITY.with(() -> this), new SentryShootSoundPacket(this.getId(), soundId, 1.0f, 1.0f));
            }
        } else {
            if (!soundId.isEmpty()) {
                PacketHandler.CHANNEL.send(PacketDistributor.TRACKING_ENTITY.with(() -> this), new SentryShootSoundPacket(this.getId(), soundId, 0.6f, 0.8f));
            }
        }

        		// Consume 1 ammo per trigger shot
		setCurrentAmmo(mountedGun, Math.max(0, sentryAmmo - 1));

        nextShotTick = now + interval;
    }

    @Override
    public InteractionResult interactAt(Player player, Vec3 vec, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        if (!held.isEmpty() && getMountedGun().isEmpty() && isTACZWeapon(held)) {
            setMountedGun(held.copy());
            held.shrink(1);
            return InteractionResult.SUCCESS;
        }
        if (held.isEmpty() && player.isShiftKeyDown() && player instanceof ServerPlayer sp) {
            if (!getMountedGun().isEmpty()) {
                sp.getInventory().placeItemBackInInventory(getMountedGun().copy());
                setMountedGun(ItemStack.EMPTY);
                return InteractionResult.SUCCESS;
            }
        }
        return super.interactAt(player, vec, hand);
    }

    private boolean isTACZWeapon(ItemStack stack) {
        var key = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem());
        return key != null && "tacz".equals(key.getNamespace());
    }

    // --- Gun data helpers (capacity, bullets per shot, NBT) ---
    private int getIntTag(CompoundTag tag, int fallback, String... keys) {
        for (String k : keys) {
            if (tag.contains(k)) return tag.getInt(k);
        }
        return fallback;
    }

    private @Nullable JsonObject getGunJson(ItemStack gun) {
        String[] parts = getGunKey(gun).split(":", 2);
        String ns = parts.length == 2 ? parts[0] : "tacz";
        String path = parts.length == 2 ? parts[1] : parts[0];
        JsonObject root = loadGunJsonFromPacks(ns, path);
        if (root == null) {
            File f = findGunJson(ns, path);
            if (f != null) {
                try (FileReader r = new FileReader(f)) {
                    root = GSON.fromJson(r, JsonObject.class);
                } catch (Exception ignored) {}
            }
        }
        return root;
    }

    private int getMagazineCapacity(ItemStack gun) {
        CompoundTag tag = gun.getOrCreateTag();
        int tagCap = getIntTag(tag, -1, "GunMaxAmount", "MaxAmmo", "ammo_amount");
        if (tagCap > 0) return tagCap;
        JsonObject root = getGunJson(gun);
        if (root != null) {
            int base = root.has("ammo_amount") ? root.get("ammo_amount").getAsInt() : -1;
            if (root.has("extended_mag_ammo_amount")) {
                int lvl = getIntTag(tag, 0, "xmag_level", "XMagLevel", "GunXMagLevel", "extended_mag_level", "ext_mag_level");
                try {
                    var arr = root.getAsJsonArray("extended_mag_ammo_amount");
                    if (lvl > 0 && lvl <= arr.size()) {
                        return Math.max(1, arr.get(lvl - 1).getAsInt());
                    }
                } catch (Exception ignored) {}
            }
            if (base > 0) return base;
        }
        return 30; // sensible default
    }

    private int getBulletsPerShot(ItemStack gun) {
        CompoundTag tag = gun.getTag();
        if (tag != null) {
            int v = getIntTag(tag, -1, "bullet_amount", "BulletAmount");
            if (v > 0) return v;
        }
        JsonObject root = getGunJson(gun);
        if (root != null && root.has("bullet")) {
            JsonObject bullet = root.getAsJsonObject("bullet");
            if (bullet.has("bullet_amount")) {
                return Math.max(1, bullet.get("bullet_amount").getAsInt());
            }
        }
        return 1;
    }

    private int getCurrentAmmo(ItemStack gun) {
        CompoundTag tag = gun.getTag();
        if (tag != null && tag.contains("GunCurrentAmount")) {
            return Math.max(0, tag.getInt("GunCurrentAmount"));
        }
        return getMagazineCapacity(gun);
    }

    private void setCurrentAmmo(ItemStack gun, int amount) {
        int cap = getMagazineCapacity(gun);
        int clamped = Math.max(0, Math.min(cap, amount));
        gun.getOrCreateTag().putInt("GunCurrentAmount", clamped);
        this.sentryAmmo = clamped;
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.getEntityData().define(DATA_GUN, ItemStack.EMPTY);
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (key.equals(DATA_GUN)) {
            if (level().isClientSide) {
                this.mountedGun = this.getEntityData().get(DATA_GUN);
            }
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        lifeTicks = tag.getInt("lifeTicks");
        if (tag.contains("Gun")) setMountedGun(ItemStack.of(tag.getCompound("Gun")));
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("lifeTicks", lifeTicks);
        if (!mountedGun.isEmpty()) tag.put("Gun", mountedGun.save(new CompoundTag()));
    }

    @Override
    public void registerControllers(ControllerRegistrar controllers) {}

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
} 