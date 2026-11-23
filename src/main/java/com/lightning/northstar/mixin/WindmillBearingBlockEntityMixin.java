package com.lightning.northstar.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.lightning.northstar.world.dimension.NorthstarPlanets;
import com.simibubi.create.content.contraptions.bearing.MechanicalBearingBlockEntity;
import com.simibubi.create.content.contraptions.bearing.WindmillBearingBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

@Mixin(value = WindmillBearingBlockEntity.class, remap = false)
public abstract class WindmillBearingBlockEntityMixin extends MechanicalBearingBlockEntity {

	public WindmillBearingBlockEntityMixin(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}
	@Inject(method = "getGeneratedSpeed", at = @At("RETURN"), cancellable = true)
	private void northstar$applyWindMultiplier(CallbackInfoReturnable<Float> info) {
		// The cached speed already includes the planet multiplier. Only scale a
		// fresh calculation, leaving Create's sail count and direction logic intact.
		if (level != null && running && movedContraption != null) {
			info.setReturnValue(info.getReturnValueF() * NorthstarPlanets.getWindMultiplier(level));
		}
	}
	
}
