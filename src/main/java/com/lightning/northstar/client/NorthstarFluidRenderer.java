package com.lightning.northstar.client;

import com.mojang.blaze3d.vertex.PoseStack;

import net.createmod.catnip.render.FluidRenderHelper;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraftforge.fluids.FluidStack;

/** Thin compatibility wrapper for the old static Create FluidRenderer.renderFluidBox. */
public class NorthstarFluidRenderer {
	private static final FluidRenderHelper<FluidStack> HELPER = new FluidRenderHelper<>();

	public static void renderFluidBox(FluidStack fluid, float xMin, float yMin, float zMin, float xMax, float yMax,
		float zMax, MultiBufferSource buffer, PoseStack ms, int light, boolean renderBottom) {
		HELPER.renderFluidBox(fluid, xMin, yMin, zMin, xMax, yMax, zMax, buffer, ms, light, renderBottom, false);
	}
}
