package io.github.stuff_stuffs.tbcexv4.common.impl.battle.participant;

import com.mojang.datafixers.util.Unit;
import io.github.stuff_stuffs.tbcexv4.common.api.Tbcexv4Registries;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.BattlePos;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.BattleParticipant;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.BattleParticipantBounds;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.BattleParticipantHandle;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.BattleParticipantPhase;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.stat.Stat;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.stat.StatContainer;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.team.BattleParticipantTeam;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.state.BattleState;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.tracer.BattleTracer;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.tracer.event.CoreBattleTraceEvents;
import io.github.stuff_stuffs.tbcexv4.common.api.event.EventMap;
import io.github.stuff_stuffs.tbcexv4.common.api.util.Result;
import io.github.stuff_stuffs.tbcexv4.common.generated_events.participant.BasicParticipantEvents;
import io.github.stuff_stuffs.tbcexv4.common.generated_events.participant.PostAddModifierEvent;
import io.github.stuff_stuffs.tbcexv4.common.impl.battle.participant.stat.StatContainerImpl;
import io.github.stuff_stuffs.tbcexv4.common.impl.battle.state.BattleStateImpl;

import java.util.UUID;

public class BattleParticipantImpl implements BattleParticipant {
    private final UUID id;
    private final EventMap events;
    private final BattleStateImpl battleState;
    private final StatContainer stats;
    private double health;
    private BattleParticipantBounds bounds;
    private BattlePos pos;
    private BattleParticipantPhase phase;

    public BattleParticipantImpl(final UUID id, final EventMap events, final BattleStateImpl battleState, final BattleParticipantBounds bounds, final BattlePos pos) {
        this.id = id;
        this.events = events;
        this.battleState = battleState;
        this.bounds = bounds;
        this.pos = pos;
        stats = new StatContainerImpl(this);
        phase = BattleParticipantPhase.INIT;
        events().registerMut(BasicParticipantEvents.POST_ADD_MODIFIER_EVENT_KEY, new PostAddModifierEvent() {
            @Override
            public <T> void onPostAddModifierEvent(final BattleParticipant participant, final Stat<T> stat, final T oldValue, final T newValue, final BattleTracer.Span<?> trace) {
                if (participant.phase() == BattleParticipantPhase.BATTLE && stat == Tbcexv4Registries.Stats.MAX_HEALTH) {
                    if (((Double) newValue) <= 0.0001) {
                        tryKill(trace);
                    }
                }
            }
        });
    }

    private void tryKill(final BattleTracer.Span<?> span) {
        if (phase() == BattleParticipantPhase.FINISHED) {
            throw new RuntimeException();
        }
        final Result<Unit, BattleState.RemoveParticipantError> result = battleState.removeParticipant(handle(), BattleState.RemoveParticipantReason.DEAD, span);
        if (result instanceof Result.Failure<Unit, BattleState.RemoveParticipantError>) {
            if (health() <= 0.0001) {
                throw new RuntimeException();
            }
        }
    }

