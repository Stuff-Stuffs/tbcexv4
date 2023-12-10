package io.github.stuff_stuffs.tbcexv4.common.internal;

import io.github.stuff_stuffs.event_gen.api.event.EventKey;
import io.github.stuff_stuffs.tbcexv4.common.api.Tbcexv4Registries;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.BattleParticipantEventInitEvent;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.state.BattleStateEventInitEvent;
import io.github.stuff_stuffs.tbcexv4.common.api.event.EventMap;
import io.github.stuff_stuffs.tbcexv4.common.generated_events.BasicEvents;
import io.github.stuff_stuffs.tbcexv4.common.generated_events.env.BasicEnvEvents;
import io.github.stuff_stuffs.tbcexv4.common.generated_events.participant.BasicParticipantEvents;
import io.github.stuff_stuffs.tbcexv4.common.internal.network.Tbcexv4CommonNetwork;
import io.github.stuff_stuffs.tbcexv4.common.internal.world.BattlePersistentState;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.packet.s2c.play.ChunkDataS2CPacket;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
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

public class Tbcexv4 implements ModInitializer {
    public static final String MOD_ID = "tbcexv4";
    public static final Logger LOGGER = LoggerFactory.getLogger("tbcexv4");
    public static final RegistryKey<World> BATTLE_WORLD_KEY = RegistryKey.of(RegistryKeys.WORLD, id("battle"));

    @Override
    public void onInitialize() {
        Tbcexv4Registries.init();
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
        BattleParticipantEventInitEvent.EVENT.register(builder -> {
            for (final KeyHandlePair key : participantKeys) {
                addEvent(builder, key.key, key.factoryMethod);
            }
        });
        Tbcexv4CommonNetwork.init();
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
        if (!world.getRegistryKey().equals(BATTLE_WORLD_KEY)) {
            throw new RuntimeException();
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
}