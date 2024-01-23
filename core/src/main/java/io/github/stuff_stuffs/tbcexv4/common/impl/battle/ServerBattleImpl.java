package io.github.stuff_stuffs.tbcexv4.common.impl.battle;

import com.mojang.datafixers.util.Pair;
import com.mojang.datafixers.util.Unit;
import com.mojang.serialization.Codec;
import io.github.stuff_stuffs.tbcexv4.common.api.Tbcexv4Registries;
import io.github.stuff_stuffs.tbcexv4.common.api.ai.ActionSearchStrategy;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.Battle;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.BattleCodecContext;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.BattleHandle;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.BattlePhase;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.action.ActionSource;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.action.BattleAction;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.action.core.EndBattleAction;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.log.BattleLogContext;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.BattleParticipant;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.BattleParticipantEventInitEvent;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.BattleParticipantHandle;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.attachment.BattleParticipantAIControllerAttachment;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.attachment.BattleParticipantPlayerControllerAttachmentView;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.team.BattleParticipantTeam;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.team.BattleParticipantTeamRelation;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.state.BattleState;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.state.BattleStateEventInitEvent;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.state.env.BattleEnvironment;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.tracer.BattleTracer;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.tracer.event.CoreBattleTraceEvents;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.transaction.BattleTransaction;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.turn.TurnManager;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.turn.TurnManagerType;
import io.github.stuff_stuffs.tbcexv4.common.api.event.EventMap;
import io.github.stuff_stuffs.tbcexv4.common.api.util.Result;
import io.github.stuff_stuffs.tbcexv4.common.impl.battle.state.BattleStateImpl;
import io.github.stuff_stuffs.tbcexv4.common.impl.battle.state.env.ServerBattleEnvironmentImpl;
import io.github.stuff_stuffs.tbcexv4.common.internal.Tbcexv4;
import io.github.stuff_stuffs.tbcexv4.common.internal.world.BattlePersistentState;
import io.github.stuff_stuffs.tbcexv4.common.internal.world.ServerBattleWorld;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtOps;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.*;
import java.util.function.BiConsumer;

public class ServerBattleImpl implements Battle {
    private static final long VERSION = 0;
    private final List<BattleAction> actions;
    private final ServerBattleWorld world;
    private final BattleHandle handle;
    private final RegistryKey<World> sourceWorld;
    private final BlockPos min;
    private final int xSize, ySize, zSize;
    public final TurnManagerContainer<?> turnManagerContainer;
    private final BattleState state;
    private final BattleTracer tracer;
    private final TurnManager turnManager;
    private final AiController aiController;
    private boolean loading = false;

    public ServerBattleImpl(final ServerBattleWorld world, final BattleHandle handle, final RegistryKey<World> sourceWorld, final BlockPos min, final int xSize, final int ySize, final int zSize, final TurnManagerContainer<?> container, final AiController aiController) {
        this.handle = handle;
        this.sourceWorld = sourceWorld;
        turnManagerContainer = container;
        this.aiController = aiController;
        actions = new ArrayList<>();
        this.world = world;
        this.min = min;
        final EventMap.Builder builder = EventMap.builder();
        BattleStateEventInitEvent.EVENT.invoker().addEvents(builder);
        final EventMap.Builder participantEventBuilder = EventMap.builder();
        BattleParticipantEventInitEvent.EVENT.invoker().addEvents(participantEventBuilder);
        this.xSize = xSize;
        this.ySize = ySize;
        this.zSize = zSize;
        state = new BattleStateImpl(this, createEnv(), sourceWorld, builder, participantEventBuilder);
        tracer = BattleTracer.create(new CoreBattleTraceEvents.Root());
        turnManager = turnManagerContainer.create(BattleCodecContext.create(world.getRegistryManager()));
        try (final var transaction = state.transactionManager().open(); final var span = tracer.push(new CoreBattleTraceEvents.TurnManagerSetup(), transaction)) {
            turnManager.setup(state, transaction, span);
            transaction.commit();
        }
    }

    public void setLoading(final boolean loading) {
        this.loading = loading;
        if (!loading) {
            checks();
        }
    }

    public Result<Unit, Text> check(final Optional<UUID> sourceId, final BattleAction action) {
        aiController.cancel();
        Result<Unit, Text> result = turnManager.check(action);
        if (result instanceof Result.Success<Unit, Text> && sourceId.isPresent() && action.source().isPresent()) {
            final Optional<BattleParticipantPlayerControllerAttachmentView> attachmentView = state.participant(action.source().get().actor()).attachmentView(Tbcexv4Registries.BattleParticipantAttachmentTypes.PLAYER_CONTROLLED);
            if (attachmentView.isPresent()) {
                if (!attachmentView.get().controllerId().equals(sourceId.get())) {
                    result = new Result.Failure<>(Text.of("You do not have control!"));
                }
            } else {
                result = new Result.Failure<>(Text.of("You do not have control!"));
            }
        }
        checks();
        return result;
    }

