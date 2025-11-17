package org.lupz.doomsdayessentials.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;
import net.minecraftforge.network.PacketDistributor;
import org.lupz.doomsdayessentials.airdrop.AirdropConfig;
import org.lupz.doomsdayessentials.airdrop.AirdropLootManager;
import org.lupz.doomsdayessentials.airdrop.AirdropChestMenu;
import org.lupz.doomsdayessentials.airdrop.network.AirdropNoticePacket;
import org.lupz.doomsdayessentials.network.PacketHandler;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * Airdrop entity that slowly descends and becomes an interactable loot container on landing.
 */
public class AirdropEntity extends Entity implements GeoEntity {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    private boolean landed = false;
    private boolean announced = false;
    // Timestamp em milissegundos de quando tocou o chão e quando deve expirar
    private long landTimeMs = 0L;
    private long expireAtMs = 0L;
    // Flag para indicar que o loot foi inicializado (para não remover antes da primeira abertura)
    private boolean lootInitialized = false;
    private final SimpleContainer container = new SimpleContainer(27); // 3 rows x 9 cols

    public AirdropEntity(EntityType<?> type, Level level) {
        super(type, level);
        this.noPhysics = false;
        this.setNoGravity(false);
        // Set bounding box for interaction
        this.setBoundingBox(this.makeBoundingBox());
    }

    @Override
    protected void defineSynchedData() {
        // no synced data fields
    }

    @Override
    public void tick() {
        super.tick();

        if (!landed) {
            // Apply gravity and movement manually
            Vec3 motion = this.getDeltaMovement();
            
            // Apply gravity (standard is -0.08 per tick)
            double newY = motion.y - 0.08;
            
            // Limit fall speed to configured value
            double maxDown = -Math.max(0.005, AirdropConfig.FALL_SPEED.get());
            if (newY < maxDown) {
                newY = maxDown;
            }
            
            // Apply air resistance to horizontal movement
            double newX = motion.x * 0.98;
            double newZ = motion.z * 0.98;
            
            // Set the new movement
            this.setDeltaMovement(newX, newY, newZ);
            
            // Move the entity with collision detection
            this.move(MoverType.SELF, this.getDeltaMovement());
            
            // Check for landing conditions
            if (this.onGround() || this.verticalCollision) {
                landed = true;
                this.setDeltaMovement(Vec3.ZERO);
                this.setNoGravity(true);
                // Define timestamps de pouso e expiração (15 minutos)
                this.landTimeMs = System.currentTimeMillis();
                this.expireAtMs = this.landTimeMs + (15L * 60L * 1000L);
                
                // Play landing sound
                if (!this.level().isClientSide) {
                    this.level().playSound(null, this.getX(), this.getY(), this.getZ(), 
                        SoundEvents.ANVIL_LAND, SoundSource.BLOCKS, 0.5f, 1.0f);
                    // Broadcast landing HUD notice to all players
                    try {
                        PacketHandler.CHANNEL.send(PacketDistributor.ALL.noArg(),
                                new AirdropNoticePacket("airdrop.landed", AirdropNoticePacket.STATE_LANDED));
                    } catch (Exception ignored) {}
                }
            }
        } else {
            // Lógica de remoção no servidor: some se esvaziar após loot inicializado ou se expirar por tempo
            if (!this.level().isClientSide) {
                // Remover por esvaziamento (apenas após inicialização do loot)
                if (lootInitialized && isContainerEmpty()) {
                    // Notify clients of despawn
                    try {
                        PacketHandler.CHANNEL.send(PacketDistributor.ALL.noArg(),
                                new AirdropNoticePacket("airdrop.disappeared", AirdropNoticePacket.STATE_DESPAWNED));
                    } catch (Exception ignored) {}
                    this.discard();
                } else if (expireAtMs > 0 && System.currentTimeMillis() >= expireAtMs) {
                    // Notify clients of despawn
                    try {
                        PacketHandler.CHANNEL.send(PacketDistributor.ALL.noArg(),
                                new AirdropNoticePacket("airdrop.disappeared", AirdropNoticePacket.STATE_DESPAWNED));
                    } catch (Exception ignored) {}
                    this.discard();
                }
            }
        }
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        if (this.level().isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (!(player instanceof ServerPlayer sp)) return InteractionResult.PASS;
        if (!landed) return InteractionResult.PASS; // only interactable after landing

        // First-time loot population
        if (!lootInitialized) {
            AirdropLootManager.populate(container, (ServerLevel) this.level(), sp, this.position());
            lootInitialized = true;
        }

        // Play opening sound
        this.level().playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.CHEST_OPEN, SoundSource.BLOCKS, 1.0f, 1.0f);

        // Announce once when first opened
        if (!announced) {
            announced = true;
            broadcastLooted(sp);
            // Broadcast HUD typing message to all players
            try {
                // Include player name so clients can highlight it in the HUD
                String openerName = sp.getName().getString();
                PacketHandler.CHANNEL.send(PacketDistributor.ALL.noArg(),
                        new AirdropNoticePacket("airdrop.opened_by", AirdropNoticePacket.STATE_OPENED, openerName));
            } catch (Exception ignored) {}
        }

        // Open chest-like menu using custom menu to handle close sound
        sp.openMenu(new net.minecraft.world.MenuProvider() {
            @Override public net.minecraft.network.chat.Component getDisplayName() {
                return net.minecraft.network.chat.Component.literal("Airdrop");
            }
            @Override public AbstractContainerMenu createMenu(int id, net.minecraft.world.entity.player.Inventory inv, Player p) {
                return new AirdropChestMenu(id, inv, container, 3, AirdropEntity.this);
            }
        });

        return InteractionResult.SUCCESS;
    }

