package io.github.stuff_stuffs.tbcexv4.common.internal;

import com.mojang.datafixers.util.Unit;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.stuff_stuffs.event_gen.api.event.EventKey;
import io.github.stuff_stuffs.tbcexv4.common.api.Tbcexv4Api;
import io.github.stuff_stuffs.tbcexv4.common.api.Tbcexv4Registries;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.Battle;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.BattleParticipantEventInitEvent;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.damage.DamageType;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.damage.DamageTypeGraph;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.state.BattleStateEventInitEvent;
import io.github.stuff_stuffs.tbcexv4.common.api.event.EventMap;
import io.github.stuff_stuffs.tbcexv4.common.generated_events.BasicEvents;
import io.github.stuff_stuffs.tbcexv4.common.generated_events.env.BasicEnvEvents;
import io.github.stuff_stuffs.tbcexv4.common.generated_events.participant.BasicParticipantEvents;
import io.github.stuff_stuffs.tbcexv4.common.generated_events.participant.ParticipantInventoryEvents;
import io.github.stuff_stuffs.tbcexv4.common.impl.battle.participant.damage.DamageTypeGraphImpl;
import io.github.stuff_stuffs.tbcexv4.common.internal.network.ControllingBattleUpdatePacket;
import io.github.stuff_stuffs.tbcexv4.common.internal.network.Tbcexv4CommonNetwork;
import io.github.stuff_stuffs.tbcexv4.common.internal.world.BattlePersistentState;
import io.github.stuff_stuffs.tbcexv4.common.internal.world.ServerBattleWorld;
import it.unimi.dsi.fastutil.objects.Object2ReferenceOpenHashMap;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.dimension.v1.FabricDimensions;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityWorldChangeEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.packet.s2c.play.ChunkDataS2CPacket;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ChunkTicketType;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;
import net.minecraft.world.TeleportTarget;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.world.chunk.light.LightingProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

public class Tbcexv4 implements ModInitializer {
    public static final String MOD_ID = "tbcexv4";
    public static final String GENERATED_MOD_ID = "tbcexv4_generated";
    public static final Logger LOGGER = LoggerFactory.getLogger("tbcexv4");
    public static final ChunkTicketType<Unit> BATTLE_LOAD_CHUNK_TICKET_TYPE = ChunkTicketType.create(MOD_ID + ":battle_load", (a, b) -> 0, 1);
    private static final Map<RegistryKey<World>, Map<ChunkPos, Chunk>> UPDATED_BIOMES = new Object2ReferenceOpenHashMap<>();
    private static final AtomicReference<DamageTypeGraph> CACHED_DAMAGE_GRAPH = new AtomicReference<>(null);
    private static final AttachmentType<PlayerEnterBattleState> ENTER_BATTLE_ATTACHMENT_TYPE = AttachmentRegistry.<PlayerEnterBattleState>builder().copyOnDeath().persistent(PlayerEnterBattleState.CODEC).buildAndRegister(id("enter_battle"));