    private BattleEnvironment createEnv() {
        return new ServerBattleEnvironmentImpl(this);
    }

    @Override
    public BattleHandle handle() {
        return handle;
    }

    @Override
    public BattleState state() {
        if (state == null) {
            throw new RuntimeException();
        }
        return state;
    }

    @Override
    public int actions() {
        return actions.size();
    }

    @Override
    public BattleAction action(final int index) {
        return actions.get(index);
    }

    @Override
    public void pushAction(final BattleAction action) {
        if (phase() == BattlePhase.FINISHED) {
            throw new RuntimeException();
        }
        aiController.cancel();
        actions.add(action);
        final Optional<ActionSource> actionSource = action.source();
        try (final var transaction = state.transactionManager().open(); final var span = tracer.push(new CoreBattleTraceEvents.ActionRoot(actionSource.map(ActionSource::actor)), transaction)) {
            action.apply(state, transaction, tracer, BattleLogContext.DISABLED);
            actionSource.ifPresent(source -> turnManager.onAction(source.energy(), source.actor(), state, transaction, span));
            transaction.commit();
        }
        if (phase() == BattlePhase.FINISHED) {
            return;
        }
        if (!loading) {
            checks();
        }
    }

    private void checks() {
        if (checkFinished()) {
            if (phase() == BattlePhase.BATTLE) {
                pushAction(new EndBattleAction());
            }
        } else {
            final Set<BattleParticipantHandle> handles = turnManager.currentTurn();
            boolean playerFound = false;
            final List<BattleParticipantHandle> ais = new ArrayList<>();
            for (final BattleParticipantHandle participantHandle : handles) {
                final BattleParticipant participant = state.participant(participantHandle);
                if (participant.attachmentView(Tbcexv4Registries.BattleParticipantAttachmentTypes.PLAYER_CONTROLLED).isPresent()) {
                    playerFound = true;
                    break;
                } else if (participant.attachmentView(Tbcexv4Registries.BattleParticipantAttachmentTypes.AI_CONTROLLER).isPresent()) {
                    ais.add(participantHandle);
                }
            }
            if (!playerFound && !ais.isEmpty()) {
                final BattleParticipantHandle chosen = ais.get(actions.size() % ais.size());
                final BattleParticipant participant = state.participant(chosen);
                final Optional<BattleParticipantAIControllerAttachment> opt = participant.attachment(Tbcexv4Registries.BattleParticipantAttachmentTypes.AI_CONTROLLER);
                if (opt.isEmpty()) {
                    throw new RuntimeException();
                }
                final BattleParticipantAIControllerAttachment attachment = opt.get();
                aiController.compute(chosen, attachment.strategy(), state.transactionManager().open(), tracer);
            }
        }
    }

