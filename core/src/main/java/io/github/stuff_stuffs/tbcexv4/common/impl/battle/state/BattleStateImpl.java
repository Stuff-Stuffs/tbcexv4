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
import io.github.stuff_stuffs.tbcexv4.common.api.battle.transaction.BattleTransactionContext;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.transaction.BattleTransactionManager;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.transaction.DeltaSnapshotParticipant;
import io.github.stuff_stuffs.tbcexv4.common.api.event.EventMap;
import io.github.stuff_stuffs.tbcexv4.common.api.util.Result;
import io.github.stuff_stuffs.tbcexv4.common.generated_events.BasicEvents;
import io.github.stuff_stuffs.tbcexv4.common.generated_traces.*;
import io.github.stuff_stuffs.tbcexv4.common.generated_traces.participant.*;
import io.github.stuff_stuffs.tbcexv4.common.impl.battle.participant.BattleParticipantImpl;
import io.github.stuff_stuffs.tbcexv4.common.impl.battle.transaction.BattleTransactionManagerImpl;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.math.random.Xoroshiro128PlusPlusRandom;
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
    private final Map<BattleAttachmentType<?, ?>, BattleAttachment> attachments;
    private final Random random;
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
        random = new Xoroshiro128PlusPlusRandom(sourceWorld.getValue().hashCode(), battle.handle().id().hashCode());
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

    public void finish(final BattleTransactionContext transactionContext, final BattleTracer.Span<?> tracer) {
        if (phase != BattlePhase.BATTLE) {
            throw new RuntimeException();
        }
        phase = BattlePhase.FINISHED;
        delta(transactionContext, new PhaseDelta(BattlePhase.BATTLE));
        for (final BattleParticipantImpl participant : participantContainer.participants.values()) {
            participant.finish(transactionContext);
        }
        events.invoker(BasicEvents.END_BATTLE_KEY, transactionContext).onEndBattle(this, transactionContext, tracer);
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
    public <V, T extends BattleAttachment> Optional<V> attachmentView(final BattleAttachmentType<V, T> type) {
        return attachment(type).map(type::view);
    }

    @Override
    public <T extends BattleAttachment> Optional<T> attachment(final BattleAttachmentType<?, T> type) {
        return Optional.ofNullable((T) attachments.get(type));
    }

    @Override
    public Result<Unit, RemoveParticipantError> removeParticipant(final BattleParticipantHandle handle, final RemoveParticipantReason reason, final BattleTransactionContext transactionContext, final BattleTracer.Span<?> tracer) {
        ensureBattleOngoing();
        try (final var preSpan = tracer.push(new PreRemoveParticipantTrace(handle, reason), transactionContext)) {
            if (!participantContainer.participants.containsKey(handle)) {
                return new Result.Failure<>(RemoveParticipantError.MISSING_PARTICIPANT);
            }
            if (!events().invoker(BasicEvents.PRE_REMOVE_PARTICIPANT_EVENT_KEY, transactionContext).onPreRemoveParticipantEvent(participantContainer.participants.get(handle), reason, transactionContext, preSpan)) {
                return new Result.Failure<>(RemoveParticipantError.EVENT);
            }
            final BattleParticipantImpl participant = participantContainer.participants.get(handle);
            if (participant != null) {
                participant.finish(transactionContext);
                delta(transactionContext, new RemoveParticipantDelta(participant, participant.team()));
                participantContainer.remove(handle);
                try (final var span = preSpan.push(new RemoveParticipantTrace(handle, reason), transactionContext)) {
                    events().invoker(BasicEvents.POST_REMOVE_PARTICIPANT_EVENT_KEY, transactionContext).onPostRemoveParticipantEvent(this, handle, reason, transactionContext, span);
                }
                return new Result.Success<>(Unit.INSTANCE);
            }
            return new Result.Failure<>(RemoveParticipantError.EVENT);
        }
    }

    @Override
    public Result<BattleParticipantHandle, AddParticipantError> addParticipant(final BattleParticipantInitialState battleParticipant, final BattleTransactionContext transactionContext, final BattleTracer.Span<?> tracer) {
        ensureBattleOngoing();
        final Optional<UUID> id = battleParticipant.id();
        final BattleParticipantHandle handle = new BattleParticipantHandle(id.orElseGet(() -> new UUID(random.nextLong(), random.nextLong())));
        try (final var preSpan = tracer.push(new PreAddParticipantTrace(handle), transactionContext)) {
            if (id.isPresent() && participantContainer.participants.containsKey(new BattleParticipantHandle(id.get()))) {
                return new Result.Failure<>(AddParticipantError.ID_OVERLAP);
            }
            final BattleParticipantBounds bounds = battleParticipant.bounds();
            final BattlePos pos = battleParticipant.pos();
            if (!this.bounds.check(bounds, pos)) {
                return new Result.Failure<>(AddParticipantError.OUT_OF_BOUNDS);
            }
            if (!events().invoker(BasicEvents.PRE_ADD_PARTICIPANT_EVENT_KEY, transactionContext).onPreAddParticipantEvent(this, battleParticipant, handle, transactionContext, preSpan)) {
                return new Result.Failure<>(AddParticipantError.EVENT);
            }
            if (participantContainer.participants.containsKey(handle)) {
                return new Result.Failure<>(AddParticipantError.UNKNOWN);
            }
            final BattleParticipantImpl participant = new BattleParticipantImpl(handle.id(), participantEvents.build(), this, battleParticipant::addAttachments, bounds, pos, transactionContext);
            if (!participant.collisionChecker().check(pos.x(), pos.y(), pos.z(), Double.NaN)) {
                return new Result.Failure<>(AddParticipantError.ENV_COLLISION);
            }
            delta(transactionContext, new AddParticipantDelta(handle));
            participantContainer.participants.put(participant.handle(), participant);
            participantContainer.teams.put(participant.handle(), battleParticipant.team());
            participantContainer.byTeam.computeIfAbsent(battleParticipant.team(), k -> new ObjectOpenHashSet<>()).add(participant.handle());
            try (final var span = preSpan.push(new AddParticipantTrace(participant.handle()), transactionContext)) {
                participant.start(transactionContext, span);
                battleParticipant.initialize(this, participant, transactionContext, preSpan);
                span.push(new ParticipantSetTeamTrace(handle, Optional.empty(), participant.team()), transactionContext).close();
                events().invoker(BasicEvents.POST_ADD_PARTICIPANT_EVENT_KEY, transactionContext).onPostAddParticipantEvent(this, participant, transactionContext, span);
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
        try (final var preSpan = tracer.push(new PreSetBoundsTrace(this.bounds, bounds), transactionContext)) {
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
            if (!events().invoker(BasicEvents.PRE_SET_BOUNDS_EVENT_KEY, transactionContext).onPreSetBoundsEvent(this, bounds, transactionContext, preSpan)) {
                return new Result.Failure<>(SetBoundsError.EVENT);
            }
            final BattleBounds oldBounds = this.bounds;
            this.bounds = bounds;
            delta(transactionContext, new BoundsDelta(oldBounds));
            try (final var span = preSpan.push(new SetBoundsTrace(oldBounds, bounds), transactionContext)) {
                events().invoker(BasicEvents.POST_SET_BOUNDS_EVENT_KEY, transactionContext).onPostSetBoundsEvent(this, oldBounds, transactionContext, span);
            }
            return new Result.Success<>(Unit.INSTANCE);
        }
    }

    @Override
    public Result<Unit, SetTeamRelationError> setRelation(final BattleParticipantTeam first, final BattleParticipantTeam second, final BattleParticipantTeamRelation relation, final BattleTransactionContext transactionContext, final BattleTracer.Span<?> tracer) {
        ensureBattleOngoing();
        try (final var preSpan = tracer.push(new PreSetTeamRelationTrace(first, second, relation), transactionContext)) {
            if (first.equals(second)) {
                return new Result.Failure<>(SetTeamRelationError.SAME_TEAM);
            }
            final BattleParticipantTeamRelation oldRelation = participantContainer.relation(first, second);
            if (oldRelation == relation) {
                return new Result.Success<>(Unit.INSTANCE);
            }
            if (!events().invoker(BasicEvents.PRE_SET_TEAM_RELATION_EVENT_KEY, transactionContext).onPreSetTeamRelationEvent(this, first, second, relation, transactionContext, preSpan)) {
                return new Result.Failure<>(SetTeamRelationError.EVENT);
            }
            final BattleParticipantTeamRelation old = participantContainer.setRelation(first, second, relation);
            delta(transactionContext, new TeamRelationDelta(first, second, oldRelation));
            try (final var span = preSpan.push(new SetTeamRelationTrace(first, second, oldRelation, relation), transactionContext)) {
                events().invoker(BasicEvents.POST_SET_TEAM_RELATION_EVENT_KEY, transactionContext).onPostSetTeamRelationEvent(this, first, second, old, transactionContext, span);
            }
            return new Result.Success<>(Unit.INSTANCE);
        }
    }

    @Override
    public Result<Unit, SetTeamError> setTeam(final BattleParticipantHandle handle, final BattleParticipantTeam newTeam, final BattleTransactionContext context, final BattleTracer.Span<?> tracer) {
        final BattleParticipantTeam oldTeam = participantContainer.teams.get(handle);
        if (oldTeam.equals(newTeam)) {
            return new Result.Success<>(Unit.INSTANCE);
        }
        tracer.push(new ParticipantSetTeamTrace(handle, Optional.of(oldTeam), newTeam), context).close();
        participantContainer.setTeam(handle, newTeam);
        delta(context, new SetTeamAttachment(handle, oldTeam));
        return new Result.Success<>(Unit.INSTANCE);
    }

    @Override
    public <T extends BattleAttachment> void setAttachment(final @Nullable T value, final BattleAttachmentType<?, T> type, final BattleTransactionContext transactionContext, final BattleTracer.Span<?> tracer) {
        try (final var span = tracer.push(new SetAttachmentTrace(type), transactionContext)) {
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

        public void setTeam(final BattleParticipantHandle handle, final BattleParticipantTeam team) {
            final BattleParticipantTeam oldTeam = teams.get(handle);
            if (oldTeam == null) {
                throw new NullPointerException();
            }
            teams.put(handle, team);
            byTeam.computeIfAbsent(team, l -> new ObjectOpenHashSet<>()).add(handle);
            byTeam.get(oldTeam).remove(handle);
        }

        public BattleParticipantTeamRelation relation(final BattleParticipantTeam first, final BattleParticipantTeam second) {
            if (first.equals(second)) {
                return BattleParticipantTeamRelation.ALLY;
            }
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
        }
    }

    private record RemoveParticipantDelta(BattleParticipantImpl participant,
                                          BattleParticipantTeam team) implements Delta {
        @Override
        public void apply(final BattleStateImpl state) {
            state.participantContainer.participants.put(participant.handle(), participant);
            state.participantContainer.teams.put(participant.handle(), team);
            state.participantContainer.byTeam.computeIfAbsent(team, k -> new ObjectOpenHashSet<>()).add(participant.handle());
        }
    }

    private record PhaseDelta(BattlePhase phase) implements Delta {
        @Override
        public void apply(final BattleStateImpl state) {
            state.phase = phase;
        }
    }

    private record SetAttachmentDelta<T extends BattleAttachment>(
            @Nullable T previous,
            BattleAttachmentType<?, T> type
    ) implements Delta {
        @Override
        public void apply(final BattleStateImpl state) {
            if (previous == null) {
                state.attachments.remove(type);
            } else {
                state.attachments.put(type, previous);
            }
        }
    }

    private record SetTeamAttachment(BattleParticipantHandle handle, BattleParticipantTeam team) implements Delta {
        @Override
        public void apply(final BattleStateImpl state) {
            state.participantContainer.setTeam(handle, team);
        }
    }
}