    public void start() {
        if (phase != BattleParticipantPhase.INIT) {
            throw new RuntimeException();
        }
        phase = BattleParticipantPhase.BATTLE;
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
    public double health() {
        return Math.min(health, stats().get(Tbcexv4Registries.Stats.MAX_HEALTH));
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
        return battleState.team(new BattleParticipantHandle(id));
    }

    @Override
    public BattleParticipantHandle handle() {
        return new BattleParticipantHandle(id);
    }

    @Override
    public Result<Unit, SetBoundsError> setBounds(final BattleParticipantBounds bounds, final BattleTracer.Span<?> tracer) {
        battleState.ensureBattleOngoing();
        if (bounds.equals(this.bounds)) {
            return new Result.Success<>(Unit.INSTANCE);
        }
        if (!battleState.bounds().check(bounds, pos)) {
            return new Result.Failure<>(SetBoundsError.OUTSIDE_BATTLE);
        }
        if (!events().invoker(BasicParticipantEvents.PRE_SET_BOUNDS_EVENT_KEY).onPreSetBoundsEvent(this, bounds, tracer)) {
            return new Result.Failure<>(SetBoundsError.EVENT);
        }
        final BattleParticipantBounds old = this.bounds;
        this.bounds = bounds;
        try (final var span = tracer.push(new CoreBattleTraceEvents.SetParticipantBounds(old, bounds))) {
            events().invoker(BasicParticipantEvents.POST_SET_BOUNDS_EVENT_KEY).onPostSetBoundsEvent(this, old, span);
        }
        return new Result.Success<>(Unit.INSTANCE);
    }

    @Override
    public Result<Unit, SetPosError> setPos(final BattlePos pos, final BattleTracer.Span<?> tracer) {
        battleState.ensureBattleOngoing();
        if (pos.equals(this.pos)) {
            return new Result.Success<>(Unit.INSTANCE);
        }
        if (!battleState.bounds().check(bounds, pos)) {
            return new Result.Failure<>(SetPosError.OUTSIDE_BATTLE);
        }
        if (!events().invoker(BasicParticipantEvents.PRE_SET_POS_EVENT_KEY).onPreSetPosEvent(this, pos, tracer)) {
            return new Result.Failure<>(SetPosError.EVENT);
        }
        final BattlePos oldPos = this.pos;
        this.pos = pos;
        try (final var span = tracer.push(new CoreBattleTraceEvents.SetParticipantPos(oldPos, pos))) {
            events().invoker(BasicParticipantEvents.POST_SET_POS_EVENT_KEY).onPostSetPosEvent(this, oldPos, span);
        }
        return new Result.Success<>(Unit.INSTANCE);
    }

    @Override
    public double damage(final double amount, final BattleTracer.Span<?> tracer) {
        battleState.ensureBattleOngoing();
        final double modified = events().invoker(BasicParticipantEvents.PRE_DAMAGE_EVENT_KEY).onPreDamageEvent(this, amount, tracer);
        if (modified <= 0.0 || !Double.isFinite(modified)) {
            return 0.0;
        }
        final double oldHealth = health();
        health = Math.max(oldHealth - modified, 0.0);
        final double min = Math.min(oldHealth, modified);
        try (final var span = tracer.push(new CoreBattleTraceEvents.DamageParticipant(handle(), min))) {
            events().invoker(BasicParticipantEvents.POST_DAMAGE_EVENT_KEY).onPostDamageEvent(this, min, modified - min, span);
        }
        if (health() <= 0) {
            tryKill(tracer);
        }
        return min;
    }

    @Override
    public double heal(final double amount, final BattleTracer.Span<?> tracer) {
        battleState.ensureBattleOngoing();
        final double modified = events().invoker(BasicParticipantEvents.PRE_HEAL_EVENT_KEY).onPreHealEvent(this, amount, tracer);
        if (modified <= 0.0 || !Double.isFinite(modified)) {
            return 0.0;
        }
        final double oldHealth = health;
        health = Math.min(oldHealth + modified, stats().get(Tbcexv4Registries.Stats.MAX_HEALTH));
        final double overflow = Math.max(modified - (health - oldHealth), 0.0);
        final double healed = Math.max(health - oldHealth, 0.0);
        try (final var span = tracer.push(new CoreBattleTraceEvents.HealParticipant(handle(), modified, overflow))) {
            events().invoker(BasicParticipantEvents.POST_HEAL_EVENT_KEY).onPostHealEvent(this, healed, overflow, span);
        }
        return healed;
    }

    @Override
    public double setHealth(final double amount, final BattleTracer.Span<?> tracer) {
        battleState.ensureBattleOngoing();
        final double modified = events().invoker(BasicParticipantEvents.PRE_SET_HEALTH_EVENT_KEY).onPreSetHealthEvent(this, amount, tracer);
        if (!Double.isFinite(modified)) {
            return health();
        }
        final double oldHealth = health();
        health = modified;
        try (final var span = tracer.push(new CoreBattleTraceEvents.ParticipantSetHealth(handle(), oldHealth, health()))) {
            events().invoker(BasicParticipantEvents.POST_SET_HEALTH_EVENT_KEY).onPostSetHealthEvent(this, oldHealth, span);
        }
        if (health() <= 0) {
            tryKill(tracer);
        }
        return health();
    }
}