    private boolean checkFinished() {
        final Set<BattleParticipantTeam> teams = new ObjectOpenHashSet<>();
        for (final BattleParticipantHandle participant : state.participants()) {
            teams.add(state.participant(participant).team());
        }
        if (teams.size() < 2) {
            return true;
        }
        for (final BattleParticipantTeam team : teams) {
            for (final BattleParticipantTeam other : teams) {
                if (state.relation(team, other) == BattleParticipantTeamRelation.HOSTILE) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public TurnManager turnManager() {
        return turnManager;
    }

    @Override
    public BattlePhase phase() {
        return state != null ? state.phase() : BattlePhase.SETUP;
    }

    public ServerBattleWorld world() {
        return world;
    }

    @Override
    public int worldX(final int localX) {
        return min.getX() + localX;
    }

    @Override
    public int localX(final int worldX) {
        return worldX - min.getX();
    }

    @Override
    public int worldY(final int localY) {
        return min.getY() + localY;
    }

    @Override
    public int localY(final int worldY) {
        return worldY - min.getY();
    }

    @Override
    public int worldZ(final int localZ) {
        return min.getZ() + localZ;
    }

    @Override
    public int localZ(final int worldZ) {
        return worldZ - min.getZ();
    }

    @Override
    public int xSize() {
        return xSize;
    }

    @Override
    public int ySize() {
        return ySize;
    }

    @Override
    public int zSize() {
        return zSize;
    }

    public interface AiController {
        void compute(BattleParticipantHandle handle, ActionSearchStrategy strategy, BattleTransaction context, BattleTracer tracer);

        void cancel();
    }

    public NbtCompound serialize() {
        final NbtList actionList = new NbtList();
        final BattleCodecContext codecContext = BattleCodecContext.create(world.getRegistryManager());
        final Codec<BattleAction> codec = BattleAction.codec(codecContext);
        for (final BattleAction action : actions) {
            final NbtCompound wrapper = new NbtCompound();
            wrapper.put("data", codec.encodeStart(NbtOps.INSTANCE, action).getOrThrow(false, Tbcexv4.LOGGER::error));
            actionList.add(wrapper);
        }
        final NbtCompound nbt = new NbtCompound();
        nbt.putLong("version", VERSION);
        final Optional<NbtElement> result = RegistryKey.createCodec(RegistryKeys.WORLD).encodeStart(NbtOps.INSTANCE, sourceWorld).result();
        if (result.isEmpty()) {
            throw new RuntimeException();
        }
        nbt.put("sourceWorld", result.get());
        nbt.put("actions", actionList);
        nbt.putInt("x", xSize);
        nbt.putInt("y", ySize);
        nbt.putInt("z", zSize);
        nbt.put("turnManager", turnManagerContainer.toNbt(codecContext));
        return nbt;
    }

    public static Optional<Pair<BattlePersistentState.Token, ServerBattleImpl>> deserialize(final NbtCompound nbt,
                                                                                            final BattleHandle handle, final ServerBattleWorld world, final AiController aiController, final BiConsumer<BattleHandle, BattleState> listenerSetup) {
        if (nbt.getLong("version") != VERSION) {
            return Optional.empty();
        }
        final Optional<Pair<BattlePersistentState.Token, BlockPos>> min = Tbcexv4.getBattlePersistentState(world).allocate(nbt.getInt("x"), nbt.getInt("z"), world);
        if (min.isEmpty()) {
            return Optional.empty();
        }
        final Optional<RegistryKey<World>> sourceWorld = RegistryKey.createCodec(RegistryKeys.WORLD).parse(NbtOps.INSTANCE, nbt.get("sourceWorld")).result();
        if (sourceWorld.isEmpty()) {
            throw new RuntimeException();
        }
        final BattleCodecContext codecContext = BattleCodecContext.create(world.getRegistryManager());
        final Optional<TurnManagerContainer<?>> turnManager = TurnManagerContainer.fromNbt(codecContext, nbt.getCompound("turnManager"));
        if (turnManager.isEmpty()) {
            return Optional.empty();
        }
        final ServerBattleImpl battle = new ServerBattleImpl(world, handle, sourceWorld.get(), min.get().getSecond(), nbt.getInt("x"), nbt.getInt("y"), nbt.getInt("z"), turnManager.get(), aiController);
        battle.setLoading(true);
        listenerSetup.accept(handle, battle.state);
        final NbtList actions = nbt.getList("actions", NbtElement.COMPOUND_TYPE);
        final Codec<BattleAction> codec = BattleAction.codec(codecContext);
        for (final NbtElement action : actions) {
            final NbtCompound wrapper = (NbtCompound) action;
            final Optional<BattleAction> result = codec.parse(NbtOps.INSTANCE, wrapper.get("data")).result();
            if (result.isEmpty()) {
                return Optional.empty();
            }
            final BattleAction battleAction = result.get();
            battle.pushAction(battleAction);
        }
        return Optional.of(Pair.of(min.get().getFirst(), battle));
    }

    public record TurnManagerContainer<P>(TurnManagerType<P> type, P parameter) {
        public TurnManager create(final BattleCodecContext context) {
            return type.create(context, parameter);
        }

        public static Optional<TurnManagerContainer<?>> fromNbt(final BattleCodecContext context, final NbtCompound nbt) {
            final Identifier id = new Identifier(nbt.getString("type"));
            final TurnManagerType<?> type = Tbcexv4Registries.TurnManagerTypes.REGISTRY.get(id);
            if (type == null) {
                return Optional.empty();
            }
            return decodeParameter(context, type, nbt);
        }

        private static <P> Optional<TurnManagerContainer<?>> decodeParameter(final BattleCodecContext context, final TurnManagerType<P> type, final NbtCompound nbt) {
            final Optional<P> parameter = type.parameterCodec(context).parse(NbtOps.INSTANCE, nbt.get("parameter")).result();
            if (parameter.isEmpty()) {
                return Optional.empty();
            }
            return Optional.of(new TurnManagerContainer<>(type, parameter.get()));
        }

        public NbtCompound toNbt(final BattleCodecContext context) {
            final NbtCompound nbt = new NbtCompound();
            nbt.putString("type", Tbcexv4Registries.TurnManagerTypes.REGISTRY.getId(type).toString());
            nbt.put("parameter", type.parameterCodec(context).encodeStart(NbtOps.INSTANCE, parameter).getOrThrow(false, err -> Tbcexv4.LOGGER.error("Error while saving TurnManager Paramter: {}", err)));
            return nbt;
        }
    }
}
