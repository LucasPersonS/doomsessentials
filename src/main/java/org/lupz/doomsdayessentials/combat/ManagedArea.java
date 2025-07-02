package org.lupz.doomsdayessentials.combat;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.core.registries.Registries;

/**
 * Immutable definition of a rectangular combat area in a specific dimension.
 * The two positions may come in any order; the constructor will sort them so that
 * {@code pos1} is the minimum corner and {@code pos2} is the maximum corner.
 */
public final class ManagedArea {

    private final String name;
    private final AreaType type;
    private final ResourceKey<Level> dimension;
    private final BlockPos pos1;
    private final BlockPos pos2;

    // Optional behavioural flags -------------------------------------------------------------

    private boolean preventPvp;
    private boolean preventExplosions;
    private boolean preventMobGriefing;
    private boolean preventBlockModification;
    private boolean allowFlight;
    private boolean disableFallDamage;
    private boolean preventHungerLoss;
    private boolean healPlayers;
    private float radiationDamage;
    private String entryMessage;
    private String exitMessage;

    public ManagedArea(@NotNull String name,
                       @NotNull AreaType type,
                       @NotNull ResourceKey<Level> dimension,
                       @NotNull BlockPos pos1,
                       @NotNull BlockPos pos2,
                       boolean preventPvp,
                       boolean preventExplosions,
                       boolean preventMobGriefing,
                       boolean preventBlockModification,
                       boolean allowFlight,
                       boolean disableFallDamage,
                       boolean preventHungerLoss,
                       boolean healPlayers,
                       float radiationDamage,
                       String entryMessage,
                       String exitMessage) {
        this.name = name;
        this.type = type;
        this.dimension = dimension;

        // Ensure min/max ordering for convenience in containment checks
        int minX = Math.min(pos1.getX(), pos2.getX());
        int minY = Math.min(pos1.getY(), pos2.getY());
        int minZ = Math.min(pos1.getZ(), pos2.getZ());
        int maxX = Math.max(pos1.getX(), pos2.getX());
        int maxY = Math.max(pos1.getY(), pos2.getY());
        int maxZ = Math.max(pos1.getZ(), pos2.getZ());

        this.pos1 = new BlockPos(minX, minY, minZ);
        this.pos2 = new BlockPos(maxX, maxY, maxZ);

        this.preventPvp = preventPvp;
        this.preventExplosions = preventExplosions;
        this.preventMobGriefing = preventMobGriefing;
        this.preventBlockModification = preventBlockModification;
        this.allowFlight = allowFlight;
        this.disableFallDamage = disableFallDamage;
        this.preventHungerLoss = preventHungerLoss;
        this.healPlayers = healPlayers;
        this.radiationDamage = radiationDamage;
        this.entryMessage = entryMessage;
        this.exitMessage = exitMessage;
    }

    public ManagedArea(@NotNull String name,
                       @NotNull AreaType type,
                       @NotNull ResourceKey<Level> dimension,
                       @NotNull BlockPos pos1,
                       @NotNull BlockPos pos2) {
        this(name, type, dimension, pos1, pos2, false, false, false, false, false, false, false, false, 0.0f, "", "");
    }

    /**
     * Copy constructor to create a new area with different bounds but identical properties.
     */
    public ManagedArea(ManagedArea other, BlockPos newPos1, BlockPos newPos2) {
        this(other.name, other.type, other.dimension, newPos1, newPos2,
                other.preventPvp, other.preventExplosions, other.preventMobGriefing,
                other.preventBlockModification, other.allowFlight, other.disableFallDamage,
                other.preventHungerLoss, other.healPlayers, other.radiationDamage,
                other.entryMessage, other.exitMessage);
    }

