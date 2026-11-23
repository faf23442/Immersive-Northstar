package com.lightning.northstar.client;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.base.IRotate;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.base.RotatingInstance;
import com.simibubi.create.foundation.render.AllInstanceTypes;

import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import dev.engine_room.flywheel.lib.model.Models;
import dev.engine_room.flywheel.lib.visual.AbstractBlockEntityVisual;
import dev.engine_room.flywheel.lib.visual.SimpleDynamicVisual;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;

/** Flywheel 1.0 replacement for Northstar's eight legacy block entity instances. */
public class NorthstarMachineVisual<T extends BlockEntity> extends AbstractBlockEntityVisual<T> implements SimpleDynamicVisual {
    private final Map<Direction, RotatingInstance> shafts = new EnumMap<>(Direction.class);
    private final List<NorthstarMachineModels.Part> parts;
    private final List<TransformedInstance> instances = new ArrayList<>();

    public NorthstarMachineVisual(VisualizationContext context, T be, float partialTick) {
        super(context, be, partialTick);
        if (be instanceof KineticBlockEntity kinetic) {
            IRotate block = (IRotate) blockState.getBlock();
            var instancer = instancerProvider().instancer(AllInstanceTypes.ROTATING, Models.partial(AllPartialModels.SHAFT_HALF));
            for (Direction direction : Direction.values()) {
                if (block.hasShaftTowards(level, pos, blockState, direction)) {
                    var shaft = instancer.createInstance();
                    shaft.rotateToFace(Direction.SOUTH, direction).setup(kinetic)
                        .setPosition(getVisualPosition()).setChanged();
                    shafts.put(direction, shaft);
                }
            }
        }
        parts = NorthstarMachineModels.parts(be);
        for (var part : parts) {
            instances.add(instancerProvider().instancer(InstanceTypes.TRANSFORMED, Models.partial(part.model())).createInstance());
        }
        transformParts(partialTick);
        updateLight(partialTick);
    }

    @Override
    public void beginFrame(Context context) {
        transformParts(context.partialTick());
    }

    private void transformParts(float partialTick) {
        PoseStack ms = new PoseStack();
        ms.translate(visualPos.getX(), visualPos.getY(), visualPos.getZ());
        for (int i = 0; i < parts.size(); i++) {
            ms.pushPose();
            parts.get(i).transform().accept(ms, partialTick);
            instances.get(i).setTransform(ms).setChanged();
            ms.popPose();
        }
    }

    @Override
    public void update(float partialTick) {
        if (blockEntity instanceof KineticBlockEntity kinetic) {
            shafts.values().forEach(shaft -> shaft.setup(kinetic).setChanged());
        }
        transformParts(partialTick);
    }

    @Override
    public void updateLight(float partialTick) {
        shafts.values().forEach(shaft -> relight(shaft));
        instances.forEach(instance -> relight(instance));
    }

    @Override
    public void collectCrumblingInstances(Consumer<Instance> consumer) {
        shafts.values().forEach(consumer);
        instances.forEach(consumer);
    }

    @Override
    protected void _delete() {
        shafts.values().forEach(Instance::delete);
        instances.forEach(Instance::delete);
    }
}
