package com.lightning.northstar.contraptions;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.apache.commons.lang3.tuple.Pair;

import com.lightning.northstar.NorthstarTags;
import com.lightning.northstar.block.NorthstarBlocks;
import com.lightning.northstar.block.NorthstarTechBlocks;
import com.lightning.northstar.block.tech.computer_rack.TargetingComputerRackBlockEntity;
import com.lightning.northstar.block.tech.jet_engine.JetEngineBlock;
import com.lightning.northstar.block.tech.oxygen_generator.OxygenGeneratorBlockEntity;
import com.lightning.northstar.block.tech.rocket_station.RocketStationBlockEntity;
import com.lightning.northstar.block.tech.temperature_regulator.TemperatureRegulatorBlockEntity;
import com.lightning.northstar.item.NorthstarItems;
import com.lightning.northstar.world.dimension.NorthstarPlanets;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.contraptions.AssemblyException;
import com.simibubi.create.api.contraption.ContraptionType;
import com.simibubi.create.content.contraptions.MountedStorageManager;
import com.simibubi.create.content.contraptions.TranslatingContraption;
import com.simibubi.create.content.contraptions.minecart.TrainCargoManager;
import com.simibubi.create.content.fluids.tank.CreativeFluidTankBlockEntity;
import com.simibubi.create.content.fluids.tank.FluidTankBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;
import net.minecraftforge.fluids.IFluidTank;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import net.minecraftforge.fluids.capability.templates.FluidTank;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.ItemStackHandler;

public class RocketContraption extends TranslatingContraption{

	public int fuelCost = 0;
	public int fuelReturnCost = 0;
	public int weightCost = 0;
	public int heatShielding = 0;
	public int blockCount = 0;
	private boolean rocket_station = false;
	public boolean hasControls = false;
	public boolean hasInterplanetaryNavigation = false;
	private boolean has_fuel = false;
	public boolean isUsingTicket;
	private int fuelAmount = 0;
	private int jet_engines = 0;
	private int visual_jet_engines = 0;
	public float computingPower = 0;
	private List<BlockPos> assembledJets;
	public int fuelTicks;
	public String name = "Rocket";
	public Player owner;
	public BlockPos localControlsPos;

	protected MountedStorageManager storageProxy;

	public ResourceKey<Level> dest = null;

	static final IItemHandlerModifiable fallbackItems = new ItemStackHandler();
	static final IFluidHandler fallbackFluids = new FluidTank(0);
	
		
	public float prevYaw;
	public float yaw;
	public float targetYaw;	
	
	public RocketContraption() {
		assembledJets = new ArrayList<>();
		storage = new TrainCargoManager();
	}

	@Override
	public boolean assemble(Level world, BlockPos pos) throws AssemblyException {
		if (!searchMovedStructure(world, pos, null))
			return false;
		System.out.print(blocks.size());
		startMoving(world);
		
		return true;
	}
	public int requiredFuel() {
		return Math.max(0, (int) (weightCost + fuelCost * (1 - computingPower)));
	}

	public void burnFuel() {
		tryBurnFuel();
	}

	/** Plan the complete withdrawal before touching cargo; water and oxygen are never fuel. */
	public boolean tryBurnFuel() {
		if (hasCreativeFuel()) return true;
		IFluidHandler tanks = storage.getFluids();
		long remaining = requiredFuel();
		List<FluidStack> withdrawals = new ArrayList<>();
		for (int slot = 0; slot < tanks.getTanks() && remaining > 0; slot++) {
			FluidStack fluid = tanks.getFluidInTank(slot);
			int tier = fuelValue(fluid);
			if (tier == 0 || withdrawals.stream().anyMatch(fluid::isFluidEqual)) continue;
			FluidStack request = fluid.copy();
			request.setAmount((int) ((remaining + tier - 1) / tier));
			FluidStack available = tanks.drain(request, FluidAction.SIMULATE);
			if (available.isEmpty()) continue;
			withdrawals.add(available);
			remaining -= (long) available.getAmount() * tier;
		}
		if (remaining > 0) return false;
		for (FluidStack withdrawal : withdrawals) tanks.drain(withdrawal, FluidAction.EXECUTE);
		refreshFuel();
		return true;
	}

