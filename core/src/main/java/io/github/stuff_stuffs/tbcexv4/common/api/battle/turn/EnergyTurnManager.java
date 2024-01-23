package io.github.stuff_stuffs.tbcexv4.common.api.battle.turn;

import com.mojang.datafixers.util.Unit;
import io.github.stuff_stuffs.tbcexv4.common.api.Tbcexv4Registries;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.action.ActionSource;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.action.BattleAction;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.action.core.NoopBattleAction;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.BattleParticipantHandle;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.state.BattleState;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.tracer.BattleTracer;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.transaction.BattleTransactionContext;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.transaction.DeltaSnapshotParticipant;
import io.github.stuff_stuffs.tbcexv4.common.api.util.Result;
import io.github.stuff_stuffs.tbcexv4.common.generated_events.BasicEvents;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

public class EnergyTurnManager extends DeltaSnapshotParticipant<EnergyTurnManager.State> implements TurnManager {
    private List<BattleParticipantHandle> ordered;
    private Set<BattleParticipantHandle> containing;
    private int index;
    private int remainingEnergy;
    private Function<BattleParticipantHandle, Integer> maxEnergy;

    public EnergyTurnManager() {
        ordered = new ArrayList<>();
        containing = new ObjectOpenHashSet<>();
    }

    @Override
    public Set<BattleParticipantHandle> currentTurn() {
        if (ordered.isEmpty()) {
            return Set.of();
        }
        return Set.of(ordered.get(index));
    }

    @Override
    public Result<Unit, Text> check(final BattleAction action) {
        final Result<Unit, Text> result = TurnManager.super.check(action);
        if (result instanceof final Result.Success<Unit, Text> success) {
            if (action.source().isEmpty()) {
                return result;
            }
            return action.source().get().energy() <= remainingEnergy ? success : new Result.Failure<>(Text.of("Not enough energy!"));
        }
        return result;
    }

    @Override
    public boolean checkAi(final List<BattleAction> actions) {
        int energy = 0;
        for (final BattleAction action : actions) {
            final Optional<ActionSource> opt = action.source();
            if (opt.isPresent()) {
                final ActionSource source = opt.get();
                energy += source.energy();
                if (!currentTurn().contains(source.actor())) {
                    return false;
                }
            }
        }
        return energy <= remainingEnergy;
    }

    @Override
    public BattleAction skipTurn(final BattleParticipantHandle handle) {
        return new NoopBattleAction(Optional.of(handle), remainingEnergy);
    }

    @Override
    public void setup(final BattleState state, final BattleTransactionContext context, final BattleTracer.Span<?> trace) {
        state.events().registerView(BasicEvents.POST_ADD_PARTICIPANT_EVENT_KEY, (stateView, participant, transactionContext, span) -> onParticipantJoin(participant.handle(), transactionContext), context);
        state.events().registerView(BasicEvents.POST_REMOVE_PARTICIPANT_EVENT_KEY, (stateView, handle, reason, transactionContext, span) -> onParticipantLeave(handle, transactionContext), context);
        maxEnergy = handle -> state.participant(handle).stats().get(Tbcexv4Registries.Stats.MAX_ENERGY);
    }

    private void onParticipantJoin(final BattleParticipantHandle handle, final BattleTransactionContext transactionContext) {
        delta(transactionContext, new State(new ObjectArrayList<>(ordered), new ObjectOpenHashSet<>(containing), index, remainingEnergy));
        if (containing.add(handle)) {
            ordered.add(handle);
            if (ordered.size() == 1) {
                remainingEnergy = maxEnergy.apply(handle);
            }
        }
    }

    private void onParticipantLeave(final BattleParticipantHandle participant, final BattleTransactionContext transactionContext) {
        delta(transactionContext, new State(new ObjectArrayList<>(ordered), new ObjectOpenHashSet<>(containing), index, remainingEnergy));
        containing.remove(participant);
        final int handleIndex = ordered.indexOf(participant);
        if (handleIndex == -1) {
            throw new RuntimeException();
        }
        ordered.remove(handleIndex);
        final boolean removedCurrent = handleIndex == index;
        if (handleIndex <= index) {
            index--;
            if (index < 0) {
                index = Math.max(index, ordered.size() - 1);
            }
        }
        if (!ordered.isEmpty() && removedCurrent) {
            remainingEnergy = maxEnergy.apply(ordered.get(index));
        }
    }

    @Override
    public void onAction(final int energy, final BattleParticipantHandle source, final BattleState state, final BattleTransactionContext context, final BattleTracer.Span<?> trace) {
        delta(context, new State(new ObjectArrayList<>(ordered), new ObjectOpenHashSet<>(containing), index, remainingEnergy));
        remainingEnergy = Math.max(remainingEnergy - energy, 0);
        if (ordered.isEmpty()) {
            index = 0;
        } else if (remainingEnergy == 0) {
            index = (index + 1) % ordered.size();
            remainingEnergy = maxEnergy.apply(ordered.get(index));
        }
    }

    @Override
    protected void revertDelta(final State delta) {
        ordered = delta.ordered;
        containing = delta.containing;
        index = delta.index;
        remainingEnergy = delta.energy;
    }

    public record State(List<BattleParticipantHandle> ordered, Set<BattleParticipantHandle> containing, int index,
                        int energy) {
    }
}
