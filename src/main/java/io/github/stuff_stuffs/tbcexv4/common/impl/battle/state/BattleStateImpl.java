package io.github.stuff_stuffs.tbcexv4.common.impl.battle.state;

import com.mojang.datafixers.util.Pair;
import com.mojang.datafixers.util.Unit;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.*;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.BattleParticipant;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.BattleParticipantBounds;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.BattleParticipantHandle;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.BattleParticipantInitialState;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.team.BattleParticipantTeam;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.team.BattleParticipantTeamRelation;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.state.BattleState;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.state.attachment.BattleAttachment;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.state.attachment.BattleAttachmentType;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.state.env.BattleEnvironment;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.tracer.BattleTracer;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.tracer.event.CoreBattleTraceEvents;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.transaction.BattleTransactionContext;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.transaction.BattleTransactionManager;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.transaction.DeltaSnapshotParticipant;
import io.github.stuff_stuffs.tbcexv4.common.api.event.EventMap;
import io.github.stuff_stuffs.tbcexv4.common.api.util.Result;
import io.github.stuff_stuffs.tbcexv4.common.generated_events.BasicEvents;
import io.github.stuff_stuffs.tbcexv4.common.impl.battle.ServerBattleImpl;
import io.github.stuff_stuffs.tbcexv4.common.impl.battle.participant.BattleParticipantImpl;
import io.github.stuff_stuffs.tbcexv4.common.impl.battle.transaction.BattleTransactionManagerImpl;
import io.github.stuff_stuffs.tbcexv4.common.internal.BattleManager;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public class BattleStateImpl extends DeltaSnapshotParticipant<BattleStateImpl.Delta> implements BattleState {
    private final BattleEnvironment environment;
    private final RegistryKey<World> sourceWorld;
    private final EventMap events;
    private final BattleView battle;
    private final BattleParticipantContainer participantContainer;
    private final EventMap.Builder participantEvents;
    private final BattleTransactionManager transactionManager;
    private final Map<BattleAttachmentType<?>, BattleAttachment> attachments;
    private BattlePhase phase;
    private BattleBounds bounds;

    public BattleStateImpl(final Battle battle, final BattleEnvironment environment, final RegistryKey<World> sourceWorld, final EventMap.Builder builder, final EventMap.Builder participantEvents) {
        this.environment = environment;
        this.sourceWorld = sourceWorld;
        events = builder.build();
        this.battle = battle;
        this.participantEvents = participantEvents;
        participantContainer = new BattleParticipantContainer();
        transactionManager = new BattleTransactionManagerImpl();
        attachments = new Reference2ObjectOpenHashMap<>();
        phase = BattlePhase.INIT;
        bounds = new BattleBounds(0, 0, 0, battle.xSize(), battle.ySize(), battle.zSize());
    }

    public void ensureBattleOngoing() {
        if (phase == BattlePhase.FINISHED) {
            throw new RuntimeException();
        }
    }

    public void start(final BattleTransactionContext transactionContext) {
        if (phase != BattlePhase.INIT) {
            throw new RuntimeException();
        }
        phase = BattlePhase.BATTLE;
        delta(transactionContext, new PhaseDelta(BattlePhase.INIT));
    }

    @Override
    public BattleEnvironment environment() {
        return environment;
    }

    @Override
    public BattleParticipant participant(final BattleParticipantHandle handle) {
        return participantContainer.participants.get(handle);
    }

    @Override
    public <T extends BattleAttachment> Optional<T> attachment(final BattleAttachmentType<? extends T> type) {
        //noinspection unchecked
        return Optional.ofNullable((T) attachments.getOrDefault(type, null));
    }

    @Override
    public Result<Unit, RemoveParticipantError> removeParticipant(final BattleParticipantHandle handle, final RemoveParticipantReason reason, final BattleTransactionContext transactionContext, final BattleTracer.Span<?> tracer) {
        ensureBattleOngoing();
        try (final var preSpan = tracer.push(new CoreBattleTraceEvents.PreRemoveParticipant(handle, reason), transactionContext)) {
            if (!participantContainer.participants.containsKey(handle)) {
                return new Result.Failure<>(RemoveParticipantError.MISSING_PARTICIPANT);
            }
            if (!events().invoker(BasicEvents.PRE_REMOVE_PARTICIPANT_EVENT_KEY).onPreRemoveParticipantEvent(participantContainer.participants.get(handle), reason, transactionContext, preSpan)) {
                return new Result.Failure<>(RemoveParticipantError.EVENT);
            }
            final BattleParticipantImpl participant = participantContainer.participants.get(handle);
            if (participant != null) {
                participant.finish(transactionContext);
                delta(transactionContext, new RemoveParticipantDelta(participant));
            }
            participantContainer.remove(handle);
            try (final var span = preSpan.push(new CoreBattleTraceEvents.RemoveParticipant(handle, reason), transactionContext)) {
                events().invoker(BasicEvents.POST_REMOVE_PARTICIPANT_EVENT_KEY).onPostRemoveParticipantEvent(this, handle, reason, transactionContext, span);
            }
            return new Result.Success<>(Unit.INSTANCE);
        }
    }

    @Override
    public Result<BattleParticipantHandle, AddParticipantError> addParticipant(final BattleParticipantInitialState battleParticipant, final BattleTransactionContext transactionContext, final BattleTracer.Span<?> tracer) {
        ensureBattleOngoing();
        final Optional<UUID> id = battleParticipant.id();
        final BattleParticipantHandle handle = new BattleParticipantHandle(id.orElseGet(MathHelper::randomUuid));
        try (final var preSpan = tracer.push(new CoreBattleTraceEvents.PreAddParticipant(handle), transactionContext)) {
            if (id.isPresent() && participantContainer.participants.containsKey(new BattleParticipantHandle(id.get()))) {
                return new Result.Failure<>(AddParticipantError.ID_OVERLAP);
            }
            final BattleParticipantBounds bounds = battleParticipant.bounds();
            final BattlePos pos = battleParticipant.pos();
            if (!this.bounds.check(bounds, pos)) {
                return new Result.Failure<>(AddParticipantError.OUT_OF_BOUNDS);
            }
            if (!events().invoker(BasicEvents.PRE_ADD_PARTICIPANT_EVENT_KEY).onPreAddParticipantEvent(this, battleParticipant, handle, transactionContext, preSpan)) {
                return new Result.Failure<>(AddParticipantError.EVENT);
            }
            if (participantContainer.participants.containsKey(handle)) {
                return new Result.Failure<>(AddParticipantError.UNKNOWN);
            }
            if (battle instanceof ServerBattleImpl serverBattle) {
                final BattleManager manager = serverBattle.world().battleManager();
                manager.addBattle(handle.id(), battle.handle());
            }
            delta(transactionContext, new AddParticipantDelta(handle));
            final BattleParticipantImpl participant = new BattleParticipantImpl(handle.id(), participantEvents.build(), this, battleParticipant::addAttachments, bounds, pos, transactionContext);
            participantContainer.participants.put(participant.handle(), participant);
            participantContainer.teams.put(participant.handle(), battleParticipant.team());
            participantContainer.byTeam.computeIfAbsent(battleParticipant.team(), k -> new ObjectOpenHashSet<>()).add(participant.handle());
            battleParticipant.initialize(this, participant, transactionContext, preSpan);
            participant.start();
            try (final var span = preSpan.push(new CoreBattleTraceEvents.AddParticipant(participant.handle()), transactionContext)) {
                events().invoker(BasicEvents.POST_ADD_PARTICIPANT_EVENT_KEY).onPostAddParticipantEvent(this, participant, transactionContext, span);
            }
            return new Result.Success<>(participant.handle());
        }
    }

    @Override
    public BattleTransactionManager transactionManager() {
        return transactionManager;
    }

    @Override
    public RegistryKey<World> sourceWorld() {
        return sourceWorld;
    }

    @Override
    public EventMap events() {
        return events;
    }

    @Override
    public BattleBounds bounds() {
        return bounds;
    }

    @Override
    public BattleParticipantTeamRelation relation(final BattleParticipantTeam first, final BattleParticipantTeam second) {
        return participantContainer.relation(first, second);
    }

    @Override
    public Set<BattleParticipantHandle> participants() {
        return Set.copyOf(participantContainer.participants.keySet());
    }

    @Override
    public Set<BattleParticipantHandle> participants(final BattleParticipantTeam team) {
        return participantContainer.byTeam.getOrDefault(team, Set.of());
    }

    @Override
    public Result<Unit, SetBoundsError> setBounds(final BattleBounds bounds, final BattleTransactionContext transactionContext, final BattleTracer.Span<?> tracer) {
        ensureBattleOngoing();
        try (final var preSpan = tracer.push(new CoreBattleTraceEvents.PreSetBounds(bounds), transactionContext)) {
            if (bounds.equals(this.bounds)) {
                return new Result.Success<>(Unit.INSTANCE);
            }
            if (bounds.x1() > battle.xSize() || bounds.y1() > battle.ySize() || bounds.z1() > battle.zSize()) {
                return new Result.Failure<>(SetBoundsError.TOO_BIG);
            }
            for (final BattleParticipant value : participantContainer.participants.values()) {
                if (!bounds.check(value.bounds(), value.pos())) {
                    return new Result.Failure<>(SetBoundsError.PARTICIPANT_OUTSIDE);
                }
            }
            if (!events().invoker(BasicEvents.PRE_SET_BOUNDS_EVENT_KEY).onPreSetBoundsEvent(this, bounds, transactionContext, preSpan)) {
                return new Result.Failure<>(SetBoundsError.EVENT);
            }
            final BattleBounds oldBounds = this.bounds;
            this.bounds = bounds;
            delta(transactionContext, new BoundsDelta(oldBounds));
            try (final var span = preSpan.push(new CoreBattleTraceEvents.SetBounds(oldBounds, bounds), transactionContext)) {
                events().invoker(BasicEvents.POST_SET_BOUNDS_EVENT_KEY).onPostSetBoundsEvent(this, oldBounds, transactionContext, span);
            }
            return new Result.Success<>(Unit.INSTANCE);
        }
    }

    @Override
    public Result<Unit, SetTeamRelationError> setRelation(final BattleParticipantTeam first, final BattleParticipantTeam second, final BattleParticipantTeamRelation relation, final BattleTransactionContext transactionContext, final BattleTracer.Span<?> tracer) {
        ensureBattleOngoing();
        try (final var preSpan = tracer.push(new CoreBattleTraceEvents.PreSetTeamRelation(first, second, relation), transactionContext)) {
            final BattleParticipantTeamRelation oldRelation = participantContainer.relation(first, second);
            if (oldRelation == relation) {
                return new Result.Success<>(Unit.INSTANCE);
            }
            if (!events().invoker(BasicEvents.PRE_SET_TEAM_RELATION_EVENT_KEY).onPreSetTeamRelationEvent(this, first, second, relation, transactionContext, preSpan)) {
                return new Result.Failure<>(SetTeamRelationError.EVENT);
            }
            final BattleParticipantTeamRelation old = participantContainer.setRelation(first, second, relation);
            delta(transactionContext, new TeamRelationDelta(first, second, oldRelation));
            try (final var span = preSpan.push(new CoreBattleTraceEvents.SetTeamRelation(first, second, oldRelation, relation), transactionContext)) {
                events().invoker(BasicEvents.POST_SET_TEAM_RELATION_EVENT_KEY).onPostSetTeamRelationEvent(this, first, second, old, transactionContext, span);
            }
            return new Result.Success<>(Unit.INSTANCE);
        }
    }

    @Override
    public <T extends BattleAttachment> void setAttachment(final @Nullable T value, final BattleAttachmentType<T> type, final BattleTransactionContext transactionContext, final BattleTracer.Span<?> tracer) {
        try (final var span = tracer.push(new CoreBattleTraceEvents.SetAttachment(type), transactionContext)) {
            //noinspection unchecked
            final T old = (T) attachments.put(type, value);
            delta(transactionContext, new SetAttachmentDelta<>(old, type));
            if (old != null) {
                old.deinit(this, transactionContext, span);
            }
            if (value != null) {
                value.init(this, transactionContext, span);
            }
        }
    }

    @Override
    public BattlePhase phase() {
        return phase;
    }

    public BattleParticipantTeam team(final BattleParticipantHandle handle) {
        return participantContainer.teams.get(handle);
    }

    @Override
    protected void revertDelta(final Delta delta) {
        delta.apply(this);
    }

    private static final class BattleParticipantContainer {
        private final Map<BattleParticipantHandle, BattleParticipantImpl> participants;
        private final Map<BattleParticipantHandle, BattleParticipantTeam> teams;
        private final Map<BattleParticipantTeam, Set<BattleParticipantHandle>> byTeam;
        private final Map<Pair<BattleParticipantTeam, BattleParticipantTeam>, BattleParticipantTeamRelation> relations;

        public BattleParticipantContainer() {
            participants = new Object2ObjectLinkedOpenHashMap<>();
            teams = new Object2ObjectOpenHashMap<>();
            byTeam = new Object2ObjectLinkedOpenHashMap<>();
            relations = new Object2ObjectOpenHashMap<>();
        }

        public BattleParticipantTeamRelation relation(final BattleParticipantTeam first, final BattleParticipantTeam second) {
            return relations.getOrDefault(Pair.of(first, second), BattleParticipantTeamRelation.NEUTRAL);
        }

        public BattleParticipantTeamRelation setRelation(final BattleParticipantTeam first, final BattleParticipantTeam second, final BattleParticipantTeamRelation relation) {
            relations.put(Pair.of(first, second), relation);
            final BattleParticipantTeamRelation r = relations.put(Pair.of(second, first), relation);
            return r != null ? r : BattleParticipantTeamRelation.NEUTRAL;
        }

        public void remove(final BattleParticipantHandle handle) {
            participants.remove(handle);
            final BattleParticipantTeam team = teams.remove(handle);
            byTeam.get(team).remove(handle);
        }
    }

    public sealed interface Delta {
        void apply(BattleStateImpl state);
    }

    private record BoundsDelta(BattleBounds bounds) implements Delta {
        @Override
        public void apply(final BattleStateImpl state) {
            state.bounds = bounds;
        }
    }

    private record TeamRelationDelta(BattleParticipantTeam first, BattleParticipantTeam second,
                                     BattleParticipantTeamRelation relation) implements Delta {
        @Override
        public void apply(final BattleStateImpl state) {
            state.participantContainer.setRelation(first, second, relation);
        }
    }

    private record AddParticipantDelta(BattleParticipantHandle handle) implements Delta {
        @Override
        public void apply(final BattleStateImpl state) {
            state.participantContainer.remove(handle);
            if (state.battle instanceof ServerBattleImpl serverBattle) {
                final BattleManager manager = serverBattle.world().battleManager();
                manager.removeBattle(handle.id(), state.battle.handle());
            }
        }
    }

    private record RemoveParticipantDelta(BattleParticipantImpl participant) implements Delta {
        @Override
        public void apply(final BattleStateImpl state) {
            state.participantContainer.participants.put(participant.handle(), participant);
            state.participantContainer.teams.put(participant.handle(), participant.team());
            state.participantContainer.byTeam.computeIfAbsent(participant.team(), k -> new ObjectOpenHashSet<>()).add(participant.handle());
            if (state.battle instanceof ServerBattleImpl serverBattle) {
                final BattleManager manager = serverBattle.world().battleManager();
                manager.addBattle(participant.handle().id(), state.battle.handle());
            }
        }
    }

    private record PhaseDelta(BattlePhase phase) implements Delta {
        @Override
        public void apply(final BattleStateImpl state) {
            if (state.battle instanceof ServerBattleImpl serverBattle) {
                if (state.phase == BattlePhase.FINISHED) {
                    for (final BattleParticipantHandle handle : state.participants()) {
                        serverBattle.world().battleManager().unresolveBattle(handle.id(), state.battle.handle());
                    }
                } else if (phase == BattlePhase.FINISHED) {
                    for (final BattleParticipantHandle handle : state.participants()) {
                        serverBattle.world().battleManager().resolveBattle(handle.id(), state.battle.handle());
                    }
                }
            }
            state.phase = phase;
        }
    }

    private record SetAttachmentDelta<T extends BattleAttachment>(@Nullable T previous,
                                                                  BattleAttachmentType<T> type) implements Delta {
        @Override
        public void apply(final BattleStateImpl state) {
            if (previous == null) {
                state.attachments.remove(type);
            } else {
                state.attachments.put(type, previous);
            }
        }
    }
}
