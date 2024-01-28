package io.github.stuff_stuffs.tbcexv4.common.impl.battle.participant;

import com.mojang.datafixers.util.Unit;
import io.github.stuff_stuffs.tbcexv4.common.api.Tbcexv4Registries;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.BattlePos;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.BattleParticipant;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.BattleParticipantBounds;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.BattleParticipantHandle;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.BattleParticipantPhase;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.attachment.BattleParticipantAttachment;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.attachment.BattleParticipantAttachmentType;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.damage.DamageType;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.inventory.Inventory;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.pathing.Pather;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.stat.DamageResistanceStat;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.stat.Stat;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.stat.StatContainer;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.stat.StatModificationPhase;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.team.BattleParticipantTeam;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.state.BattleState;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.tracer.BattleTracer;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.transaction.BattleTransactionContext;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.transaction.DeltaSnapshotParticipant;
import io.github.stuff_stuffs.tbcexv4.common.api.event.EventMap;
import io.github.stuff_stuffs.tbcexv4.common.api.util.Result;
import io.github.stuff_stuffs.tbcexv4.common.generated_events.participant.BasicParticipantEvents;
import io.github.stuff_stuffs.tbcexv4.common.generated_events.participant.PostAddModifierEvent;
import io.github.stuff_stuffs.tbcexv4.common.generated_traces.participant.*;
import io.github.stuff_stuffs.tbcexv4.common.impl.battle.participant.inventory.InventoryImpl;
import io.github.stuff_stuffs.tbcexv4.common.impl.battle.participant.pather.CollisionChecker;
import io.github.stuff_stuffs.tbcexv4.common.impl.battle.participant.stat.StatContainerImpl;
import io.github.stuff_stuffs.tbcexv4.common.impl.battle.state.BattleStateImpl;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

public class BattleParticipantImpl extends DeltaSnapshotParticipant<BattleParticipantImpl.Delta> implements BattleParticipant {
    private final UUID id;
    private final EventMap events;
    private final BattleStateImpl battleState;
    private final StatContainer stats;
    private final Map<BattleParticipantAttachmentType<?, ?>, BattleParticipantAttachment> attachments;
    private final InventoryImpl inventory;
    private double health;
    private BattleParticipantBounds bounds;
    private BattlePos pos;
    private BattleParticipantPhase phase;
    private CollisionChecker cachedCollisionChecker = null;

    public BattleParticipantImpl(final UUID id, final EventMap events, final BattleStateImpl battleState, final Consumer<BattleParticipantAttachment.Builder> builder, final BattleParticipantBounds bounds, final BattlePos pos, final BattleTransactionContext transactionContext) {
        this.id = id;
        this.events = events;
        this.battleState = battleState;
        this.bounds = bounds;
        this.pos = pos;
        stats = new StatContainerImpl(this);
        phase = BattleParticipantPhase.INIT;
        attachments = new Reference2ObjectOpenHashMap<>();
        inventory = new InventoryImpl(this);
        builder.accept(new BattleParticipantAttachment.Builder() {
            @Override
            public <T extends BattleParticipantAttachment> void accept(final T value, final BattleParticipantAttachmentType<?, T> type) {
                if (attachments.putIfAbsent(type, value) != null) {
                    throw new RuntimeException();
                }
            }
        });
        events().registerMut(BasicParticipantEvents.POST_ADD_MODIFIER_EVENT_KEY, new PostAddModifierEvent() {
            @Override
            public <T> void onPostAddModifierEvent(final BattleParticipant participant, final Stat<T> stat, final StatModificationPhase phase, final T oldValue, final T newValue, final BattleTransactionContext transactionContext, final BattleTracer.Span<?> trace) {
                if (participant.phase() == BattleParticipantPhase.BATTLE && stat == Tbcexv4Registries.Stats.MAX_HEALTH) {
                    if (((Double) newValue) <= 0.0001) {
                        tryKill(transactionContext, trace);
                    }
                }
            }
        }, transactionContext);
    }

    public void finish(final BattleTransactionContext context) {
        if (phase == BattleParticipantPhase.FINISHED) {
            throw new RuntimeException();
        }
        delta(context, new PhaseDelta(phase));
        phase = BattleParticipantPhase.FINISHED;
    }