	private static int fuelValue(FluidStack fluid) {
		if (fluid.isEmpty()) return 0;
		if (NorthstarTags.NorthstarFluidTags.TIER_3_ROCKET_FUEL.matches(fluid.getFluid())) return 4;
		if (NorthstarTags.NorthstarFluidTags.TIER_2_ROCKET_FUEL.matches(fluid.getFluid())) return 2;
		return NorthstarTags.NorthstarFluidTags.TIER_1_ROCKET_FUEL.matches(fluid.getFluid()) ? 1 : 0;
	}

	private void refreshFuel() {
		if (hasCreativeFuel()) {
			fuelAmount = Integer.MAX_VALUE;
			has_fuel = true;
			return;
		}
		long amount = 0;
		IFluidHandler tanks = storage.getFluids();
		for (int slot = 0; slot < tanks.getTanks(); slot++) {
			FluidStack fluid = tanks.getFluidInTank(slot);
			amount += (long) fluid.getAmount() * fuelValue(fluid);
		}
		fuelAmount = (int) Math.min(Integer.MAX_VALUE, amount);
		has_fuel = fuelAmount > 0;
	}

	private boolean hasCreativeFuel() {
		return storage.getFluids().storages.values().stream().anyMatch(tank ->
			tank instanceof com.simibubi.create.content.fluids.tank.storage.creative.CreativeFluidTankMountedStorage
				&& fuelValue(tank.getFluidInTank(0)) > 0);
	}

	@Override
	public CompoundTag writeNBT(boolean spawnPacket) {
		CompoundTag tag = super.writeNBT(spawnPacket);
		CompoundTag rocket = new CompoundTag();
		rocket.putString("Name", name);
		rocket.putInt("FuelCost", fuelCost);
		rocket.putInt("ReturnCost", fuelReturnCost);
		rocket.putFloat("ComputingPower", computingPower);
		rocket.putBoolean("UsingTicket", isUsingTicket);
		rocket.putInt("FuelTicks", fuelTicks);
		if (dest != null) rocket.putString("Destination", dest.location().toString());
		tag.put("NorthstarRocket", rocket);
		return tag;
	}

