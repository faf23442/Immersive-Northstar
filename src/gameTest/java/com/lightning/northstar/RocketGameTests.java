package com.lightning.northstar;

import java.util.List;

import com.lightning.northstar.block.NorthstarTechBlocks;
import com.lightning.northstar.contraptions.RocketContraption;
import com.lightning.northstar.contraptions.RocketContraptionEntity;
import com.lightning.northstar.contraptions.RocketHandler;
import com.lightning.northstar.fluids.NorthstarFluids;
import com.lightning.northstar.world.dimension.NorthstarDimensions;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.contraptions.Contraption;

import net.createmod.catnip.nbt.NBTHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.items.ItemStackHandler;

@GameTestHolder(Northstar.MOD_ID)
@PrefixGameTestTemplate(false)
public class RocketGameTests {
    /** Opt-in scene for inspecting moving controls, engine particles and landing in the dev client. */
    @net.minecraft.gametest.framework.GameTestGenerator
    public static java.util.Collection<net.minecraft.gametest.framework.TestFunction> visualScenes() {
        if (!Boolean.getBoolean("northstar.visualRocketTest")) return List.of();
        return List.of(new net.minecraft.gametest.framework.TestFunction("northstar_visual", "visualrocketflight",
            "northstar:rocket", 2400, 0, true, RocketGameTests::visualRocketFlight));
    }

