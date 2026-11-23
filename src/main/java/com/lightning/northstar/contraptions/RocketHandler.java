package com.lightning.northstar.contraptions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.lightning.northstar.Northstar;
import com.lightning.northstar.NorthstarPackets;
import com.lightning.northstar.block.tech.rocket_station.RocketStationBlockEntity;
import com.mojang.datafixers.util.Pair;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.portal.PortalInfo;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import net.minecraftforge.network.PacketDistributor;

@EventBusSubscriber(modid = Northstar.MOD_ID, bus = Bus.FORGE)
public class RocketHandler {
    private static final TicketType<Integer> ROCKET_TICKET = TicketType.create("northstar_rocket", Integer::compareTo, 40);
    private static final Map<RocketContraptionEntity, ChunkPos> LOADED_CHUNKS = new HashMap<>();
	public static List<RocketContraptionEntity> ROCKETS = new java.util.concurrent.CopyOnWriteArrayList<>();
	public static List<Pair<Level, BlockPos>> TICKET_QUEUE = new ArrayList<>();
	public static HashMap<Pair<UUID, BlockPos>, Integer> CONTROL_QUEUE = new HashMap<Pair<UUID, BlockPos>, Integer>();
	public static long eventTickNumber;
	public static long eventTickNumberCheck;

	@SubscribeEvent
    public static void onWorldTick(TickEvent.LevelTickEvent event){
		if (event.phase != TickEvent.Phase.END) return;
		eventTickNumber = event.level.getGameTime();


        if (event.level.isClientSide) {
            var pending = CONTROL_QUEUE.entrySet().iterator();
            while (pending.hasNext()) {
                var entry = pending.next();
                Player player = event.level.getPlayerByUUID(entry.getKey().getFirst());
                if (player != null && event.level.getEntity(entry.getValue()) instanceof RocketContraptionEntity rocket) {
                    rocket.setControllingPlayer(player.getUUID());
                    net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(net.minecraftforge.api.distmarker.Dist.CLIENT,
                        () -> () -> com.lightning.northstar.block.tech.rocket_controls.RocketControlsHandler
                            .startControlling(rocket, entry.getKey().getSecond()));
                    pending.remove();
                }
            }
        }
		if(event.level.isClientSide)
			return;

        for (RocketContraptionEntity rocket : List.copyOf(ROCKETS)) {
            if (rocket.isRemoved()) {
                ROCKETS.remove(rocket);
                continue;
            }
            if (rocket.level() == event.level) keepLoaded(rocket);
            if (rocket.level() == event.level && rocket.destination != null
                && rocket.level().dimension() != rocket.destination && rocket.getY() > 1750)
                transferRocket(rocket);
        }

		if(eventTickNumber > eventTickNumberCheck) {
			//deleting contents of the queue
			if(!TICKET_QUEUE.isEmpty()) {
				List<Pair<Level, BlockPos>> DELETE_QUEUE = new ArrayList<>();
				for(Pair<Level,BlockPos> entries : TICKET_QUEUE) {
					if(event.level.dimension() == entries.getFirst().dimension()) {
						if(entries.getFirst().getBlockEntity(entries.getSecond()) instanceof RocketStationBlockEntity rsbe) {
							rsbe.container.setItem(0, new ItemStack(Blocks.AIR.asItem()));
							DELETE_QUEUE.add(entries);
						}
					}
				}
				for(Pair<Level,BlockPos> entries : DELETE_QUEUE) {
					TICKET_QUEUE.remove(entries);
				}
			}
		}

    }

	public static void changeDim(RocketContraptionEntity entity, Level level) {
        transferRocket(entity);
    }

