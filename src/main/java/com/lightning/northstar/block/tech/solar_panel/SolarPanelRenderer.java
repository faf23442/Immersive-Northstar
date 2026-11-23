package com.lightning.northstar.block.tech.solar_panel;

import com.lightning.northstar.client.NorthstarMachineRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;

public class SolarPanelRenderer extends NorthstarMachineRenderer<SolarPanelBlockEntity> {
    public SolarPanelRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }
}
