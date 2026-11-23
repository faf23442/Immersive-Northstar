package com.lightning.northstar;

import java.util.List;

import com.lightning.northstar.block.NorthstarTechBlocks;
import com.lightning.northstar.contraptions.NorthstarContraptionTypes;
import com.lightning.northstar.contraptions.RocketContraption;
import com.lightning.northstar.fluids.NorthstarFluids;
import com.lightning.northstar.world.dimension.NorthstarDimensions;
import com.simibubi.create.AllBlockEntityTypes;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllContraptionTypes;
import com.simibubi.create.AllDisplaySources;
import com.simibubi.create.api.behaviour.display.DisplaySource;
import com.simibubi.create.api.contraption.ContraptionType;
import com.simibubi.create.api.registry.CreateBuiltInRegistries;
import com.simibubi.create.api.stress.BlockStressValues;
import com.simibubi.create.content.contraptions.bearing.WindmillBearingBlockEntity;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.fluids.VirtualFluid;
import com.simibubi.create.content.fluids.tank.FluidTankBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;

@GameTestHolder(Northstar.MOD_ID)
@PrefixGameTestTemplate(false)
public class PortGameTests {
    @GameTest(template = "empty")
    public static void resourceDataLoads(GameTestHelper helper) {
        for (String tree : List.of("argyre", "coiler", "wilter")) {
            var id = new ResourceLocation("northstar", "blocks/" + tree + "_leaves");
            helper.assertTrue(helper.getLevel().getServer().getLootData().getLootTable(id) != LootTable.EMPTY,
                "Leaf loot table must load: " + id);
        }
        var ores = TagKey.create(Registries.ITEM, new ResourceLocation("northstar", "diamond_ores"));
        for (String planet : List.of("mars", "moon", "venus", "mercury")) {
            var ore = ForgeRegistries.ITEMS.getValue(new ResourceLocation("northstar", planet + "_diamond_ore"));
            helper.assertTrue(ore != null && ore.getDefaultInstance().is(ores), "Diamond ore tag must include " + planet);
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void virtualFluidVariants(GameTestHelper helper) {
        for (VirtualFluid fluid : List.of(NorthstarFluids.OXYGEN.get(), NorthstarFluids.HYDROGEN.get(),
            NorthstarFluids.CHOCOLATE_ICE_CREAM.get(), NorthstarFluids.VANILLA_ICE_CREAM.get(),
            NorthstarFluids.STRAWBERRY_ICE_CREAM.get())) {
            helper.assertTrue(fluid.getSource() != fluid.getFlowing(), "Source and flowing variants must differ");
            helper.assertTrue(fluid.getSource().defaultFluidState().isSource(), "Source variant must be a source");
            helper.assertTrue(!fluid.getFlowing().defaultFluidState().isSource(), "Flowing variant must not be a source");
            helper.assertTrue(fluid.isSame(fluid.getSource()) && fluid.isSame(fluid.getFlowing()),
                "Both variants must belong to the same fluid");
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void rocketTypeCompatibility(GameTestHelper helper) {
        helper.assertTrue(CreateBuiltInRegistries.CONTRAPTION_TYPE.get(new ResourceLocation("northstar", "rocket"))
            == NorthstarContraptionTypes.ROCKET.get(), "Rocket type must be registered under its saved ID");
        helper.assertTrue(AllContraptionTypes.BY_LEGACY_NAME.get("northstar:rocket")
            == NorthstarContraptionTypes.ROCKET.get(), "Legacy rocket alias must resolve to the registered type");
        helper.assertTrue(ContraptionType.fromType("northstar:rocket") instanceof RocketContraption,
            "Old rocket type names must create a RocketContraption");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void createRegistrations(GameTestHelper helper) {
        List<DisplaySource> sources = DisplaySource.BY_BLOCK.get(NorthstarTechBlocks.ROCKET_STATION.get());
        helper.assertTrue(sources != null && sources.contains(AllDisplaySources.STATION_SUMMARY.get())
            && sources.contains(AllDisplaySources.TRAIN_STATUS.get()), "Rocket station display sources must be registered");
        helper.assertTrue(BlockStressValues.getCapacity(NorthstarTechBlocks.SOLAR_PANEL.get()) > 0,
            "Solar panels must register stress capacity through Create's API");
        helper.assertTrue(BlockStressValues.getCapacity(NorthstarTechBlocks.COMBUSTION_ENGINE.get()) > 0,
            "Combustion engines must register stress capacity through Create's API");
        helper.assertTrue(BlockStressValues.RPM.get(NorthstarTechBlocks.COMBUSTION_ENGINE.get()).value() == 128,
            "Combustion engine tooltips must show the actual maximum RPM");
        helper.succeed();
    }

    @GameTest(template = "machines", timeoutTicks = 100)
    public static void machineBlockEntitiesLoad(GameTestHelper helper) {
        List<BlockState> states = List.of(NorthstarTechBlocks.SOLAR_PANEL.getDefaultState(),
            NorthstarTechBlocks.COMBUSTION_ENGINE.getDefaultState(), NorthstarTechBlocks.CIRCUIT_ENGRAVER.getDefaultState(),
            NorthstarTechBlocks.ELECTROLYSIS_MACHINE.getDefaultState(), NorthstarTechBlocks.OXYGEN_CONCENTRATOR.getDefaultState(),
            NorthstarTechBlocks.OXYGEN_GENERATOR.getDefaultState(), NorthstarTechBlocks.TEMPERATURE_REGULATOR.getDefaultState(),
            NorthstarTechBlocks.ROCKET_CONTROLS.getDefaultState(), NorthstarTechBlocks.IRON_COGWHEEL.getDefaultState(),
            NorthstarTechBlocks.JET_ENGINE.getDefaultState());
        for (int i = 0; i < states.size(); i++) {
            BlockPos pos = new BlockPos(1 + i * 2, 1, 1);
            helper.setBlock(pos.below(), Blocks.STONE);
            helper.setBlock(pos, states.get(i));
            helper.assertTrue(helper.getBlockEntity(pos) != null, "Machine block entity must load: " + states.get(i));
            if (helper.getBlockEntity(pos) instanceof KineticBlockEntity kinetic) {
                kinetic.setSpeed(32);
                kinetic.sendData();
            }
        }
        helper.runAfterDelay(20, helper::succeed);
    }

    @GameTest(template = "empty")
    public static void fluidTankAcceptsOxygen(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, AllBlocks.FLUID_TANK.getDefaultState());
        FluidTankBlockEntity tank = (FluidTankBlockEntity) helper.getBlockEntity(pos);
        helper.assertTrue(tank != null, "Create fluid tank block entity must load");
        int filled = tank.getTankInventory().fill(new FluidStack(NorthstarFluids.OXYGEN.getSource(), 1000), FluidAction.EXECUTE);
        helper.assertTrue(filled == 1000, "Create fluid tanks must accept Northstar oxygen");
        helper.assertTrue(tank.getTankInventory().getFluid().getFluid().isSame(NorthstarFluids.OXYGEN.get()),
            "Tank must retain the oxygen fluid identity");
        tank.getBlockState().getLightEmission(helper.getLevel(), helper.absolutePos(pos));
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void windmillRetainsCachedPlanetSpeed(GameTestHelper helper) {
        ServerLevel venus = helper.getLevel().getServer().getLevel(NorthstarDimensions.VENUS_DIM_KEY);
        helper.assertTrue(venus != null, "Venus dimension must load");
        WindmillBearingBlockEntity bearing = new WindmillBearingBlockEntity(AllBlockEntityTypes.WINDMILL_BEARING.get(),
            BlockPos.ZERO, AllBlocks.WINDMILL_BEARING.getDefaultState());
        bearing.setLevel(venus);
        CompoundTag saved = new CompoundTag();
        saved.putBoolean("Running", true);
        saved.putFloat("LastGenerated", 8);
        bearing.load(saved);
        helper.assertTrue(bearing.getGeneratedSpeed() == 8, "Cached speed must not receive the planet multiplier again");
        helper.assertTrue(bearing.getGeneratedSpeed() == 8, "Repeated queries must retain the cached speed");
        saved.putBoolean("Running", false);
        bearing.load(saved);
        helper.assertTrue(bearing.getGeneratedSpeed() == 0, "Stopped windmills must generate no rotation");
        helper.succeed();
    }
}