    @Override
    public void onInitialize() {
        Tbcexv4Registries.init();
        eventInit();
        Tbcexv4CommonNetwork.init();
        Tbcexv4InternalEvents.BATTLE_WATCH_EVENT.register((prev, current, entity) -> {
            if (current != null) {
                entity.changeGameMode(GameMode.SPECTATOR);
                final ServerBattleWorld world = (ServerBattleWorld) entity.getServerWorld().getServer().getWorld(battleWorldKey(current.sourceWorld()));
                if (world == null) {
                    throw new RuntimeException("Failed to get battle world, something in registration went wrong!");
                }
                final Optional<? extends Battle> opt = world.battleManager().getOrLoadBattle(current);
                if (opt.isPresent()) {
                    final Battle battle = opt.get();
                    entity.teleport(world, battle.worldX(battle.xSize() / 2), battle.worldY(battle.ySize() / 2), battle.worldZ(battle.ySize() / 2), 0, 0);
                }
            }
            ((ServerPlayerExtensions) entity).tbcexv4$setWatchIndex(0);
        });
        ServerTickEvents.START_WORLD_TICK.register(world -> {
            final RegistryKey<World> key = world.getRegistryKey();
            if (!checkGenerated(key.getValue())) {
                getBattlePersistentState(world).tick((ServerBattleWorld) world.getServer().getWorld(battleWorldKey(key)));
            }
        });
        ServerTickEvents.END_WORLD_TICK.register(world -> {
            final RegistryKey<World> key = world.getRegistryKey();
            if (!checkGenerated(key.getValue())) {
                getBattlePersistentState(world).tick((ServerBattleWorld) world.getServer().getWorld(battleWorldKey(key)));
            }
            final Map<ChunkPos, Chunk> updated = UPDATED_BIOMES.remove(key);
            if (updated == null) {
                return;
            }
            world.getChunkManager().threadedAnvilChunkStorage.sendChunkBiomePackets(List.copyOf(updated.values()));
        });
        ServerEntityWorldChangeEvents.AFTER_ENTITY_CHANGE_WORLD.register((originalEntity, newEntity, origin, destination) -> {
            if (newEntity instanceof final ServerPlayerEntity player) {
                ServerPlayNetworking.send(player, new ControllingBattleUpdatePacket(Tbcexv4Api.controlling(player)));
            }
        });
        ServerTickEvents.START_SERVER_TICK.register(server -> {
            for (final ServerPlayerEntity entity : server.getPlayerManager().getPlayerList()) {
                if (entity.age % 40 == 0) {
                    ServerPlayNetworking.send(entity, new ControllingBattleUpdatePacket(Tbcexv4Api.controlling(entity)));
                }
            }
        });
        ServerLifecycleEvents.END_DATA_PACK_RELOAD.register((server, resourceManager, success) -> {
            if (success) {
                CACHED_DAMAGE_GRAPH.setRelease(null);
                final List<ServerPlayerEntity> list = server.getPlayerManager().getPlayerList();
                for (final ServerWorld world : server.getWorlds()) {
                    if (world instanceof final ServerBattleWorld battleWorld) {
                        battleWorld.battleManager().reload(list);
                    }
                }
            }
        });
        Tbcexv4InternalEvents.BATTLE_WATCH_EVENT.register(Tbcexv4InternalEvents.PRE, (prev, current, entity) -> {
            if (prev != null || current == null) {
                return;
            }
            final RegistryKey<World> key = entity.getServerWorld().getRegistryKey();
            if (!Tbcexv4.checkGenerated(key.getValue())) {
                entity.setAttached(ENTER_BATTLE_ATTACHMENT_TYPE, new PlayerEnterBattleState(key, entity.getBlockPos()));
            }
        });
        Tbcexv4InternalEvents.BATTLE_WATCH_EVENT.register((prev, current, entity) -> {
            if (current == null) {
                final PlayerEnterBattleState attached = entity.removeAttached(ENTER_BATTLE_ATTACHMENT_TYPE);
                boolean fallback = true;
                if (attached != null) {
                    final ServerWorld world = entity.server.getWorld(attached.dimensionKey());
                    if (world != null) {
                        FabricDimensions.teleport(entity, world, new TeleportTarget(attached.pos.toCenterPos(), Vec3d.ZERO, 0, 0));
                        fallback = false;
                    }
                }
                if (fallback) {
                    entity.networkHandler.player = entity.server.getPlayerManager().respawnPlayer(entity, true);
                }
            }
        });
    }

    public static DamageTypeGraph getCachedDamageGraph(final Registry<DamageType> registry) {
        DamageTypeGraph graph = CACHED_DAMAGE_GRAPH.getAcquire();
        if (graph == null) {
            graph = new DamageTypeGraphImpl(registry);
            CACHED_DAMAGE_GRAPH.setRelease(graph);
        }
        return graph;
    }

    public static void updateBiomes(final RegistryKey<World> key, final Chunk chunk) {
        UPDATED_BIOMES.computeIfAbsent(key, k -> new Object2ReferenceOpenHashMap<>()).put(chunk.getPos(), chunk);
    }

    private static void eventInit() {
        final List<KeyHandlePair> keys = new ArrayList<>();
        collectEvents(BasicEvents.class, keys);
        collectEvents(BasicEnvEvents.class, keys);
        BattleStateEventInitEvent.EVENT.register(builder -> {
            for (final KeyHandlePair key : keys) {
                addEvent(builder, key.key, key.factoryMethod);
            }
        });
        final List<KeyHandlePair> participantKeys = new ArrayList<>();
        collectEvents(BasicParticipantEvents.class, participantKeys);
        collectEvents(ParticipantInventoryEvents.class, participantKeys);
        BattleParticipantEventInitEvent.EVENT.register(builder -> {
            for (final KeyHandlePair key : participantKeys) {
                addEvent(builder, key.key, key.factoryMethod);
            }
        });
    }