	@Override
	public void readNBT(Level world, CompoundTag tag, boolean spawnData) {
		super.readNBT(world, tag, spawnData);
		migrateLegacyItems(spawnData);
		// Old Northstar saves only contain Create's structure and cargo, so derive these again.
		rocket_station = hasControls = hasInterplanetaryNavigation = false;
		jet_engines = visual_jet_engines = blockCount = weightCost = heatShielding = 0;
		computingPower = 0;
		localControlsPos = null;
		assembledJets.clear();
		for (StructureBlockInfo info : blocks.values()) {
			BlockState state = info.state();
			if (!state.isAir()) {
				blockCount++;
				weightCost += state.is(NorthstarTags.NorthstarBlockTags.HEAVY_BLOCKS.tag) ? 5
					: state.is(NorthstarTags.NorthstarBlockTags.SUPER_HEAVY_BLOCKS.tag) ? 10 : 1;
			}
			if (state.is(NorthstarTags.NorthstarBlockTags.TIER_1_HEAT_RESISTANCE.tag)) heatShielding += 3;
			if (state.is(NorthstarTags.NorthstarBlockTags.TIER_2_HEAT_RESISTANCE.tag)) heatShielding += 8;
			if (state.is(NorthstarTags.NorthstarBlockTags.TIER_3_HEAT_RESISTANCE.tag)) heatShielding += 20;
			if (NorthstarTechBlocks.ROCKET_STATION.has(state)) rocket_station = true;
			if (state.is(NorthstarBlocks.INTERPLANETARY_NAVIGATOR.get())) hasInterplanetaryNavigation = true;
			if (NorthstarTechBlocks.ROCKET_CONTROLS.has(state)) {
				hasControls = true;
				if (localControlsPos == null) localControlsPos = info.pos();
			}
			if (NorthstarTechBlocks.JET_ENGINE.has(state)) {
				jet_engines++;
				if (state.getValue(JetEngineBlock.BOTTOM)) visual_jet_engines++;
				assembledJets.add(info.pos());
			}
			if (NorthstarTechBlocks.COMPUTER_RACK.has(state) && info.nbt() != null) {
				for (int slot = 1; slot <= 6; slot++) {
					if (ItemStack.of(info.nbt().getCompound("slot" + slot)).is(NorthstarItems.TARGETING_COMPUTER.get()))
						computingPower = Math.min(.4f, computingPower + .0025f);
				}
			}
		}
		refreshFuel();
		if (tag.contains("NorthstarRocket")) {
			CompoundTag rocket = tag.getCompound("NorthstarRocket");
			name = rocket.getString("Name");
			fuelCost = rocket.getInt("FuelCost");
			fuelReturnCost = rocket.getInt("ReturnCost");
			computingPower = rocket.getFloat("ComputingPower");
			isUsingTicket = rocket.getBoolean("UsingTicket");
			fuelTicks = rocket.getInt("FuelTicks");
			dest = readDimension(rocket.getString("Destination"));
		}
	}

	private void migrateLegacyItems(boolean spawnData) {
		if (storage.getAllItemStorages().values().stream().noneMatch(item ->
			item instanceof com.simibubi.create.impl.contraption.storage.FallbackMountedStorage)) return;
		// Create's legacy reader uses FALLBACK even for chests. Its type check would
		// otherwise skip unmounting these inventories when the rocket lands.
		CompoundTag cargo = new CompoundTag();
		storage.write(cargo, false);
		boolean changed = false;
		for (Tag entry : cargo.getList("items", Tag.TAG_COMPOUND)) {
			CompoundTag item = (CompoundTag) entry;
			BlockPos pos = NbtUtils.readBlockPos(item.getCompound("pos"));
			var mounted = storage.getAllItemStorages().get(pos);
			StructureBlockInfo block = blocks.get(pos);
			if (!(mounted instanceof com.simibubi.create.impl.contraption.storage.FallbackMountedStorage) || block == null) continue;
			var expected = com.simibubi.create.api.contraption.storage.item.MountedItemStorageType.REGISTRY.get(block.state().getBlock());
			com.simibubi.create.api.contraption.storage.item.MountedItemStorage replacement;
			if (expected == com.simibubi.create.AllMountedStorageTypes.CHEST.get())
				replacement = new com.simibubi.create.api.contraption.storage.item.chest.ChestMountedStorage(mounted);
			else if (expected == com.simibubi.create.AllMountedStorageTypes.SIMPLE.get())
				replacement = new com.simibubi.create.api.contraption.storage.item.simple.SimpleMountedStorage(mounted);
			else continue;
			var encoded = com.simibubi.create.api.contraption.storage.item.MountedItemStorage.CODEC
				.encodeStart(NbtOps.INSTANCE, replacement).result();
			if (encoded.isPresent()) {
				item.put("storage", encoded.get());
				changed = true;
			}
		}
		if (changed) storage.read(cargo, spawnData, this);
	}

	static ResourceKey<Level> readDimension(String value) {
		ResourceKey<Level> legacy = NorthstarPlanets.getPlanetDimension(value);
		if (legacy != null) return legacy;
		ResourceLocation id = ResourceLocation.tryParse(value);
		return id == null || value.isEmpty() ? null : ResourceKey.create(Registries.DIMENSION, id);
	}