    private static void visualRocketFlight(GameTestHelper helper) {
        var pilot = helper.getLevel().players().stream().filter(p -> !p.isSpectator()).findFirst().orElseThrow();
        for (int x = 1; x <= 6; x++) for (int z = 1; z <= 6; z++)
            helper.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
        RocketContraptionEntity rocket = rocketEntity(helper);
        var seat = new BlockPos(1, 0, 2);
        rocket.getContraption().getBlocks().put(seat,
            new net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo(
                seat, AllBlocks.SEATS.get(net.minecraft.world.item.DyeColor.WHITE).getDefaultState(), null));
        rocket.getContraption().getSeats().add(seat);
        rocket.localControlsPos = new BlockPos(1, 0, 0);
        rocket.destination = Level.OVERWORLD;
        helper.getLevel().addFreshEntity(rocket);
        pilot.teleportTo(rocket.getX() + 1.5, rocket.getY() + 1, rocket.getZ() + 2.5);
        pilot.setYRot(180);
        pilot.setXRot(25);
        rocket.addSittingPassenger(pilot, 0);
        rocket.setControllingPlayer(pilot.getUUID());
        NorthstarPackets.getChannel().send(net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> pilot),
            new com.lightning.northstar.contraptions.RocketControlPacket(pilot.getUUID(), rocket.getId(), rocket.localControlsPos));
        double startY = rocket.getY();
        helper.runAfterDelay(100, () -> rocket.control(rocket.localControlsPos, List.of(4), pilot));
        helper.onEachTick(() -> {
            if (rocket.getY() > startY + 6 && !rocket.landing) rocket.startLanding();
            if (rocket.landing && !rocket.isRemoved()) rocket.control(rocket.localControlsPos, List.of(4), pilot);
        });
        helper.succeedWhen(() -> helper.assertTrue(rocket.isRemoved(), "Visual flight must complete its landing"));
    }

    @GameTest(template = "rocket")
    public static void legacyRocketRestoresStructureAndCargo(GameTestHelper helper) {
        RocketContraption rocket = legacyRocket(helper, false);
        helper.assertTrue(rocket.getBlocks().size() == 6, "Legacy block-list format must retain every block");
        helper.assertTrue(rocket.hasJetEngine() == 1 && rocket.getVisualJetEngines() == 1,
            "Legacy rocket must recover its engines from saved blocks");
        helper.assertTrue(rocket.hasControls && rocket.localControlsPos.equals(new BlockPos(1, 0, 0)),
            "Legacy rocket must recover the controls position");
        helper.assertTrue(rocket.hasRocketStation() && rocket.hasFuel() && rocket.fuelAmount() == 12000,
            "Legacy rocket must recover station and fuel information");
        helper.assertTrue(rocket.getStorage().getAllItems().getStackInSlot(0).is(Items.DIAMOND)
            && rocket.getStorage().getAllItems().getStackInSlot(0).getCount() == 7,
            "Create 0.5 item cargo must survive migration");
        helper.assertTrue(fluidAmount(rocket, Fluids.WATER) == 6000
            && fluidAmount(rocket, NorthstarFluids.METHANE.getSource()) == 6000,
            "Create 0.5 tanks must retain fluid identity and volume");
        helper.succeed();
    }

    @GameTest(template = "rocket")
    public static void rocketMetadataSurvivesReload(GameTestHelper helper) {
        RocketContraption rocket = legacyRocket(helper, false);
        rocket.name = "Saved rocket";
        rocket.fuelCost = 1000;
        rocket.fuelReturnCost = 1500;
        rocket.computingPower = .25f;
        rocket.isUsingTicket = true;
        rocket.dest = NorthstarDimensions.MOON_DIM_KEY;
        RocketContraption restored = (RocketContraption) Contraption.fromNBT(helper.getLevel(), rocket.writeNBT(false), false);
        helper.assertTrue(restored.name.equals("Saved rocket"), "Reload must preserve the rocket name");
        helper.assertTrue(restored.fuelCost == 1000 && restored.fuelReturnCost == 1500,
            "Reload must preserve flight costs");
        helper.assertTrue(restored.computingPower == .25f && restored.isUsingTicket
            && restored.dest == NorthstarDimensions.MOON_DIM_KEY, "Reload must preserve navigation and ticket data");
        helper.succeed();
    }

    @GameTest(template = "rocket")
    public static void rocketThrustSurvivesReload(GameTestHelper helper) {
        RocketContraptionEntity rocket = rocketEntity(helper);
        rocket.blasting = true;
        rocket.lift_vel = 1.25f;
        RocketContraptionEntity restored = reload(helper, rocket);
        helper.assertTrue(restored.blasting && restored.lift_vel == 1.25f,
            "A rocket saved during ascent must retain thrust");
        helper.succeed();
    }

    @GameTest(template = "rocket")
    public static void rocketCountdownSurvivesReload(GameTestHelper helper) {
        RocketContraptionEntity rocket = rocketEntity(helper);
        var pilot = helper.makeMockPlayer();
        pilot.setPos(rocket.getX() + 1, rocket.getY() + 1, rocket.getZ());
        helper.assertTrue(rocket.control(new BlockPos(1, 0, 0), List.of(4), pilot), "Nearby pilot must be able to launch");
        helper.assertTrue(rocket.launchtime == 200, "Launch must start the countdown");
        RocketContraptionEntity restored = reload(helper, rocket);
        helper.assertTrue(restored.launchtime == 200, "Reload must retain the remaining countdown");
        restored.tick();
        helper.assertTrue(restored.launchtime == 199 && !restored.blasting, "Reloaded countdown must resume before thrust starts");
        helper.succeed();
    }

    @GameTest(template = "rocket")
    public static void rocketFuelDoesNotConsumeWater(GameTestHelper helper) {
        for (boolean swap : List.of(false, true)) {
            RocketContraption rocket = legacyRocket(helper, swap);
            rocket.fuelCost = 1000;
            rocket.weightCost = 0;
            rocket.owner = helper.makeMockPlayer();
            rocket.burnFuel();
            helper.assertTrue(fluidAmount(rocket, Fluids.WATER) == 6000, "Fuel burning must not drain water from another tank");
            helper.assertTrue(fluidAmount(rocket, NorthstarFluids.METHANE.getSource()) == 5500,
                "Fuel burning must drain the matching fuel tank");
        }
        helper.succeed();
    }

    @GameTest(template = "rocket")
    public static void rocketFuelWithdrawalIsAtomic(GameTestHelper helper) {
        RocketContraption rocket = legacyRocket(helper, false);
        rocket.weightCost = 0;
        rocket.fuelCost = 12001;
        helper.assertTrue(!rocket.tryBurnFuel(), "Insufficient fuel must reject the entire withdrawal");
        helper.assertTrue(fluidAmount(rocket, NorthstarFluids.METHANE.getSource()) == 6000,
            "Rejected withdrawal must leave cargo intact");
        rocket.fuelCost = 16000;
        rocket.computingPower = .25f;
        helper.assertTrue(rocket.tryBurnFuel(), "Exact fuel supply must work with the owner offline");
        helper.assertTrue(rocket.fuelCost == 16000 && rocket.fuelAmount() == 0,
            "Computing discount must not mutate the base cost and drained supply must update");
        helper.succeed();
    }

    @GameTest(template = "rocket")
    public static void rocketBurnsFuelAcrossTanks(GameTestHelper helper) {
        for (boolean mixed : List.of(false, true)) {
            CompoundTag saved = legacyRocket(helper, false).writeNBT(false);
            saved.remove("fluids");
            ListTag tanks = new ListTag();
            fluid(tanks, new BlockPos(0, 0, 1), new FluidStack(NorthstarFluids.METHANE.getSource(), 500));
            fluid(tanks, new BlockPos(1, 0, 1), new FluidStack(mixed ? NorthstarFluids.HYDROCARBON.getSource()
                : NorthstarFluids.METHANE.getSource(), 500));
            saved.put("FluidStorage", tanks);
            RocketContraption rocket = (RocketContraption) Contraption.fromNBT(helper.getLevel(), saved, false);
            rocket.weightCost = 0;
            rocket.fuelCost = mixed ? 1200 : 2000;
            helper.assertTrue(rocket.tryBurnFuel(), "Flight must use matching fuel across multiple tanks");
            helper.assertTrue(mixed ? rocket.fuelAmount() >= 299 && rocket.fuelAmount() <= 300 : rocket.fuelAmount() == 0,
                "Mixed fuel tiers and split tanks must retain the correct balance");
        }
        helper.succeed();
    }

    @GameTest(template = "rocket")
    public static void rocketOwnerAndOrbitSurviveRepeatedReload(GameTestHelper helper) {
        RocketContraptionEntity rocket = rocketEntity(helper);
        rocket.home = NorthstarDimensions.EARTH_ORBIT_DIM_KEY;
        rocket.destination = Level.NETHER;
        rocket.ownerID = java.util.UUID.randomUUID();
        RocketContraptionEntity restored = reload(helper, reload(helper, rocket));
        helper.assertTrue(rocket.ownerID.equals(restored.ownerID), "Offline owner UUID must survive repeated saves");
        helper.assertTrue(restored.home == NorthstarDimensions.EARTH_ORBIT_DIM_KEY && restored.destination == Level.NETHER,
            "Dimension IDs must retain orbit and dimensions outside the planet aliases");
        helper.succeed();
    }

    @GameTest(template = "rocket")
    public static void rocketLaunchesAfterCountdown(GameTestHelper helper) {
        RocketContraptionEntity rocket = rocketEntity(helper);
        var pilot = helper.makeMockPlayer();
        pilot.setPos(rocket.getX() + 1, rocket.getY() + 1, rocket.getZ());
        rocket.control(new BlockPos(1, 0, 0), List.of(4), pilot);
        double start = rocket.getY();
        for (int tick = 0; tick < 199; tick++) tickRocket(rocket);
        helper.assertTrue(!rocket.blasting && rocket.getY() == start, "Countdown must hold the rocket stationary");
        for (int tick = 0; tick < 21; tick++) tickRocket(rocket);
        helper.assertTrue(rocket.blasting && rocket.getY() > start, "Rocket must rise after the countdown");
        helper.succeed();
    }

    @GameTest(template = "rocket")
    public static void rocketLandingRestoresBlocksAndCargo(GameTestHelper helper) {
        for (int x = 1; x <= 6; x++) for (int z = 1; z <= 6; z++)
            helper.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
        RocketContraptionEntity rocket = rocketEntity(helper);
        rocket.startLanding();
        for (int tick = 0; tick < 200 && !rocket.isRemoved(); tick++) {
            var pilot = helper.makeMockPlayer();
            pilot.setPos(rocket.getX() + 1, rocket.getY() + 1, rocket.getZ());
            rocket.control(new BlockPos(1, 0, 0), List.of(4), pilot);
            tickRocket(rocket);
        }
        helper.assertTrue(rocket.isRemoved(), "Slow landing must disassemble the rocket");
        BlockPos anchor = BlockPos.containing(rocket.position().add(.5, .5, .5));
        helper.assertTrue(NorthstarTechBlocks.ROCKET_STATION.has(helper.getLevel().getBlockState(anchor)),
            "Landing must restore the station at the landing position");
        var chest = (net.minecraft.world.level.block.entity.ChestBlockEntity)
            helper.getLevel().getBlockEntity(anchor.offset(2, 0, 1));
        helper.assertTrue(chest != null && chest.getItem(0).is(Items.DIAMOND) && chest.getItem(0).getCount() == 7,
            "Landing must restore item cargo");
        var tank = (com.simibubi.create.content.fluids.tank.FluidTankBlockEntity)
            helper.getLevel().getBlockEntity(anchor.offset(1, 0, 1));
        helper.assertTrue(tank != null && tank.getTankInventory().getFluid().getFluid().isSame(NorthstarFluids.METHANE.getSource())
            && tank.getTankInventory().getFluidAmount() == 6000, "Landing must restore fuel cargo");
        helper.succeed();
    }

    @GameTest(template = "rocket")
    public static void rocketTransfersPilotControls(GameTestHelper helper) {
        var server = helper.getLevel().getServer();
        var pilot = new net.minecraft.server.level.ServerPlayer(server, helper.getLevel(),
            new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "rocket-test-pilot"));
        var connection = new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND);
        var channel = new io.netty.channel.embedded.EmbeddedChannel(connection);
        server.getPlayerList().placeNewPlayer(connection, pilot);
        RocketContraptionEntity rocket = rocketEntity(helper);
        RocketContraptionEntity arrived = null;
        try {
            rocket.getContraption().getSeats().add(new BlockPos(1, 1, 1));
            rocket.localControlsPos = new BlockPos(1, 0, 0);
            rocket.owner = pilot;
            pilot.setPos(rocket.getX() + 1, rocket.getY() + 1, rocket.getZ() + 1);
            helper.getLevel().addFreshEntity(rocket);
            rocket.addSittingPassenger(pilot, 0);
            rocket.setControllingPlayer(pilot.getUUID());
            arrived = RocketHandler.transferRocket(rocket);
            helper.assertTrue(arrived != null && pilot.level() == arrived.level() && pilot.getVehicle() == arrived,
                "Pilot must arrive in the same dimension and seat as the rocket");
            helper.assertTrue(arrived.getControllingPlayer().orElseThrow().equals(pilot.getUUID()) && arrived.owner == pilot,
                "Transfer must restore pilot controls and resolve the owner");
        } finally {
            pilot.stopRiding();
            helper.getLevel().getServer().getPlayerList().remove(pilot);
            pilot.discard();
            channel.finishAndReleaseAll();
            if (arrived != null) arrived.discard();
            if (!rocket.isRemoved()) rocket.discard();
        }
        helper.succeed();
    }

    @GameTest(template = "rocket")
    public static void rocketTransfersPassengersAndCargo(GameTestHelper helper) {
        RocketContraptionEntity rocket = rocketEntity(helper);
        rocket.getContraption().getSeats().add(new BlockPos(1, 1, 1));
        Entity passenger = EntityType.PIG.create(helper.getLevel());
        passenger.setPos(rocket.getX() + 1, rocket.getY() + 1, rocket.getZ() + 1);
        helper.getLevel().addFreshEntity(passenger);
        helper.getLevel().addFreshEntity(rocket);
        rocket.addSittingPassenger(passenger, 0);
        RocketContraptionEntity arrived = null;
        Entity moved = null;
        try {
            helper.assertTrue(RocketHandler.ROCKETS.contains(rocket), "Joining the world must register loaded rockets");
            arrived = RocketHandler.transferRocket(rocket);
            helper.assertTrue(arrived != null && rocket.isRemoved(), "Transfer must replace the source entity");
            helper.assertTrue(arrived.level().dimension() == NorthstarDimensions.MOON_DIM_KEY
                && arrived.position().equals(rocket.position()) && arrived.landing && arrived.final_lift_vel == 0,
                "Transfer must retain position and begin landing in the destination");
            moved = arrived.getPassengers().stream().filter(p -> p.getUUID().equals(passenger.getUUID())).findFirst().orElse(null);
            helper.assertTrue(moved != null && moved.getVehicle() == arrived
                && arrived.getContraption().getSeatMapping().get(moved.getUUID()) == 0,
                "Seated passenger must retain its seat across dimensions");
            helper.assertTrue(arrived.getContraption().hasJetEngine() == 1
                && fluidAmount(arrived.getContraption(), NorthstarFluids.METHANE.getSource()) == 6000
                && arrived.getContraption().getStorage().getAllItems().getStackInSlot(0).getCount() == 7,
                "Transfer must retain rocket structure and cargo");
            helper.assertTrue(RocketHandler.ROCKETS.contains(arrived) && !RocketHandler.ROCKETS.contains(rocket),
                "Tracking must follow the replacement rocket");
        } finally {
            if (moved != null) moved.discard();
            if (arrived != null) arrived.discard();
            if (!passenger.isRemoved()) passenger.discard();
            if (!rocket.isRemoved()) rocket.discard();
        }
        helper.succeed();
    }

    @GameTest(template = "rocket", timeoutTicks = 400)
    public static void loadedRocketTransfersAtFlightHeight(GameTestHelper helper) {
        RocketContraptionEntity rocket = reload(helper, rocketEntity(helper));
        rocket.setPos(rocket.getX(), 1751, rocket.getZ());
        helper.getLevel().addFreshEntity(rocket);
        // Entity section loading is asynchronous, even after the destination block chunk is ready.
        helper.runAfterDelay(399, () -> RocketHandler.ROCKETS.stream()
            .filter(r -> r.getUUID().equals(rocket.getUUID())).toList().forEach(Entity::discard));
        helper.succeedWhen(() -> {
            var destination = helper.getLevel().getServer().getLevel(NorthstarDimensions.MOON_DIM_KEY);
            Entity arrived = destination.getEntity(rocket.getUUID());
            helper.assertTrue(rocket.isRemoved() && arrived instanceof RocketContraptionEntity,
                "A loaded rocket must become visible in the destination entity lookup");
            helper.assertTrue(((RocketContraptionEntity) arrived).landing,
                "Transferred rocket must remain in its landing phase");
            arrived.discard();
        });
    }

    @GameTest(template = "rocket")
    public static void unavailableDestinationRetainsRocket(GameTestHelper helper) {
        RocketContraptionEntity rocket = rocketEntity(helper);
        rocket.destination = net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION,
            new net.minecraft.resources.ResourceLocation("northstar", "missing_test_dimension"));
        helper.assertTrue(RocketHandler.transferRocket(rocket) == null && !rocket.isRemoved() && !rocket.landing,
            "An unavailable destination must leave the source rocket intact");
        helper.succeed();
    }

    static RocketContraptionEntity rocketEntity(GameTestHelper helper) {
        RocketContraptionEntity rocket = RocketContraptionEntity.create(helper.getLevel(), legacyRocket(helper, false));
        BlockPos pos = helper.absolutePos(new BlockPos(2, 4, 2));
        rocket.setPos(pos.getX(), pos.getY(), pos.getZ());
        rocket.home = Level.OVERWORLD;
        rocket.destination = NorthstarDimensions.MOON_DIM_KEY;
        return rocket;
    }

    private static void tickRocket(RocketContraptionEntity rocket) {
        // Level normally advances this counter before Entity.tick(). These tests tick an unspawned entity.
        rocket.tickCount++;
        rocket.tick();
    }

    static RocketContraptionEntity reload(GameTestHelper helper, RocketContraptionEntity original) {
        CompoundTag saved = original.saveWithoutId(new CompoundTag());
        RocketContraptionEntity restored = rocketEntity(helper);
        restored.load(saved);
        return restored;
    }

    /** Create 0.5's block list and mounted-storage schema, without Northstar-specific metadata. */
    static RocketContraption legacyRocket(GameTestHelper helper, boolean swapFluids) {
        CompoundTag saved = new CompoundTag();
        saved.putString("Type", "northstar:rocket");
        saved.put("Anchor", NbtUtils.writeBlockPos(helper.absolutePos(new BlockPos(2, 4, 2))));
        saved.put("BoundsFront", NBTHelper.writeAABB(new AABB(0, -1, 0, 3, 2, 3)));
        ListTag blocks = new ListTag();
        block(blocks, BlockPos.ZERO, NorthstarTechBlocks.ROCKET_STATION.getDefaultState());
        block(blocks, new BlockPos(0, -1, 0), NorthstarTechBlocks.JET_ENGINE.getDefaultState());
        block(blocks, new BlockPos(1, 0, 0), NorthstarTechBlocks.ROCKET_CONTROLS.getDefaultState());
        block(blocks, new BlockPos(0, 0, 1), AllBlocks.FLUID_TANK.getDefaultState());
        block(blocks, new BlockPos(1, 0, 1), AllBlocks.FLUID_TANK.getDefaultState());
        block(blocks, new BlockPos(2, 0, 1), Blocks.CHEST.defaultBlockState());
        saved.put("Blocks", blocks);
        ListTag actors = new ListTag();
        for (BlockPos pos : List.of(new BlockPos(0, -1, 0), new BlockPos(1, 0, 0))) {
            CompoundTag actor = new CompoundTag();
            actor.put("Pos", NbtUtils.writeBlockPos(pos));
            actors.add(actor);
        }
        saved.put("Actors", actors);
        ListTag fluids = new ListTag();
        fluid(fluids, new BlockPos(0, 0, 1), new FluidStack(swapFluids ? NorthstarFluids.METHANE.getSource() : Fluids.WATER, 6000));
        fluid(fluids, new BlockPos(1, 0, 1), new FluidStack(swapFluids ? Fluids.WATER : NorthstarFluids.METHANE.getSource(), 6000));
        saved.put("FluidStorage", fluids);
        ItemStackHandler inventory = new ItemStackHandler(27);
        inventory.setStackInSlot(0, new ItemStack(Items.DIAMOND, 7));
        CompoundTag chest = new CompoundTag();
        chest.put("Pos", NbtUtils.writeBlockPos(new BlockPos(2, 0, 1)));
        chest.put("Data", inventory.serializeNBT());
        ListTag items = new ListTag();
        items.add(chest);
        saved.put("Storage", items);
        return (RocketContraption) Contraption.fromNBT(helper.getLevel(), saved, false);
    }

    private static void block(ListTag blocks, BlockPos pos, BlockState state) {
        CompoundTag block = new CompoundTag();
        block.put("Pos", NbtUtils.writeBlockPos(pos));
        block.put("Block", NbtUtils.writeBlockState(state));
        blocks.add(block);
    }

    private static void fluid(ListTag fluids, BlockPos pos, FluidStack stack) {
        CompoundTag entry = new CompoundTag();
        entry.put("Pos", NbtUtils.writeBlockPos(pos));
        CompoundTag data = stack.writeToNBT(new CompoundTag());
        data.putInt("Capacity", 8000);
        entry.put("Data", data);
        fluids.add(entry);
    }

    private static int fluidAmount(RocketContraption rocket, net.minecraft.world.level.material.Fluid fluid) {
        int amount = 0;
        var tanks = rocket.getStorage().getFluids();
        for (int i = 0; i < tanks.getTanks(); i++) {
            FluidStack stack = tanks.getFluidInTank(i);
            if (stack.getFluid().isSame(fluid)) amount += stack.getAmount();
        }
        return amount;
    }
}