    public static boolean checkGenerated(final Identifier id) {
        if (!GENERATED_MOD_ID.equals(id.getNamespace())) {
            return false;
        }
        final String path = id.getPath();
        final int index = path.indexOf('/');
        return index >= 1;
    }

    public static Identifier parseGeneratedId(final Identifier id) {
        if (!GENERATED_MOD_ID.equals(id.getNamespace())) {
            throw new RuntimeException();
        }
        final String path = id.getPath();
        final int index = path.indexOf('/');
        if (index < 1) {
            throw new RuntimeException();
        }
        return new Identifier(path.substring(0, index), path.substring(index + 1));
    }

    private static Identifier generateId(final Identifier base) {
        return new Identifier(GENERATED_MOD_ID, base.getNamespace() + "/" + base.getPath());
    }

    public static RegistryKey<World> normalWorldKey(final RegistryKey<World> key) {
        if (!checkGenerated(key.getValue())) {
            return key;
        }
        return RegistryKey.of(RegistryKeys.WORLD, parseGeneratedId(key.getValue()));
    }

    public static RegistryKey<World> battleWorldKey(final RegistryKey<World> key) {
        if (checkGenerated(key.getValue())) {
            return key;
        }
        return RegistryKey.of(RegistryKeys.WORLD, generateId(key.getValue()));
    }

    private static <Mut, View> void addEvent(final EventMap.Builder builder, final EventKey<Mut, View> key, final MethodHandle handle) {
        try {
            //noinspection unchecked
            builder.add(key, (EventKey.Factory<Mut, View>) handle.invoke());
        } catch (final Throwable e) {
            throw new RuntimeException(e);
        }
    }

    private static void collectEvents(final Class<?> holder, final List<KeyHandlePair> keys) {
        final MethodHandles.Lookup lookup = MethodHandles.publicLookup();
        for (final Field field : holder.getDeclaredFields()) {
            if (field.getType() == EventKey.class && field.canAccess(null) && (field.getModifiers() & Modifier.STATIC) != 0) {
                try {
                    final EventKey<?, ?> key = (EventKey<?, ?>) field.get(null);
                    final MethodHandle handle = lookup.findStatic(key.mut(), "factory", MethodType.methodType(EventKey.Factory.class));
                    keys.add(new KeyHandlePair(key, handle));
                } catch (final IllegalAccessException | NoSuchMethodException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    public static Identifier id(final String path) {
        return new Identifier(MOD_ID, path);
    }

    public static BattlePersistentState getBattlePersistentState(final ServerWorld world) {
        if (checkGenerated(world.getRegistryKey().getValue())) {
            final ServerWorld source = world.getServer().getWorld(normalWorldKey(world.getRegistryKey()));
            if (source == null) {
                throw new RuntimeException();
            }
            return getBattlePersistentState(source);
        }
        return world.getPersistentStateManager().getOrCreate(BattlePersistentState.TYPE, "tbcexv4_battle_allocations");
    }

    public static void updateAllWatchers(final WorldChunk chunk, final LightingProvider provider) {
        for (final ServerPlayerEntity entity : PlayerLookup.tracking((ServerWorld) chunk.getWorld(), chunk.getPos())) {
            ServerPlayNetworking.getSender(entity).sendPacket(new ChunkDataS2CPacket(chunk, provider, null, null));
        }
    }

    private record KeyHandlePair(EventKey<?, ?> key, MethodHandle factoryMethod) {
    }

    public record PlayerEnterBattleState(RegistryKey<World> dimensionKey, BlockPos pos) {
        public static final Codec<PlayerEnterBattleState> CODEC = RecordCodecBuilder.create(instance ->
                instance.group(
                        RegistryKey.createCodec(RegistryKeys.WORLD).fieldOf("key").forGetter(PlayerEnterBattleState::dimensionKey),
                        BlockPos.CODEC.fieldOf("pos").forGetter(PlayerEnterBattleState::pos)
                ).apply(instance, PlayerEnterBattleState::new));
    }
}