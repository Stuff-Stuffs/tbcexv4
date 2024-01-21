package io.github.stuff_stuffs.tbcexv4.client.internal;

import io.github.stuff_stuffs.tbcexv4.client.api.BattleActionReceivedEvent;
import io.github.stuff_stuffs.tbcexv4.client.api.DelayedResponse;
import io.github.stuff_stuffs.tbcexv4.client.api.Tbcexv4ClientApi;
import io.github.stuff_stuffs.tbcexv4.client.api.WatchedBattleChangeEvent;
import io.github.stuff_stuffs.tbcexv4.client.api.render.BattleItemRendererRegistry;
import io.github.stuff_stuffs.tbcexv4.client.api.render.BattleRenderContext;
import io.github.stuff_stuffs.tbcexv4.client.api.render.animation.state.BattleRenderState;
import io.github.stuff_stuffs.tbcexv4.client.api.render.animation.state.ModelRenderState;
import io.github.stuff_stuffs.tbcexv4.client.api.render.animation.state.ParticipantRenderState;
import io.github.stuff_stuffs.tbcexv4.client.api.render.animation.state.PropertyTypes;
import io.github.stuff_stuffs.tbcexv4.client.api.render.renderer.ModelRendererRegistry;
import io.github.stuff_stuffs.tbcexv4.client.impl.battle.ClientBattleImpl;
import io.github.stuff_stuffs.tbcexv4.client.impl.battle.state.env.ClientBattleEnvironmentImpl;
import io.github.stuff_stuffs.tbcexv4.client.impl.render.BattleRenderContextImpl;
import io.github.stuff_stuffs.tbcexv4.client.impl.render.animation.state.BattleRenderStateImpl;
import io.github.stuff_stuffs.tbcexv4.client.impl.render.animation.state.ModelRenderStateImpl;
import io.github.stuff_stuffs.tbcexv4.client.internal.ui.BasicTargetUi;
import io.github.stuff_stuffs.tbcexv4.client.internal.ui.BattleMenuScreen;
import io.github.stuff_stuffs.tbcexv4.client.internal.ui.BattleTargetingMenu;
import io.github.stuff_stuffs.tbcexv4.client.internal.ui.component.Tbcexv4UiComponents;
import io.github.stuff_stuffs.tbcexv4.common.api.Tbcexv4Registries;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.BattleCodecContext;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.BattleHandle;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.BattlePhase;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.BattleView;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.action.BattleAction;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.BattleParticipant;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.BattleParticipantHandle;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.BattleParticipantView;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.attachment.BattleParticipantPlayerControllerAttachmentView;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.state.BattleState;
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
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.nbt.NbtOps;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public class Tbcexv4Client implements ClientModInitializer {
    private static @Nullable BattleHandle WATCHING = null;
    private static @Nullable ClientBattleImpl WATCHED_BATTLE = null;
    private static @Nullable BattleParticipantHandle CONTROLLING = null;
    private static final Set<BattleHandle> POSSIBLE_WATCHING = new ObjectOpenHashSet<>();
    private static final Map<UUID, DelayedResponse<Tbcexv4ClientApi.RequestResult>> ONGOING_RESULTS = new Object2ReferenceOpenHashMap<>();

    @Override
    public void onInitializeClient() {
        WatchedBattleChangeEvent.EVENT.register(handle -> {
            final Screen screen = MinecraftClient.getInstance().currentScreen;
            if (screen instanceof BattleMenuScreen) {
                MinecraftClient.getInstance().setScreen(null);
            }
            if (BattleTargetingMenu.targeting()) {
                BattleTargetingMenu.closeAll();
            }
        });
        ClientPlayConnectionEvents.INIT.register((handler, client) -> {
            WATCHING = null;
            WATCHED_BATTLE = null;
            CONTROLLING = null;
        });
        ClientPlayNetworking.registerGlobalReceiver(Tbcexv4CommonNetwork.WATCH_REQUEST_RESPONSE_PACKET_TYPE, (packet, player, responseSender) -> {
            final WatchRequestResponsePacket.Info info = packet.info();
            final BattleCodecContext codecContext = BattleCodecContext.create(player.getWorld().getRegistryManager());
            final Optional<ServerBattleImpl.TurnManagerContainer<?>> opt = packet.decodeTurnManager(codecContext);
            if (info == null || opt.isEmpty()) {
                WATCHING = null;
                WATCHED_BATTLE = null;
                CONTROLLING = null;
                WatchedBattleChangeEvent.EVENT.invoker().onWatchedBattleChanged(Optional.empty());
            } else {
                WATCHING = packet.handle();
                final ServerBattleImpl.TurnManagerContainer<?> container = opt.get();
                WATCHED_BATTLE = new ClientBattleImpl(player.clientWorld, packet.handle(), info.sourceWorld(), info.min(), info.xSize(), info.ySize(), info.zSize(), () -> container.create(codecContext));
                CONTROLLING = null;
                WatchedBattleChangeEvent.EVENT.invoker().onWatchedBattleChanged(Optional.of(packet.handle()));
            }
        });
        ClientPlayNetworking.registerGlobalReceiver(Tbcexv4CommonNetwork.CONTROLLING_BATTLE_UPDATE_PACKET_TYPE, (packet, player, responseSender) -> {
            POSSIBLE_WATCHING.clear();
            POSSIBLE_WATCHING.addAll(packet.handles());
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
                    result = new Tbcexv4ClientApi.SuccessfulRequestResult();
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
        BattleActionReceivedEvent.EVENT.register(battle -> {
            if (CONTROLLING != null) {
                final BattleParticipantView participant = battle.state().participant(CONTROLLING);
                if (participant == null) {
                    CONTROLLING = null;
                    return;
                }
                final Optional<BattleParticipantPlayerControllerAttachmentView> view = participant.attachmentView(Tbcexv4Registries.BattleParticipantAttachmentTypes.PLAYER_CONTROLLED);
                final UUID uuid = MinecraftClient.getInstance().player.getUuid();
                if (view.isEmpty() || !uuid.equals(view.get().controllerId())) {
                    CONTROLLING = null;
                }
            }
        });
        BattleDebugRendererRegistry.init();
        ClientTickEvents.END_WORLD_TICK.register(client -> {
            if (WATCHED_BATTLE != null) {
                WATCHED_BATTLE.tick();
            }
        });
        WorldRenderEvents.BEFORE_ENTITIES.register(context -> {
            if (WATCHED_BATTLE != null) {
                final BattleRenderState state = WATCHED_BATTLE.animationQueue().state();
                ((BattleRenderStateImpl) state).update(WATCHED_BATTLE.time(context.tickDelta()));
            }
        });
        WorldRenderEvents.AFTER_ENTITIES.register(context -> {
            if (WATCHED_BATTLE == null) {
                return;
            }
            for (final String s : BattleDebugRendererRegistry.enabled()) {
                final BattleDebugRenderer renderer = BattleDebugRendererRegistry.get(s);
                renderer.render(context, WATCHED_BATTLE);
            }
            final BattleRenderContext renderContext = new BattleRenderContextImpl(context, WATCHED_BATTLE);
            final MatrixStack matrices = context.matrixStack();
            matrices.push();
            final Vec3d pos = context.camera().getPos();
            matrices.translate(-pos.x, -pos.y, -pos.z);
            matrices.translate(WATCHED_BATTLE.worldX(0), WATCHED_BATTLE.worldY(0), WATCHED_BATTLE.worldZ(0));
            for (final ParticipantRenderState participant : ((BattleRenderStateImpl) WATCHED_BATTLE.animationQueue().state()).cached()) {
                render(participant.modelRoot(), renderContext);
            }
            matrices.pop();
        });
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> dispatcher.register(
                ClientCommandManager.literal("tbcexv4ClientDebug")
                        .then(
                                ClientCommandManager.literal("enable")
                                        .then(
                                                ClientCommandManager.argument(
                                                                "renderer",
                                                                new BattleDebugRendererRegistry.DebugRendererArgumentType()
                                                        )
                                                        .executes(context -> {
                                                            final String id = context.getArgument("renderer", String.class);
                                                            BattleDebugRendererRegistry.enable(id);
                                                            return 0;
                                                        })
                                        )
                        ).then(
                                ClientCommandManager.literal("disable")
                                        .then(
                                                ClientCommandManager.argument(
                                                                "renderer",
                                                                new BattleDebugRendererRegistry.DebugRendererArgumentType()
                                                        )
                                                        .executes(context -> {
                                                            final String id = context.getArgument("renderer", String.class);
                                                            BattleDebugRendererRegistry.disable(id);
                                                            return 0;
                                                        })
                                        )
                        )
        ));
        BattleTargetingMenu.initClient();
        BasicTargetUi.init();
        PropertyTypes.init();
        ModelRendererRegistry.init();
    }

    private void render(final ModelRenderState state, final BattleRenderContext context) {
        state.getProperty(ModelRenderState.RENDERER).get().render(context, state);
        for (final ModelRenderState child : ((ModelRenderStateImpl) state).cached()) {
            render(child, context);
        }
    }

    public static DelayedResponse<Tbcexv4ClientApi.RequestResult> sendRequest(final BattleAction request) {
        if (CONTROLLING == null || WATCHED_BATTLE == null) {
            final DelayedResponse<Tbcexv4ClientApi.RequestResult> response = DelayedResponse.create();
            DelayedResponse.tryComplete(response, new Tbcexv4ClientApi.FailedRequestResult(Text.of("Error during sending packet!")));
            return response;
        }
        UUID id;
        do {
            id = MathHelper.randomUuid();
        } while (ONGOING_RESULTS.containsKey(id));
        final DelayedResponse<Tbcexv4ClientApi.RequestResult> response = DelayedResponse.create();
        ONGOING_RESULTS.put(id, response);
        ClientPlayNetworking.send(new TryBattleActionPacket(request, id, CONTROLLING.id(), BattleCodecContext.create(MinecraftClient.getInstance().world.getRegistryManager())));
        return response;
    }

    public static Set<BattleHandle> possibleWatching() {
        return new ObjectOpenHashSet<>(POSSIBLE_WATCHING);
    }

    public static @Nullable BattleHandle watching() {
        return WATCHING;
    }

    public static @Nullable BattleView watched() {
        return WATCHED_BATTLE;
    }

    public static @Nullable BattleParticipantHandle controlling() {
        return CONTROLLING;
    }

    public static void requestWatching(@Nullable final BattleHandle handle) {
        ClientPlayNetworking.send(new WatchRequestPacket(handle));
    }

    public static boolean tryControl(final BattleParticipantHandle handle) {
        if (WATCHED_BATTLE == null) {
            return false;
        }
        final BattleParticipant participant = WATCHED_BATTLE.state().participant(handle);
        if (participant == null) {
            return false;
        }
        final Optional<BattleParticipantPlayerControllerAttachmentView> attachment = participant.attachmentView(Tbcexv4Registries.BattleParticipantAttachmentTypes.PLAYER_CONTROLLED);
        if (attachment.isEmpty()) {
            return false;
        }
        final UUID uuid = MinecraftClient.getInstance().player.getUuid();
        if (!uuid.equals(attachment.get().controllerId())) {
            return false;
        }
        CONTROLLING = handle;
        Tbcexv4ClientApi.BATTLE_WATCH_EVENT.invoker().onWatch(WATCHING, CONTROLLING);
        return true;
    }

    public static Set<BattleParticipantHandle> possibleControlling() {
        if (WATCHED_BATTLE == null || WATCHED_BATTLE.phase() == BattlePhase.FINISHED) {
            return Set.of();
        }
        final BattleState state = WATCHED_BATTLE.state();
        final Set<BattleParticipantHandle> handles = new ObjectOpenHashSet<>();
        final UUID uuid = MinecraftClient.getInstance().player.getUuid();
        for (final BattleParticipantHandle handle : state.participants()) {
            final BattleParticipant participant = state.participant(handle);
            final Optional<BattleParticipantPlayerControllerAttachmentView> attachment = participant.attachmentView(Tbcexv4Registries.BattleParticipantAttachmentTypes.PLAYER_CONTROLLED);
            if (attachment.isPresent() && uuid.equals(attachment.get().controllerId())) {
                handles.add(handle);
            }
        }
        return handles;
    }
}