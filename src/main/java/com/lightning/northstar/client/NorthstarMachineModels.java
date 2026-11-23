package com.lightning.northstar.client;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

import com.lightning.northstar.block.tech.NorthstarPartialModels;
import com.lightning.northstar.block.tech.circuit_engraver.CircuitEngraverBlockEntity;
import com.lightning.northstar.block.tech.combustion_engine.CombustionEngineBlockEntity;
import com.lightning.northstar.block.tech.oxygen_concentrator.OxygenConcentratorBlockEntity;
import com.lightning.northstar.block.tech.oxygen_generator.OxygenGeneratorBlockEntity;
import com.lightning.northstar.block.tech.rocket_controls.RocketControlsBlock;
import com.lightning.northstar.block.tech.rocket_controls.RocketControlsBlockEntity;
import com.lightning.northstar.block.tech.temperature_regulator.TemperatureRegulatorBlockEntity;
import com.lightning.northstar.world.dimension.NorthstarPlanets;
import com.mojang.blaze3d.vertex.PoseStack;

import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import dev.engine_room.flywheel.lib.transform.TransformStack;
import net.createmod.catnip.animation.AnimationTickHolder;
import net.createmod.catnip.math.AngleHelper;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/** Shared part transforms keep Flywheel and the fallback renderer in sync. */
public final class NorthstarMachineModels {
    public record Part(PartialModel model, BiConsumer<PoseStack, Float> transform) {}

    public static List<Part> parts(BlockEntity be) {
        List<Part> parts = new ArrayList<>();
        if (be instanceof CombustionEngineBlockEntity engine) {
            PartialModel[] pistons = {NorthstarPartialModels.PISTON1, NorthstarPartialModels.PISTON2,
                NorthstarPartialModels.PISTON3, NorthstarPartialModels.PISTON4,
                NorthstarPartialModels.PISTON5, NorthstarPartialModels.PISTON6};
            int[] phases = {0, 2, 4, 8, 10, 12};
            for (int i = 0; i < pistons.length; i++) {
                int phase = phases[i];
                parts.add(new Part(pistons[i], (ms, pt) -> {
                    float time = AnimationTickHolder.getRenderTime(engine.getLevel()) * engine.getSpeed() * .04f;
                    ms.translate(0, Math.sin(time + phase) * .05, 0);
                    rotateY(ms, AngleHelper.horizontalAngle(engine.getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING)));
                }));
            }
        } else if (be instanceof CircuitEngraverBlockEntity engraver) {
            parts.add(new Part(NorthstarPartialModels.CIRCUIT_ENGRAVER_HEAD, (ms, pt) ->
                rotateY(ms, headAngle(engraver, pt) / 2)));
            parts.add(new Part(NorthstarPartialModels.CIRCUIT_ENGRAVER_LASER, (ms, pt) -> {
                if (!engraver.engravingBehaviour.running) {
                    ms.scale(0, 0, 0);
                    return;
                }
                ms.translate(0, -.16, 0);
                rotateY(ms, headAngle(engraver, pt) / 1.5f);
            }));
        } else if (be instanceof OxygenConcentratorBlockEntity concentrator) {
            parts.add(new Part(NorthstarPartialModels.OXYGEN_CONCENTATOR_FAN, (ms, pt) ->
                rotateY(ms, AnimationTickHolder.getRenderTime(be.getLevel()) * concentrator.getSpeed() * .3f / 4)));
        } else if (be instanceof OxygenGeneratorBlockEntity generator) {
            parts.add(new Part(NorthstarPartialModels.OXYGEN_FAN, (ms, pt) ->
                rotateY(ms, AnimationTickHolder.getRenderTime(be.getLevel()) * generator.getSpeed() * .3f)));
        } else if (be instanceof TemperatureRegulatorBlockEntity regulator) {
            parts.add(new Part(NorthstarPartialModels.WARM_SPINNY, (ms, pt) -> regulatorPart(ms, regulator, true)));
            parts.add(new Part(NorthstarPartialModels.COLD_SPINNY, (ms, pt) -> regulatorPart(ms, regulator, false)));
        } else if (be instanceof RocketControlsBlockEntity) {
            parts.add(new Part(NorthstarPartialModels.CONTROL_LEVER_BLOCK, (ms, pt) ->
                rotateY(ms, AngleHelper.horizontalAngle(be.getBlockState().getValue(RocketControlsBlock.FACING)))));
        }
        return parts;
    }

    private static float headAngle(CircuitEngraverBlockEntity be, float partialTick) {
        return AnimationTickHolder.getRenderTime(be.getLevel()) * be.getRenderedHeadRotationSpeed(partialTick) * .3f;
    }

    private static void regulatorPart(PoseStack ms, TemperatureRegulatorBlockEntity be, boolean warm) {
        boolean heating = be.temp >= NorthstarPlanets.getPlanetTemp(be.getLevel().dimension());
        if (heating != warm) {
            ms.scale(0, 0, 0);
            return;
        }
        rotateY(ms, AnimationTickHolder.getRenderTime(be.getLevel()) * be.getSpeed() * .3f / 2);
    }

    private static void rotateY(PoseStack ms, float angle) {
        TransformStack.of(ms).center().rotateYDegrees(angle).uncenter();
    }

    private NorthstarMachineModels() {}
}
