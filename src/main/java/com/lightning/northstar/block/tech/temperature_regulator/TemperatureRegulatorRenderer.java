package com.lightning.northstar.block.tech.temperature_regulator;

import com.lightning.northstar.client.NorthstarMachineRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;

public class TemperatureRegulatorRenderer extends NorthstarMachineRenderer<TemperatureRegulatorBlockEntity> {
    public TemperatureRegulatorRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }
}
