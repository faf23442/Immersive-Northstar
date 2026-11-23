package com.lightning.northstar.block;

import com.simibubi.create.api.stress.BlockStressValues;
import com.tterrag.registrate.util.nullness.NonNullConsumer;

import net.minecraft.world.level.block.Block;

/** Stress registration for non-Create blocks (CStress only accepts the "create" namespace in Create 6). */
public class NorthstarStress {
	public static <B extends Block> NonNullConsumer<? super B> impact(double value) {
		return b -> BlockStressValues.IMPACTS.register(b, () -> value);
	}

	public static <B extends Block> NonNullConsumer<? super B> capacity(double value) {
		return b -> BlockStressValues.CAPACITIES.register(b, () -> value);
	}

	public static <B extends Block> NonNullConsumer<? super B> noImpact() {
		return b -> BlockStressValues.IMPACTS.register(b, () -> 0);
	}
}
