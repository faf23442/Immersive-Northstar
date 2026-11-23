package com.lightning.northstar.block.tech.combustion_engine;

import com.lightning.northstar.client.NorthstarMachineRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;

public class CombustionEngineRenderer extends NorthstarMachineRenderer<CombustionEngineBlockEntity> {
    public CombustionEngineRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }
}
