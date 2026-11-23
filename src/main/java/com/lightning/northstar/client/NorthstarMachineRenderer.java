package com.lightning.northstar.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.base.IRotate;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;

import dev.engine_room.flywheel.api.visualization.VisualizationManager;
import net.createmod.catnip.render.CachedBuffers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;

public class NorthstarMachineRenderer<T extends KineticBlockEntity> extends KineticBlockEntityRenderer<T> {
    public NorthstarMachineRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    protected void renderSafe(T be, float partialTick, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
        if (VisualizationManager.supportsVisualization(be.getLevel()))
            return;
        renderShafts(be, ms, buffer, light);
        renderParts(be, partialTick, ms, buffer, light);
    }

    public static void renderShafts(KineticBlockEntity be, PoseStack ms, MultiBufferSource buffer, int light) {
        IRotate block = (IRotate) be.getBlockState().getBlock();
        for (Direction direction : Direction.values()) {
            if (!block.hasShaftTowards(be.getLevel(), be.getBlockPos(), be.getBlockState(), direction))
                continue;
            var shaft = CachedBuffers.partialFacing(AllPartialModels.SHAFT_HALF, be.getBlockState(), direction);
            renderRotatingBuffer(be, shaft, ms, buffer.getBuffer(RenderType.solid()), light);
        }
    }

    public static void renderParts(BlockEntity be, float partialTick, PoseStack ms, MultiBufferSource buffer, int light) {
        for (var part : NorthstarMachineModels.parts(be)) {
            ms.pushPose();
            part.transform().accept(ms, partialTick);
            CachedBuffers.partial(part.model(), be.getBlockState())
                .light(light).renderInto(ms, buffer.getBuffer(RenderType.cutoutMipped()));
            ms.popPose();
        }
    }
}
