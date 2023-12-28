package io.github.stuff_stuffs.tbcexv4.client.internal;

import io.github.stuff_stuffs.tbcexv4.client.api.*;
import io.github.stuff_stuffs.tbcexv4.client.impl.battle.ClientBattleImpl;
import io.github.stuff_stuffs.tbcexv4.client.impl.battle.state.env.ClientBattleEnvironmentImpl;
import io.github.stuff_stuffs.tbcexv4.client.internal.ui.BattleMenuScreen;
import io.github.stuff_stuffs.tbcexv4.client.internal.ui.component.Tbcexv4UiComponents;
import io.github.stuff_stuffs.tbcexv4.common.api.Tbcexv4Registries;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.BattleCodecContext;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.BattleHandle;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.BattleView;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.action.BattleAction;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.action.request.BattleActionRequest;
import io.github.stuff_stuffs.tbcexv4.common.impl.battle.ServerBattleImpl;
import io.github.stuff_stuffs.tbcexv4.common.internal.Tbcexv4;
import io.github.stuff_stuffs.tbcexv4.common.internal.Tbcexv4ClientDelegates;
import io.github.stuff_stuffs.tbcexv4.common.internal.network.Tbcexv4CommonNetwork;
import io.github.stuff_stuffs.tbcexv4.common.internal.network.TryBattleActionPacket;
import io.github.stuff_stuffs.tbcexv4.common.internal.network.WatchRequestPacket;
import io.github.stuff_stuffs.tbcexv4.common.internal.network.WatchRequestResponsePacket;
import io.github.stuff_stuffs.tbcexv4.common.internal.world.BattleEnvironmentInitialState;
import it.unimi.dsi.fastutil.objects.Object2ReferenceOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.nbt.NbtOps;
import net.minecraft.util.math.MathHelper;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public class Tbcexv4Client implements ClientModInitializer {
    private static @Nullable BattleHandle WATCHING = null;
    private static @Nullable ClientBattleImpl WATCHED_BATTLE = null;
    private static final Set<BattleHandle> POSSIBLE_CONTROLLING = new ObjectOpenHashSet<>();
    private static final Map<UUID, DelayedResponse<Tbcexv4ClientApi.RequestResult>> ONGOING_RESULTS = new Object2ReferenceOpenHashMap<>();

    @Override
    public void onInitializeClient() {
        WatchedBattleChangeEvent.EVENT.register(handle -> {
            final Screen screen = MinecraftClient.getInstance().currentScreen;
            if (screen instanceof BattleMenuScreen) {
                MinecraftClient.getInstance().setScreen(null);
            }
        });
        ClientPlayConnectionEvents.INIT.register((handler, client) -> {
            WATCHING = null;
            WATCHED_BATTLE = null;
        });
        ClientPlayNetworking.registerGlobalReceiver(Tbcexv4CommonNetwork.WATCH_REQUEST_RESPONSE_PACKET_TYPE, (packet, player, responseSender) -> {
            final WatchRequestResponsePacket.Info info = packet.info();
            final BattleCodecContext codecContext = BattleCodecContext.create(player.getWorld().getRegistryManager());
            final Optional<ServerBattleImpl.TurnManagerContainer<?>> opt = packet.decodeTurnManager(codecContext);
            if (info == null || opt.isEmpty()) {
                WATCHING = null;
                WATCHED_BATTLE = null;
                WatchedBattleChangeEvent.EVENT.invoker().onWatchedBattleChanged(Optional.empty());
            } else {
                WATCHING = packet.handle();
                final ServerBattleImpl.TurnManagerContainer<?> container = opt.get();
                WATCHED_BATTLE = new ClientBattleImpl(player.clientWorld, packet.handle(), info.sourceWorld(), info.min(), info.xSize(), info.ySize(), info.zSize(), () -> container.create(codecContext));
                WatchedBattleChangeEvent.EVENT.invoker().onWatchedBattleChanged(Optional.of(packet.handle()));
            }
        });
        ClientPlayNetworking.registerGlobalReceiver(Tbcexv4CommonNetwork.CONTROLLING_BATTLE_UPDATE_PACKET_TYPE, (packet, player, responseSender) -> {
            POSSIBLE_CONTROLLING.clear();
            POSSIBLE_CONTROLLING.addAll(packet.handles());
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
                final BattleCodecContext codecContext = BattleCodecContext.create(player.getWorld().getRegistryManager());
                final Optional<BattleAction> result = BattleAction.codec(codecContext).parse(NbtOps.INSTANCE, packet.element()).result();
                if (result.isEmpty()) {
                    throw new RuntimeException();
                }
                WATCHED_BATTLE.pushAction(result.get());
                BattleActionReceivedEvent.EVENT.invoker().onBattleActionReceived(WATCHED_BATTLE);
            }
        });
        ClientPlayNetworking.registerGlobalReceiver(Tbcexv4CommonNetwork.TRY_BATTLE_ACTION_RESPONSE_PACKET_TYPE, (packet, player, responseSender) -> {
            final UUID uuid = packet.requestId();
            final DelayedResponse<Tbcexv4ClientApi.RequestResult> response = ONGOING_RESULTS.remove(uuid);
            if (response == null) {
                Tbcexv4.LOGGER.error("Unknown ongoing request!");
            } else {
                final Tbcexv4ClientApi.RequestResult result;
                if (packet.success()) {
                    result = new Tbcexv4ClientApi.SuccessfulRequestResult(packet.desc());
                } else {
                    result = new Tbcexv4ClientApi.FailedRequestResult(packet.desc());
                }
                DelayedResponse.tryComplete(response, result);
            }
        });
        Tbcexv4UiComponents.init();
        BattleItemRendererRegistry.registry(Tbcexv4Registries.ItemTypes.UNKNOWN_BATTLE_ITEM_TYPE, (item, vertexConsumers, matrices) -> {
            final ItemRenderer renderer = MinecraftClient.getInstance().getItemRenderer();
            renderer.renderItem(item.renderStack(), ModelTransformationMode.FIXED, LightmapTextureManager.pack(15, 15), OverlayTexture.DEFAULT_UV, matrices, vertexConsumers, null, 42);
        });
    }

    public static DelayedResponse<Tbcexv4ClientApi.RequestResult> sendRequest(final BattleActionRequest request) {
        final UUID id = MathHelper.randomUuid();
        final DelayedResponse<Tbcexv4ClientApi.RequestResult> response = DelayedResponse.create();
        ONGOING_RESULTS.put(id, response);
        ClientPlayNetworking.send(new TryBattleActionPacket(request, id, BattleCodecContext.create(MinecraftClient.getInstance().world.getRegistryManager())));
        return response;
    }

    public static Set<BattleHandle> possibleControlling() {
        return new ObjectOpenHashSet<>(POSSIBLE_CONTROLLING);
    }

    public static @Nullable BattleHandle watching() {
        return WATCHING;
    }

    public static @Nullable BattleView watched() {
        return WATCHED_BATTLE;
    }

    public static void requestWatching(@Nullable final BattleHandle handle) {
        ClientPlayNetworking.send(new WatchRequestPacket(handle));
    }
}