    private boolean isContainerEmpty() {
        for (int i = 0; i < container.getContainerSize(); i++) {
            if (!container.getItem(i).isEmpty()) return false;
        }
        return true;
    }

    private void broadcastLooted(ServerPlayer opener) {
        String msg = String.format("[Airdrop] %s has looted an airdrop at %d, %d, %d", opener.getName().getString(), (int)this.getX(), (int)this.getY(), (int)this.getZ());
        if (this.level() instanceof ServerLevel sl && sl.getServer() != null) {
            for (ServerPlayer p : sl.getServer().getPlayerList().getPlayers()) {
                p.sendSystemMessage(net.minecraft.network.chat.Component.literal(msg));
            }
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        this.landed = tag.getBoolean("Landed");
        this.announced = tag.getBoolean("Announced");
        this.landTimeMs = tag.contains("LandTimeMs") ? tag.getLong("LandTimeMs") : 0L;
        this.expireAtMs = tag.contains("ExpireAtMs") ? tag.getLong("ExpireAtMs") : 0L;
        this.lootInitialized = tag.getBoolean("LootInitialized");
        // Load items
        for (int i = 0; i < container.getContainerSize(); i++) container.setItem(i, net.minecraft.world.item.ItemStack.EMPTY);
        int size = tag.getInt("Size");
        for (int i = 0; i < size && i < container.getContainerSize(); i++) {
            if (tag.contains("Item" + i)) {
                container.setItem(i, net.minecraft.world.item.ItemStack.of(tag.getCompound("Item" + i)));
            }
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putBoolean("Landed", this.landed);
        tag.putBoolean("Announced", this.announced);
        tag.putLong("LandTimeMs", this.landTimeMs);
        tag.putLong("ExpireAtMs", this.expireAtMs);
        tag.putBoolean("LootInitialized", this.lootInitialized);
        tag.putInt("Size", container.getContainerSize());
        for (int i = 0; i < container.getContainerSize(); i++) {
            net.minecraft.world.item.ItemStack s = container.getItem(i);
            if (!s.isEmpty()) {
                CompoundTag it = new CompoundTag();
                s.save(it);
                tag.put("Item" + i, it);
            }
        }
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        // No animation controllers currently
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    public boolean canBeCollidedWith() {
        return !this.isRemoved();
    }

    @Override
    public boolean isPickable() {
        return !this.isRemoved();
    }

    @Override
    public float getPickRadius() {
        return 1.0f;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public net.minecraft.world.entity.EntityDimensions getDimensions(net.minecraft.world.entity.Pose pose) {
        return net.minecraft.world.entity.EntityDimensions.scalable(1.0f, 1.0f);
    }
}