	public static int flightCost(ResourceKey<Level> from, ResourceKey<Level> to) {
		String home = NorthstarPlanets.getPlanetName(from);
		String target = NorthstarPlanets.getPlanetName(to);
		int dx = (int) NorthstarPlanets.getPlanetX(home) - (int) NorthstarPlanets.getPlanetX(target);
		int dy = (int) NorthstarPlanets.getPlanetY(home) - (int) NorthstarPlanets.getPlanetY(target);
		int distance = Mth.roundToward(dx * dx + dy * dy, 100) / 20;
		return (distance + NorthstarPlanets.getPlanetAtmosphereCost(from) + 1000) * 8;
	}
	
	@Override
	protected Pair<StructureBlockInfo, BlockEntity> capture(Level world, BlockPos pos) {
		BlockState blockState = world.getBlockState(pos);
		
		if (NorthstarTechBlocks.ROCKET_STATION.has(blockState)) {
			rocket_station = true;
			BlockEntity ent = world.getBlockEntity(pos);
			if(ent instanceof RocketStationBlockEntity rsbe) {
				name = rsbe.name;
				
				// this is a bit sketchy in game, it should delete the ticket after it's
				// actually been used, not when assembling the rocket
				// though I can't figure that out so this may have to do
				
				if(rsbe.container.getItem(0).is(NorthstarItems.RETURN_TICKET.get())) {
					if(rsbe.container.getItem(0).getTagElement("Planet") != null) {
						if(NorthstarPlanets.getPlanetDimension(NorthstarPlanets.targetGetter(rsbe.container.getItem(0).getTagElement("Planet").toString())) == dest)
						this.isUsingTicket = true;
						this.isUsingTicket = true;
					}
				}
			}
		}
		if (blockState.is(NorthstarBlocks.INTERPLANETARY_NAVIGATOR.get())) {
			this.hasInterplanetaryNavigation = true;
		}
		if(NorthstarTechBlocks.COMPUTER_RACK.has(blockState)) {
			BlockEntity ent = world.getBlockEntity(pos);
			if(ent instanceof TargetingComputerRackBlockEntity crbe) {
				for(int b = 0; b < crbe.container.getContainerSize(); b++){
					if(crbe.container.getItem(b).is(NorthstarItems.TARGETING_COMPUTER.get())) {
						if(computingPower < 0.4)
							computingPower += 0.0025;
					}
				}
			}
		}
		
		if (NorthstarTechBlocks.ROCKET_CONTROLS.has(blockState)) {
			hasControls = true;
			if(this.localControlsPos == null) {
				this.localControlsPos = this.toLocalPos(pos);
			}
		}
		if (NorthstarTechBlocks.JET_ENGINE.has(blockState)) {
			jet_engines += 1;
			if(blockState.getValue(JetEngineBlock.BOTTOM)) {
				visual_jet_engines++;
			}
			assembledJets.add(toLocalPos(pos));
		}
		if (AllBlocks.FLUID_TANK.has(blockState)) {
			FluidTankBlockEntity tank = (FluidTankBlockEntity) world.getBlockEntity(pos);
			IFluidTank fluid = tank.getTankInventory();
			if (NorthstarTags.NorthstarFluidTags.TIER_1_ROCKET_FUEL.matches(fluid.getFluid().getFluid())) {
				if(fluid.getFluidAmount() != 0)
				fuelAmount += fluid.getFluidAmount();
				has_fuel = true;
			}
			if (NorthstarTags.NorthstarFluidTags.TIER_2_ROCKET_FUEL.matches(fluid.getFluid().getFluid())) {
				if(fluid.getFluidAmount() != 0)
				fuelAmount += fluid.getFluidAmount() * 2;
				has_fuel = true;
			}
			if (NorthstarTags.NorthstarFluidTags.TIER_3_ROCKET_FUEL.matches(fluid.getFluid().getFluid())) {
				if(fluid.getFluidAmount() != 0)
				fuelAmount += fluid.getFluidAmount() * 4;
				has_fuel = true;
			}
		}
		
		if(blockState.is(AllBlocks.CREATIVE_FLUID_TANK.get())) {
			CreativeFluidTankBlockEntity tank = (CreativeFluidTankBlockEntity) world.getBlockEntity(pos);
			IFluidTank fluid = tank.getTankInventory();
			if (NorthstarTags.NorthstarFluidTags.TIER_1_ROCKET_FUEL.matches(fluid.getFluid().getFluid())) {
				fuelAmount = Integer.MAX_VALUE;
				has_fuel = true;
			}
			if (NorthstarTags.NorthstarFluidTags.TIER_2_ROCKET_FUEL.matches(fluid.getFluid().getFluid())) {
				fuelAmount = Integer.MAX_VALUE;
				has_fuel = true;
			}
			if (NorthstarTags.NorthstarFluidTags.TIER_3_ROCKET_FUEL.matches(fluid.getFluid().getFluid())) {
				fuelAmount = Integer.MAX_VALUE;
				has_fuel = true;
			}
		}
		if(!blockState.is(Blocks.AIR) && !blockState.is(Blocks.CAVE_AIR)) {
			blockCount++;
		}
		if(blockState.is(NorthstarTechBlocks.OXYGEN_GENERATOR.get())) {
			OxygenGeneratorBlockEntity ent = (OxygenGeneratorBlockEntity) world.getBlockEntity(pos);
			ent.removeOxy(ent, new HashSet<>());
		}else if(blockState.is(NorthstarTechBlocks.TEMPERATURE_REGULATOR.get())) {
			TemperatureRegulatorBlockEntity ent2 = (TemperatureRegulatorBlockEntity) world.getBlockEntity(pos);
			ent2.removeTemp(ent2);
		}
		
		if(blockState.is(NorthstarTags.NorthstarBlockTags.HEAVY_BLOCKS.tag) && !blockState.is(Blocks.AIR)) 
		{weightCost += 5;}else 
		if(blockState.is(NorthstarTags.NorthstarBlockTags.SUPER_HEAVY_BLOCKS.tag) && !blockState.is(Blocks.AIR))
		{weightCost += 10;} 
		else if(!blockState.is(Blocks.AIR)){weightCost+=1;}
		if(blockState.is(NorthstarTags.NorthstarBlockTags.TIER_1_HEAT_RESISTANCE.tag) && !blockState.is(Blocks.AIR)) 
		{heatShielding += 3;}
		if(blockState.is(NorthstarTags.NorthstarBlockTags.TIER_2_HEAT_RESISTANCE.tag) && !blockState.is(Blocks.AIR)) 
		{heatShielding += 8;}
		if(blockState.is(NorthstarTags.NorthstarBlockTags.TIER_3_HEAT_RESISTANCE.tag) && !blockState.is(Blocks.AIR))
		{heatShielding += 20;}
		return super.capture(world, pos);
	}
	
	public void changeDim(ResourceKey<Level> targetDim, RocketContraptionEntity entity) {
        entity.destination = targetDim;
        RocketHandler.transferRocket(entity);
    }

	@Override
	public boolean canBeStabilized(Direction facing, BlockPos localPos) {
		return false;
	}

	@Override
	public ContraptionType getType() {
		return NorthstarContraptionTypes.ROCKET.get();
	}
	
	public boolean hasRocketStation() {
		return rocket_station;
	}

	public int hasJetEngine() {
		return jet_engines;
	}
	public int getVisualJetEngines() {
		return visual_jet_engines;
	}
	public boolean hasFuel() {
		return has_fuel;
	}
	public int fuelAmount() {
		return fuelAmount;
	}
	public int heatShielding() {
		return heatShielding;
	}

	
	@Override
	protected boolean isAnchoringBlockAt(BlockPos pos) {
		return super.isAnchoringBlockAt(pos.relative(Direction.DOWN));
	}

}