    private void tryKill(final BattleTransactionContext transactionContext, final BattleTracer.Span<?> span) {
        if (phase() == BattleParticipantPhase.FINISHED) {
            throw new RuntimeException();
        }
        final Result<Unit, BattleState.RemoveParticipantError> result = battleState.removeParticipant(handle(), BattleState.RemoveParticipantReason.DEAD, transactionContext, span);
        if (result instanceof Result.Failure<Unit, BattleState.RemoveParticipantError>) {
            if (health() <= 0.0001) {
                throw new RuntimeException();
            }
        }
    }

    public void start(final BattleTransactionContext context, final BattleTracer.Span<?> tracer) {
        if (phase != BattleParticipantPhase.INIT) {
            throw new RuntimeException();
        }
        phase = BattleParticipantPhase.BATTLE;
        tracer.push(new SetParticipantPosTrace(handle(), pos), context).close();
        for (final Map.Entry<BattleParticipantAttachmentType<?, ?>, BattleParticipantAttachment> entry : attachments.entrySet()) {
            final BattleParticipantAttachmentType<?, ?> type = entry.getKey();
            final BattleParticipantAttachment attachment = entry.getValue();
            if (attachment != null) {
                tracer.push(new SetParticipantAttachmentTrace(handle(), type, attachment.traceSnapshot()), context).close();
                attachment.init(this, context, tracer);
            }
        }
    }

    @Override
    public BattleParticipantPhase phase() {
        return phase;
    }

    @Override
    public EventMap events() {
        return events;
    }

    @Override
    public BattleState battleState() {
        return battleState;
    }

    @Override
    public StatContainer stats() {
        return stats;
    }

    @Override
    public Inventory inventory() {
        return inventory;
    }

    @Override
    public double health() {
        return Math.min(health, stats().get(Tbcexv4Registries.Stats.MAX_HEALTH));
    }

    @Override
    public <V, T extends BattleParticipantAttachment> Optional<V> attachmentView(final BattleParticipantAttachmentType<V, T> type) {
        return attachment(type).map(type::view);
    }

    @Override
    public <T extends BattleParticipantAttachment> Optional<T> attachment(final BattleParticipantAttachmentType<?, T> type) {
        //noinspection unchecked
        return Optional.ofNullable((T) attachments.getOrDefault(type, null));
    }

    @Override
    public BattleParticipantBounds bounds() {
        return bounds;
    }

    @Override
    public BattlePos pos() {
        return pos;
    }

    @Override
    public BattleParticipantTeam team() {
        return battleState.team(handle());
    }

    @Override
    public BattleParticipantHandle handle() {
        return new BattleParticipantHandle(id);
    }

    @Override
    public Result<Unit, SetBoundsError> setBounds(final BattleParticipantBounds bounds, final BattleTransactionContext transactionContext, final BattleTracer.Span<?> tracer) {
        battleState.ensureBattleOngoing();
        try (final var preSpan = tracer.push(new PreSetParticipantBoundsTrace(handle(), bounds), transactionContext)) {
            if (bounds.equals(this.bounds)) {
                return new Result.Success<>(Unit.INSTANCE);
            }
            if (!battleState.bounds().check(bounds, pos)) {
                return new Result.Failure<>(SetBoundsError.OUTSIDE_BATTLE);
            }
            if (!events().invoker(BasicParticipantEvents.PRE_SET_BOUNDS_EVENT_KEY, transactionContext).onPreSetBoundsEvent(this, bounds, transactionContext, preSpan)) {
                return new Result.Failure<>(SetBoundsError.EVENT);
            }
            final BattleParticipantBounds old = this.bounds;
            delta(transactionContext, new BoundsDelta(old));
            cachedCollisionChecker = null;
            this.bounds = bounds;
            try (final var span = preSpan.push(new SetParticipantBoundsTrace(handle(), old, bounds), transactionContext)) {
                events().invoker(BasicParticipantEvents.POST_SET_BOUNDS_EVENT_KEY, transactionContext).onPostSetBoundsEvent(this, old, transactionContext, span);
            }
            return new Result.Success<>(Unit.INSTANCE);
        }
    }