    public static final Codec<ManagedArea> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.STRING.fieldOf("name").forGetter(ManagedArea::getName),
                    AreaType.CODEC.fieldOf("type").forGetter(ManagedArea::getType),
                    ResourceKey.codec(Registries.DIMENSION).fieldOf("dimension").forGetter(ManagedArea::getDimension),
                    BlockPos.CODEC.fieldOf("pos1").forGetter(ManagedArea::getPos1),
                    BlockPos.CODEC.fieldOf("pos2").forGetter(ManagedArea::getPos2),
                    Codec.BOOL.optionalFieldOf("prevent_pvp", false).forGetter(ManagedArea::isPreventPvp),
                    Codec.BOOL.optionalFieldOf("prevent_explosions", false).forGetter(ManagedArea::isPreventExplosions),
                    Codec.BOOL.optionalFieldOf("prevent_mob_griefing", false).forGetter(ManagedArea::isPreventMobGriefing),
                    Codec.BOOL.optionalFieldOf("prevent_block_modification", false).forGetter(ManagedArea::isPreventBlockModification),
                    Codec.BOOL.optionalFieldOf("allow_flight", false).forGetter(ManagedArea::isAllowFlight),
                    Codec.BOOL.optionalFieldOf("disable_fall_damage", false).forGetter(ManagedArea::isDisableFallDamage),
                    Codec.BOOL.optionalFieldOf("prevent_hunger_loss", false).forGetter(ManagedArea::isPreventHungerLoss),
                    Codec.BOOL.optionalFieldOf("heal_players", false).forGetter(ManagedArea::isHealPlayers),
                    Codec.FLOAT.optionalFieldOf("radiation_damage", 0.0f).forGetter(ManagedArea::getRadiationDamage),
                    Codec.STRING.optionalFieldOf("entry_message", "").forGetter(ManagedArea::getEntryMessage),
                    Codec.STRING.optionalFieldOf("exit_message", "").forGetter(ManagedArea::getExitMessage)
            ).apply(instance, ManagedArea::new)
    );

    // ------------------------------------------------------------------------
    // Basic getters
    // ------------------------------------------------------------------------

    public String getName() {
        return name;
    }

    public AreaType getType() {
        return type;
    }

    public ResourceKey<Level> getDimension() {
        return dimension;
    }

    public BlockPos getPos1() {
        return pos1;
    }

    public BlockPos getPos2() {
        return pos2;
    }

    // ------------------------------------------------------------------------
    // Logic
    // ------------------------------------------------------------------------

    /**
     * Checks whether the given coordinates (same dimension) are inside this area.
     */
    public boolean contains(BlockPos pos) {
        return pos.getX() >= pos1.getX() && pos.getX() <= pos2.getX()
                && pos.getY() >= pos1.getY() && pos.getY() <= pos2.getY()
                && pos.getZ() >= pos1.getZ() && pos.getZ() <= pos2.getZ();
    }

    // ------------------------------------------------------------------------
    // Flags
    // ------------------------------------------------------------------------

    public boolean isPreventPvp() {
        return preventPvp;
    }

    public void setPreventPvp(boolean preventPvp) {
        this.preventPvp = preventPvp;
    }

    public boolean isPreventExplosions() {
        return preventExplosions;
    }

    public void setPreventExplosions(boolean preventExplosions) {
        this.preventExplosions = preventExplosions;
    }

    public boolean isPreventMobGriefing() {
        return preventMobGriefing;
    }

    public void setPreventMobGriefing(boolean preventMobGriefing) {
        this.preventMobGriefing = preventMobGriefing;
    }

    public boolean isPreventBlockModification() {
        return preventBlockModification;
    }

    public void setPreventBlockModification(boolean preventBlockModification) {
        this.preventBlockModification = preventBlockModification;
    }

    // New flag accessors
    public boolean isAllowFlight() {
        return allowFlight;
    }

    public void setAllowFlight(boolean allowFlight) {
        this.allowFlight = allowFlight;
    }

    public boolean isDisableFallDamage() {
        return disableFallDamage;
    }

    public void setDisableFallDamage(boolean disableFallDamage) {
        this.disableFallDamage = disableFallDamage;
    }

    public boolean isPreventHungerLoss() {
        return preventHungerLoss;
    }

    public void setPreventHungerLoss(boolean preventHungerLoss) {
        this.preventHungerLoss = preventHungerLoss;
    }

    public boolean isHealPlayers() {
        return healPlayers;
    }

    public void setHealPlayers(boolean healPlayers) {
        this.healPlayers = healPlayers;
    }

    public String getEntryMessage() {
        return entryMessage;
    }

    public void setEntryMessage(String entryMessage) {
        this.entryMessage = entryMessage;
    }

    public String getExitMessage() {
        return exitMessage;
    }

    public void setExitMessage(String exitMessage) {
        this.exitMessage = exitMessage;
    }

    // Radiation helpers ---------------------------------------------------
    public float getRadiationDamage() {
        return radiationDamage;
    }

    public void setRadiationDamage(float radiationDamage) {
        this.radiationDamage = radiationDamage;
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeUtf(name);
        buf.writeEnum(type);
        buf.writeResourceKey(dimension);
        buf.writeBlockPos(pos1);
        buf.writeBlockPos(pos2);
        buf.writeBoolean(preventPvp);
        buf.writeBoolean(preventExplosions);
        buf.writeBoolean(preventMobGriefing);
        buf.writeBoolean(preventBlockModification);
        buf.writeBoolean(allowFlight);
        buf.writeBoolean(disableFallDamage);
        buf.writeBoolean(preventHungerLoss);
        buf.writeBoolean(healPlayers);
        buf.writeFloat(radiationDamage);
        buf.writeUtf(entryMessage != null ? entryMessage : "");
        buf.writeUtf(exitMessage != null ? exitMessage : "");
    }

    public static ManagedArea read(FriendlyByteBuf buf) {
        String name = buf.readUtf();
        AreaType type = buf.readEnum(AreaType.class);
        ResourceKey<Level> dimension = buf.readResourceKey(Registries.DIMENSION);
        BlockPos pos1 = buf.readBlockPos();
        BlockPos pos2 = buf.readBlockPos();
        ManagedArea area = new ManagedArea(name, type, dimension, pos1, pos2);
        area.setPreventPvp(buf.readBoolean());
        area.setPreventExplosions(buf.readBoolean());
        area.setPreventMobGriefing(buf.readBoolean());
        area.setPreventBlockModification(buf.readBoolean());
        area.setAllowFlight(buf.readBoolean());
        area.setDisableFallDamage(buf.readBoolean());
        area.setPreventHungerLoss(buf.readBoolean());
        area.setHealPlayers(buf.readBoolean());
        area.setRadiationDamage(buf.readFloat());
        area.setEntryMessage(buf.readUtf());
        area.setExitMessage(buf.readUtf());
        return area;
    }
} 