package com.lightning.northstar.contraptions;

import com.lightning.northstar.Northstar;
import com.simibubi.create.AllContraptionTypes;
import com.simibubi.create.api.contraption.ContraptionType;
import com.simibubi.create.api.registry.CreateRegistries;

import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class NorthstarContraptionTypes {
	private static final DeferredRegister<ContraptionType> CONTRAPTION_TYPES =
		DeferredRegister.create(CreateRegistries.CONTRAPTION_TYPE, Northstar.MOD_ID);

	public static final RegistryObject<ContraptionType> ROCKET =
		CONTRAPTION_TYPES.register("rocket", () -> new ContraptionType(RocketContraption::new));

	public static void register(IEventBus modEventBus) {
		CONTRAPTION_TYPES.register(modEventBus);
	}

	/** Called after registries are populated so that old saves using the legacy name still load. */
	public static void registerLegacyNames() {
		AllContraptionTypes.BY_LEGACY_NAME.put("northstar:rocket", ROCKET.get());
	}
}