    @Override
    public Result<Unit, SetPosError> setPos(final BattlePos pos, final BattleTransactionContext transactionContext, final BattleTracer.Span<?> tracer) {
        battleState.ensureBattleOngoing();
        try (final var preSpan = tracer.push(new PreSetParticipantPosTrace(handle(), pos), transactionContext)) {
            if (pos.equals(this.pos)) {
                return new Result.Success<>(Unit.INSTANCE);
            }
            if (!battleState.bounds().check(bounds, pos)) {
                return new Result.Failure<>(SetPosError.OUTSIDE_BATTLE);
            }
            if (!collisionChecker().check(pos.x(), pos.y(), pos.z(), Double.NaN)) {
                return new Result.Failure<>(SetPosError.ENV_COLLISION);
            }
            if (!events().invoker(BasicParticipantEvents.PRE_SET_POS_EVENT_KEY, transactionContext).onPreSetPosEvent(this, pos, transactionContext, preSpan)) {
                return new Result.Failure<>(SetPosError.EVENT);
            }
            final BattlePos oldPos = this.pos;
            delta(transactionContext, new PosDelta(oldPos));
            this.pos = pos;
            try (final var span = preSpan.push(new SetParticipantPosTrace(handle(), pos), transactionContext)) {
                events().invoker(BasicParticipantEvents.POST_SET_POS_EVENT_KEY, transactionContext).onPostSetPosEvent(this, oldPos, transactionContext, span);
            }
            return new Result.Success<>(Unit.INSTANCE);
        }
    }

    @Override
    public double damage(final double amount, final DamageType type, final BattleTransactionContext transactionContext, final BattleTracer.Span<?> tracer) {
        battleState.ensureBattleOngoing();
        try (final var preSpan = tracer.push(new PreDamageParticipantTrace(handle(), amount), transactionContext)) {
            double modified = events().invoker(BasicParticipantEvents.PRE_DAMAGE_EVENT_KEY, transactionContext).onPreDamageEvent(this, amount, type, transactionContext, preSpan);
            final double resistance = stats.get(DamageResistanceStat.of(type));
            modified = modified * (1 - resistance);
            if (modified <= 0.0 || !Double.isFinite(modified)) {
                return 0.0;
            }
            final double oldHealth = health();
            delta(transactionContext, new HealthDelta(oldHealth));
            health = Math.max(oldHealth - modified, 0.0);
            final double min = Math.min(oldHealth, modified);
            try (final var span = preSpan.push(new DamageParticipantTrace(handle(), min), transactionContext)) {
                events().invoker(BasicParticipantEvents.POST_DAMAGE_EVENT_KEY, transactionContext).onPostDamageEvent(this, min, type, modified - min, transactionContext, span);
            }
            if (health() <= 0) {
                tryKill(transactionContext, preSpan);
            }
            return min;
        }
    }

    @Override
    public double heal(final double amount, final BattleTransactionContext transactionContext, final BattleTracer.Span<?> tracer) {
        battleState.ensureBattleOngoing();
        try (final var preSpan = tracer.push(new PreHealParticipantTrace(handle(), amount), transactionContext)) {
            final double modified = events().invoker(BasicParticipantEvents.PRE_HEAL_EVENT_KEY, transactionContext).onPreHealEvent(this, amount, transactionContext, preSpan);
            if (modified <= 0.0 || !Double.isFinite(modified)) {
                return 0.0;
            }
            final double oldHealth = health;
            delta(transactionContext, new HealthDelta(oldHealth));
            health = Math.min(oldHealth + modified, stats().get(Tbcexv4Registries.Stats.MAX_HEALTH));
            final double overflow = Math.max(modified - (health - oldHealth), 0.0);
            final double healed = Math.max(health - oldHealth, 0.0);
            try (final var span = preSpan.push(new HealParticipantTrace(handle(), modified, overflow), transactionContext)) {
                events().invoker(BasicParticipantEvents.POST_HEAL_EVENT_KEY, transactionContext).onPostHealEvent(this, healed, overflow, transactionContext, span);
            }
            return healed;
        }
    }

