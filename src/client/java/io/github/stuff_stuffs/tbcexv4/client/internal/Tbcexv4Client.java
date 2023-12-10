package io.github.stuff_stuffs.tbcexv4.client.internal;

import io.github.stuff_stuffs.tbcexv4.client.impl.battle.ClientBattleImpl;
import io.github.stuff_stuffs.tbcexv4.client.impl.battle.state.env.ClientBattleEnvironmentImpl;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.BattleCodecContext;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.BattleHandle;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.action.BattleAction;
import io.github.stuff_stuffs.tbcexv4.common.internal.Tbcexv4ClientDelegates;
import io.github.stuff_stuffs.tbcexv4.common.internal.network.Tbcexv4CommonNetwork;
import io.github.stuff_stuffs.tbcexv4.common.internal.network.WatchRequestPacket;
import io.github.stuff_stuffs.tbcexv4.common.internal.network.WatchRequestResponsePacket;
import io.github.stuff_stuffs.tbcexv4.common.internal.world.BattleEnvironmentInitialState;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.nbt.NbtOps;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class Tbcexv4Client implements ClientModInitializer {
    public static @Nullable BattleHandle WATCHING = null;
    private static @Nullable ClientBattleImpl WATCHED_BATTLE = null;

    @Override
    public void onInitializeClient() {
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> WATCHING = null);
        ClientPlayNetworking.registerGlobalReceiver(Tbcexv4CommonNetwork.WATCH_REQUEST_RESPONSE_PACKET_TYPE, (packet, player, responseSender) -> {
            WATCHING = packet.handle();
            final WatchRequestResponsePacket.Info info = packet.info();
            if (info == null) {
                throw new RuntimeException();
            }
            WATCHED_BATTLE = new ClientBattleImpl(player.clientWorld, packet.handle(), info.sourceWorld(), info.min(), info.xSize(), info.ySize(), info.zSize());
        });
        Tbcexv4ClientDelegates.SETUP_CLIENT_ENV_DELEGATE = (state, environment) -> {
            final int width = state.width();
            final int height = state.height();
            final int depth = state.depth();
            for (int i = 0; i < width; i++) {
                for (int j = 0; j < height; j++) {
                    for (int k = 0; k < depth; k++) {
                        final ClientBattleEnvironmentImpl.Section section = ((ClientBattleEnvironmentImpl) environment).get(i, j, k);
                        final BattleEnvironmentInitialState.ChunkSection chunkSection = state.get(i, j, k);
                        section.blockStateContainer = chunkSection.blockStates.copy();
                        section.biomeContainer = chunkSection.biomes.copy();
                    }
                }
            }
        };
        ClientPlayNetworking.registerGlobalReceiver(Tbcexv4CommonNetwork.BATTLE_UPDATE_PACKET_TYPE, (packet, player, responseSender) -> {
            if (packet.handle().equals(WATCHING)) {
                if (WATCHED_BATTLE == null) {
                    throw new RuntimeException();
                }
                WATCHED_BATTLE.trim(packet.index());
                final BattleCodecContext codecContext = player.clientWorld::getRegistryManager;
                final Optional<BattleAction> result = BattleAction.codec(codecContext).parse(NbtOps.INSTANCE, packet.element()).result();
                if (result.isEmpty()) {
                    throw new RuntimeException();
                }
                WATCHED_BATTLE.pushAction(result.get());
            }
        });

    }

    public static void requestWatching(@Nullable final BattleHandle handle) {
        ClientPlayNetworking.send(new WatchRequestPacket(handle));
    }
}