    /** Use Forge's normal entity/player transfer so packets, hooks and lifecycle stay intact. */
    public static RocketContraptionEntity transferRocket(RocketContraptionEntity rocket) {
        if (rocket == null || rocket.isRemoved() || !(rocket.level() instanceof ServerLevel source)
            || rocket.destination == null || source.dimension() == rocket.destination) return null;
        ServerLevel destination = source.getServer().getLevel(rocket.destination);
        if (destination == null) return null;

        // A rocket can arrive without a player. Keep its destination entity-ticking while in flight.
        ChunkPos targetChunk = new ChunkPos(rocket.blockPosition());
        destination.getChunkSource().addRegionTicket(ROCKET_TICKET, targetChunk, 2, rocket.getId());
        destination.getChunk(targetChunk.x, targetChunk.z);

        UUID controller = rocket.getControllingPlayer().orElse(null);
        Map<UUID, Integer> seats = new HashMap<>(rocket.getContraption().getSeatMapping());
        java.util.Set<Entity> passengers = new java.util.LinkedHashSet<>(rocket.getPassengers());
        passengers.addAll(rocket.entitiesInContraption);
        passengers.addAll(rocket.collidingEntities.keySet());
        passengers.remove(rocket);
        Map<Entity, net.minecraft.world.phys.Vec3> positions = new HashMap<>();
        for (Entity passenger : passengers) positions.put(passenger, passenger.position());

        Entity transferred = rocket.changeDimension(destination, atPosition(rocket.position()));
        destination.getChunkSource().removeRegionTicket(ROCKET_TICKET, targetChunk, 2, rocket.getId());
        if (!(transferred instanceof RocketContraptionEntity arrived)) return null;
        keepLoaded(arrived);
        arrived.startLanding();
        arrived.getContraption().getSeatMapping().clear();
        arrived.entitiesInContraption.clear();
        for (Entity passenger : passengers) {
            if (passenger.isRemoved() || passenger.level() != source) continue;
            Entity moved = passenger.changeDimension(destination, atPosition(positions.get(passenger)));
            if (moved == null) continue;
            Integer seat = seats.get(passenger.getUUID());
            if (seat != null && seat >= 0 && seat < arrived.getContraption().getSeats().size())
                arrived.addSittingPassenger(moved, seat);
            else arrived.registerColliding(moved);
            arrived.entitiesInContraption.add(moved);
            if (moved.getUUID().equals(arrived.ownerID) && moved instanceof Player player) arrived.owner = player;
            if (moved.getUUID().equals(controller) && moved instanceof ServerPlayer player) {
                arrived.setControllingPlayer(controller);
                if (arrived.localControlsPos != null)
                    NorthstarPackets.getChannel().send(PacketDistributor.PLAYER.with(() -> player),
                        new RocketControlPacket(controller, arrived.getId(), arrived.localControlsPos));
            }
        }
        return arrived;
    }

    private static net.minecraftforge.common.util.ITeleporter atPosition(net.minecraft.world.phys.Vec3 position) {
        return new net.minecraftforge.common.util.ITeleporter() {
            @Override
            public PortalInfo getPortalInfo(Entity entity, ServerLevel destination,
                    java.util.function.Function<ServerLevel, PortalInfo> vanilla) {
                return new PortalInfo(position, entity.getDeltaMovement(), entity.getYRot(), entity.getXRot());
            }

            @Override
            public Entity placeEntity(Entity entity, ServerLevel source, ServerLevel destination, float yaw,
                    java.util.function.Function<Boolean, Entity> reposition) {
                return reposition.apply(false);
            }

            @Override
            public boolean playTeleportSound(ServerPlayer player, ServerLevel source, ServerLevel destination) {
                return false;
            }
        };
    }

    @SubscribeEvent
    public static void onEntityJoin(net.minecraftforge.event.entity.EntityJoinLevelEvent event) {
        if (event.getEntity() instanceof RocketContraptionEntity rocket && !ROCKETS.contains(rocket)) {
            ROCKETS.add(rocket);
            keepLoaded(rocket);
        }
    }

    @SubscribeEvent
    public static void onEntityLeave(net.minecraftforge.event.entity.EntityLeaveLevelEvent event) {
        if (event.getEntity() instanceof RocketContraptionEntity rocket) {
            ROCKETS.remove(rocket);
            ChunkPos chunk = LOADED_CHUNKS.remove(rocket);
            if (chunk != null && rocket.level() instanceof ServerLevel level)
                level.getChunkSource().removeRegionTicket(ROCKET_TICKET, chunk, 2, rocket.getId());
        }
    }

    private static void keepLoaded(RocketContraptionEntity rocket) {
        if (!(rocket.level() instanceof ServerLevel level)) return;
        ChunkPos chunk = new ChunkPos(rocket.blockPosition());
        ChunkPos previous = LOADED_CHUNKS.put(rocket, chunk);
        if (previous != null && !previous.equals(chunk))
            level.getChunkSource().removeRegionTicket(ROCKET_TICKET, previous, 2, rocket.getId());
        level.getChunkSource().addRegionTicket(ROCKET_TICKET, chunk, 2, rocket.getId());
    }

    @SubscribeEvent
    public static void onServerStopped(net.minecraftforge.event.server.ServerStoppedEvent event) {
        ROCKETS.clear();
        TICKET_QUEUE.clear();
        CONTROL_QUEUE.clear();
        LOADED_CHUNKS.clear();
    }

	public static void deleteTicket(Level level, BlockPos pos) {
		TICKET_QUEUE.add(Pair.of(level, pos.above()));
		eventTickNumberCheck = eventTickNumber + 3;
	}


	public static boolean isInRocket(Entity entity) {
		for(RocketContraptionEntity rockets : ROCKETS) {
			if(rockets.entitiesInContraption.contains(entity)) {
				return true;
			}
		}
		return false;
	}

	public static void register() {
		System.out.println("Handling rockets for " + Northstar.MOD_ID);
	}

}