    @Override
    public double setHealth(final double amount, final BattleTransactionContext transactionContext, final BattleTracer.Span<?> tracer) {
        battleState.ensureBattleOngoing();
        try (final var preSpan = tracer.push(new PreParticipantSetHealthTrace(handle(), amount), transactionContext)) {
            final double modified = events().invoker(BasicParticipantEvents.PRE_SET_HEALTH_EVENT_KEY, transactionContext).onPreSetHealthEvent(this, amount, transactionContext, preSpan);
            if (!Double.isFinite(modified)) {
                return health();
            }
            final double oldHealth = health();
            delta(transactionContext, new HealthDelta(oldHealth));
            health = modified;
            try (final var span = preSpan.push(new ParticipantSetHealthTrace(handle(), oldHealth, health()), transactionContext)) {
                events().invoker(BasicParticipantEvents.POST_SET_HEALTH_EVENT_KEY, transactionContext).onPostSetHealthEvent(this, oldHealth, transactionContext, span);
            }
            if (health() <= 0.0001) {
                tryKill(transactionContext, preSpan);
            }
            return health();
        }
    }

    @Override
    public Result<Pather.PathNode, MoveError> move(final Pather.PathNode node, final BattleTransactionContext transactionContext, final BattleTracer.Span<?> tracer) {
        battleState.ensureBattleOngoing();
        try (final var preSpan = tracer.push(new PreMoveParticipantTrace(handle(), node), transactionContext)) {
            final Optional<Pather.PathNode> event = events.invoker(BasicParticipantEvents.PRE_MOVE_EVENT_KEY, transactionContext).onPreMoveEvent(this, Optional.of(node), transactionContext, preSpan);
            if (event.isEmpty()) {
                return Result.failure(MoveError.EVENT);
            }
            final Pather.PathNode pathNode = event.get();
            Pather.PathNode cursor = pathNode;
            while (cursor != null) {
                final BattlePos battlePos = cursor.pos();
                if (cachedCollisionChecker.inBounds(battlePos.x(), battlePos.y(), battlePos.z())) {
                    return Result.failure(MoveError.OUTSIDE_BATTLE);
                }
                if (cachedCollisionChecker.check(battlePos.x(), battlePos.y(), battlePos.z(), Double.NaN)) {
                    return Result.failure(MoveError.ENV_COLLISION);
                }
                cursor = cursor.prev();
            }
            delta(transactionContext, new PosDelta(pos));
            pos = new BattlePos(node.pos().x(), node.pos().y(), node.pos().z());
            try (final var span = preSpan.push(new MoveParticipantTrace(handle(), node), transactionContext)) {
                events.invoker(BasicParticipantEvents.MOVE_EVENT_KEY, transactionContext).onMoveEvent(this, pathNode, transactionContext, span);
            }
            return new Result.Success<>(node);
        }
    }

    @Override
    public <T extends BattleParticipantAttachment> void setAttachment(final @Nullable T value, final BattleParticipantAttachmentType<?, T> type, final BattleTransactionContext transactionContext, final BattleTracer.Span<?> tracer) {
        try (final var span = tracer.push(new SetParticipantAttachmentTrace(handle(), type, value == null ? null : value.traceSnapshot()), transactionContext)) {
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

    public CollisionChecker collisionChecker() {
        if (cachedCollisionChecker == null) {
            cachedCollisionChecker = new CollisionChecker(bounds.width(), bounds.height(), battleState.bounds(), battleState.environment().asBlockView());
        }
        return cachedCollisionChecker;
    }

    @Override
    protected void revertDelta(final Delta delta) {
        delta.apply(this);
    }

    public sealed interface Delta {
        void apply(BattleParticipantImpl participant);
    }

    private record PhaseDelta(BattleParticipantPhase phase) implements Delta {
        @Override
        public void apply(final BattleParticipantImpl participant) {
            participant.phase = phase;
        }
    }

    private record HealthDelta(double health) implements Delta {
        @Override
        public void apply(final BattleParticipantImpl participant) {
            participant.health = health;
        }
    }

    private record PosDelta(BattlePos pos) implements Delta {
        @Override
        public void apply(final BattleParticipantImpl participant) {
            participant.pos = pos;
        }
    }

    private record BoundsDelta(BattleParticipantBounds bounds) implements Delta {
        @Override
        public void apply(final BattleParticipantImpl participant) {
            participant.bounds = bounds;
            participant.cachedCollisionChecker = null;
        }
    }

    private record SetAttachmentDelta<T extends BattleParticipantAttachment>(
            @Nullable T previous,
            BattleParticipantAttachmentType<?, T> type
    ) implements Delta {
        @Override
        public void apply(final BattleParticipantImpl participant) {
            if (previous == null) {
                participant.attachments.remove(type);
            } else {
                participant.attachments.put(type, previous);
            }
        }
    }
}
