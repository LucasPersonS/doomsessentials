package org.lupz.doomsdayessentials.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lupz.doomsdayessentials.config.EscavadeiraConfig;
import org.lupz.doomsdayessentials.menu.EscavadeiraMenu;
import org.lupz.doomsdayessentials.registry.EscavadeiraRegistries;
import net.minecraft.network.chat.Component;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.util.GeckoLibUtil;
import software.bernie.geckolib.core.animation.AnimatableManager;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class EscavadeiraControllerBlockEntity extends BlockEntity implements MenuProvider, GeoAnimatable {
	public enum Profile { MIXED, STONE, METAL, COAL, PRECIOUS; public static Profile next(Profile p){ int i=p.ordinal(); return values()[(i+1)%values().length]; } public static Profile prev(Profile p){ int i=p.ordinal(); return values()[(i-1+values().length)%values().length]; } }

	private static final int FUEL_SLOT_COUNT = 1;
	private static final int OUTPUT_SLOT_COUNT = 27;

	private final ItemStackHandler fuel = new ItemStackHandler(FUEL_SLOT_COUNT) {
		@Override
		protected void onContentsChanged(int slot) { setChanged(); }

		@Override
		public boolean isItemValid(int slot, @NotNull ItemStack stack) {
			return isFuelItem(stack);
		}
	};
	private final ItemStackHandler output = new ItemStackHandler(OUTPUT_SLOT_COUNT) {
		@Override
		protected void onContentsChanged(int slot) { setChanged(); }
	};
	private final LazyOptional<IItemHandler> fuelCap = LazyOptional.of(() -> fuel);
	private final LazyOptional<IItemHandler> outputCap = LazyOptional.of(() -> output);

	private boolean running = false;
	private int ticksUntilConsume = 0;
	private int ticksUntilProduce = 0;
	private int ticksUntilSound = 0;
	private int ticksUntilAggro = 0;
	private int warmupRemaining = 0;
	private int fuelBurnCredits = 0; // how many future consumption cycles are covered
	private Profile profile = Profile.MIXED;
	private final Random random = new Random();

	private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);

	public EscavadeiraControllerBlockEntity(BlockPos pos, BlockState state) {
		super(EscavadeiraRegistries.ESCAVADEIRA_CONTROLLER_BE, pos, state);
		resetTimers();
	}

	private void resetTimers() {
		ticksUntilConsume = getFuelIntervalTicks();
		ticksUntilProduce = EscavadeiraConfig.productionIntervalTicks;
		ticksUntilSound = Math.max(20, EscavadeiraConfig.soundIntervalTicks);
		ticksUntilAggro = 200;
		warmupRemaining = 60; // 3s warmup at 20tps
	}

	public static void serverTick(Level level, BlockPos pos, BlockState state, EscavadeiraControllerBlockEntity be) {
		if (!level.isClientSide) {
			be.tickServer((ServerLevel) level);
		}
	}

	private void tickServer(ServerLevel level) {
		// simple redstone auto-start
		if (!running && hasAnyFuelInSlot() && level.hasNeighborSignal(worldPosition)) {
			setRunning(true);
		}

		if (running) {
			if (warmupRemaining > 0) {
				warmupRemaining--;
			}
			if (--ticksUntilConsume <= 0) {
				consumeFuelIfNeeded();
				ticksUntilConsume = getFuelIntervalTicks();
			}
			if (warmupRemaining == 0 && --ticksUntilProduce <= 0) {
				produceOnce();
				ticksUntilProduce = EscavadeiraConfig.productionIntervalTicks;
			}
			if (--ticksUntilSound <= 0) {
				level.playSound(null, worldPosition, SoundEvents.ANVIL_PLACE, net.minecraft.sounds.SoundSource.BLOCKS, 1.2F, 0.8F + random.nextFloat() * 0.4F);
				spawnParticles(level);
				ticksUntilSound = Math.max(20, EscavadeiraConfig.soundIntervalTicks);
			}
			if (EscavadeiraConfig.attractHostileMobs && --ticksUntilAggro <= 0) {
				pullAggro(level);
				ticksUntilAggro = 200;
			}
		}
	}

	private static boolean isFuelItem(ItemStack s) {
		return s.is(Items.COAL) || s.is(Items.CHARCOAL) || s.is(Items.COAL_BLOCK) || s.is(Items.BLAZE_ROD);
	}

	private boolean hasAnyFuelInSlot() {
		ItemStack s = fuel.getStackInSlot(0);
		return !s.isEmpty() && isFuelItem(s);
	}

	private void consumeFuelIfNeeded() {
		if (fuelBurnCredits > 0) {
			fuelBurnCredits--;
			return;
		}
		ItemStack s = fuel.getStackInSlot(0);
		if (s.isEmpty()) { running = false; setChanged(); return; }
		// consume one item and grant credits depending on fuel type
		fuel.extractItem(0, 1, false);
		if (s.is(Items.COAL_BLOCK)) fuelBurnCredits += 8; // total of 9 cycles for a block
		else if (s.is(Items.BLAZE_ROD)) fuelBurnCredits += 4; // ~5 cycles
	}

	private void produceOnce() {
		EscavadeiraConfig.ResourceEntry entry = pickProfileResource();
		if (entry == null) return;
		Item item = net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(entry.itemId);
		if (item == null) return;
		int amount = entry.min + random.nextInt(entry.max - entry.min + 1);
		ItemStack stack = new ItemStack(item, amount);
		ItemStack remaining = insertIntoOutput(stack);
		if (!remaining.isEmpty() && level instanceof ServerLevel sl) {
			// try pushing to neighbors first
			remaining = pushToNeighbors(remaining);
			if (!remaining.isEmpty()) {
				net.minecraft.world.entity.item.ItemEntity drop = new net.minecraft.world.entity.item.ItemEntity(sl, worldPosition.getX() + 0.5, worldPosition.getY() + 1.0, worldPosition.getZ() + 0.5, remaining);
				sl.addFreshEntity(drop);
			}
		}
		setChanged();
	}

	private ItemStack pushToNeighbors(ItemStack stack) {
		if (stack.isEmpty()) return stack;
		for (Direction d : Direction.values()) {
			BlockPos np = worldPosition.relative(d);
			BlockEntity be = level.getBlockEntity(np);
			if (be == null) continue;
			LazyOptional<IItemHandler> cap = be.getCapability(ForgeCapabilities.ITEM_HANDLER, d.getOpposite());
			if (!cap.isPresent()) continue;
			IItemHandler handler = cap.orElse(null);
			if (handler == null) continue;
			stack = ItemHandlerHelper.insertItem(handler, stack, false);
			if (stack.isEmpty()) return ItemStack.EMPTY;
		}
		return stack;
	}

	private ItemStack insertIntoOutput(ItemStack stack) {
		for (int i = 0; i < output.getSlots(); i++) {
			stack = output.insertItem(i, stack, false);
			if (stack.isEmpty()) return ItemStack.EMPTY;
		}
		return stack;
	}

	private void spawnParticles(ServerLevel level) {
		for (int i = 0; i < 8; i++) {
			double ox = worldPosition.getX() + 0.5 + (random.nextDouble() - 0.5);
			double oy = worldPosition.getY() + 1.2 + random.nextDouble() * 0.4;
			double oz = worldPosition.getZ() + 0.5 + (random.nextDouble() - 0.5);
			level.sendParticles(net.minecraft.core.particles.ParticleTypes.CLOUD, ox, oy, oz, 1, 0.08, 0.02, 0.08, 0.02);
		}
	}

	private void pullAggro(ServerLevel level) {
		List<Monster> mobs = level.getEntitiesOfClass(Monster.class, new net.minecraft.world.phys.AABB(worldPosition).inflate(32), EntitySelector.LIVING_ENTITY_STILL_ALIVE);
		if (mobs.isEmpty()) return;
		Player nearest = level.getNearestPlayer(worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5, 32, false);
		if (nearest == null) return;
		for (Monster m : mobs) {
			m.setTarget(nearest);
		}
	}

	public void setRunning(boolean running) {
		boolean prev = this.running;
		this.running = running && hasAnyFuelInSlot();
		if (this.running && !prev) {
			resetTimers();
			if (level instanceof ServerLevel sl) {
				sl.playSound(null, worldPosition, SoundEvents.ANVIL_USE, net.minecraft.sounds.SoundSource.BLOCKS, 1.0F, 0.8F);
			}
		}
		setChanged();
	}

	public boolean isRunning() { return running; }
	public Profile getProfile(){ return profile; }
	public void cycleProfile(boolean forward){ profile = forward ? Profile.next(profile) : Profile.prev(profile); setChanged(); }

	@Override
	public @NotNull Component getDisplayName() {
		return Component.literal("Escavadeira");
	}

	@Override
	public @Nullable AbstractContainerMenu createMenu(int windowId, @NotNull Inventory inv, @NotNull Player player) {
		return new EscavadeiraMenu(windowId, inv, this.worldPosition);
	}

	@Override
	public void load(@NotNull CompoundTag tag) {
		super.load(tag);
		this.running = tag.getBoolean("Running");
		this.ticksUntilConsume = tag.getInt("FuelTicks");
		this.ticksUntilProduce = tag.getInt("ProduceTicks");
		this.ticksUntilSound = tag.getInt("SoundTicks");
		this.ticksUntilAggro = tag.getInt("AggroTicks");
		this.warmupRemaining = tag.getInt("Warmup");
		this.fuelBurnCredits = tag.getInt("FuelCredits");
		this.profile = Profile.values()[tag.getInt("Profile")] ;
		if (tag.contains("FuelInv")) fuel.deserializeNBT(tag.getCompound("FuelInv"));
		if (tag.contains("OutInv")) output.deserializeNBT(tag.getCompound("OutInv"));
	}

	@Override
	protected void saveAdditional(@NotNull CompoundTag tag) {
		super.saveAdditional(tag);
		tag.putBoolean("Running", running);
		tag.putInt("FuelTicks", ticksUntilConsume);
		tag.putInt("ProduceTicks", ticksUntilProduce);
		tag.putInt("SoundTicks", ticksUntilSound);
		tag.putInt("AggroTicks", ticksUntilAggro);
		tag.putInt("Warmup", warmupRemaining);
		tag.putInt("FuelCredits", fuelBurnCredits);
		tag.putInt("Profile", profile.ordinal());
		tag.put("FuelInv", fuel.serializeNBT());
		tag.put("OutInv", output.serializeNBT());
	}

	@Override
	public <T> @NotNull LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
		if (cap == ForgeCapabilities.ITEM_HANDLER) {
			if (side == Direction.DOWN) return outputCap.cast();
			if (side == Direction.UP) return fuelCap.cast();
			return outputCap.cast();
		}
		return super.getCapability(cap, side);
	}

	public IItemHandler getFuelHandler() { return fuel; }
	public IItemHandler getOutputHandler() { return output; }

	private EscavadeiraConfig.ResourceEntry pickProfileResource() {
		switch (profile) {
			case STONE: return pickFrom(entriesStone());
			case METAL: return pickFrom(entriesMetal());
			case COAL: return pickFrom(entriesCoal());
			case PRECIOUS: return pickFrom(entriesPrecious());
			default: return EscavadeiraConfig.pickRandomResource();
		}
	}

	private EscavadeiraConfig.ResourceEntry pickFrom(List<EscavadeiraConfig.ResourceEntry> list) {
		if (list.isEmpty()) return null;
		double roll = random.nextDouble();
		double acc = 0;
		for (var e : list) { acc += e.chance; if (roll <= acc) return e; }
		return list.get(list.size() - 1);
	}

	private List<EscavadeiraConfig.ResourceEntry> entriesStone() {
		List<EscavadeiraConfig.ResourceEntry> l = new ArrayList<>();
		l.add(new EscavadeiraConfig.ResourceEntry(new ResourceLocation("minecraft:stone"), 6, 12, 0.7));
		l.add(new EscavadeiraConfig.ResourceEntry(new ResourceLocation("minecraft:cobblestone"), 6, 12, 0.3));
		return l;
	}
	private List<EscavadeiraConfig.ResourceEntry> entriesMetal() {
		List<EscavadeiraConfig.ResourceEntry> l = new ArrayList<>();
		l.add(new EscavadeiraConfig.ResourceEntry(new ResourceLocation("minecraft:iron_ore"), 2, 5, 0.6));
		l.add(new EscavadeiraConfig.ResourceEntry(new ResourceLocation("minecraft:copper_ore"), 3, 6, 0.4));
		return l;
	}
	private List<EscavadeiraConfig.ResourceEntry> entriesCoal() {
		List<EscavadeiraConfig.ResourceEntry> l = new ArrayList<>();
		l.add(new EscavadeiraConfig.ResourceEntry(new ResourceLocation("minecraft:coal"), 4, 8, 1.0));
		return l;
	}
	private List<EscavadeiraConfig.ResourceEntry> entriesPrecious() {
		List<EscavadeiraConfig.ResourceEntry> l = new ArrayList<>();
		l.add(new EscavadeiraConfig.ResourceEntry(new ResourceLocation("minecraft:gold_ore"), 1, 3, 0.6));
		l.add(new EscavadeiraConfig.ResourceEntry(new ResourceLocation("minecraft:raw_gold"), 1, 2, 0.4));
		return l;
	}

	@Override
	public void registerControllers(AnimatableManager.ControllerRegistrar controllers) { }

	@Override
	public AnimatableInstanceCache getAnimatableInstanceCache() {
		return geoCache;
	}

	@Override
	public double getTick(Object blockEntity) {
		return level != null ? level.getGameTime() : 0.0;
	}

	private int getFuelIntervalTicks() {
		int base = EscavadeiraConfig.fuelConsumptionTicks;
		switch (profile) {
			case PRECIOUS:
				return Math.max(5, (int)(base * 0.25)); // ~4x faster consumption
			case METAL:
				return Math.max(5, (int)(base * 0.5)); // ~2x faster consumption
			case STONE:
			case COAL:
				return (int)(base * 2.0); // 2x slower consumption
			default:
				return base; // MIXED
		}
	}
} 