package io.github.stuff_stuffs.tbcexv4.common.api.battle.turn;

import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.BattleParticipantHandle;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.state.BattleState;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.tracer.BattleTracer;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.transaction.BattleTransactionContext;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.transaction.DeltaSnapshotParticipant;
import io.github.stuff_stuffs.tbcexv4.common.generated_events.BasicEvents;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class InOrderTurnManager extends DeltaSnapshotParticipant<InOrderTurnManager.State> implements TurnManager {
    private List<BattleParticipantHandle> ordered;
    private Set<BattleParticipantHandle> containing;
    private int index;

    public InOrderTurnManager() {
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
    public void setup(final BattleState state, final BattleTransactionContext context, final BattleTracer.Span<?> trace) {
        state.events().registerView(BasicEvents.POST_ADD_PARTICIPANT_EVENT_KEY, (stateView, participant, transactionContext, span) -> onParticipantJoin(participant.handle(), transactionContext), context);
        state.events().registerView(BasicEvents.POST_REMOVE_PARTICIPANT_EVENT_KEY, (stateView, handle, reason, transactionContext, span) -> onParticipantLeave(handle, transactionContext), context);
    }

    private void onParticipantJoin(final BattleParticipantHandle handle, final BattleTransactionContext transactionContext) {
        delta(transactionContext, new State(new ObjectArrayList<>(ordered), new ObjectOpenHashSet<>(containing), index));
        if (containing.add(handle)) {
            ordered.add(handle);
        }
    }

    private void onParticipantLeave(final BattleParticipantHandle participant, final BattleTransactionContext transactionContext) {
        delta(transactionContext, new State(new ObjectArrayList<>(ordered), new ObjectOpenHashSet<>(containing), index));
        containing.remove(participant);
        final int handleIndex = ordered.indexOf(participant);
        if (handleIndex == -1) {
            throw new RuntimeException();
        }
        ordered.remove(handleIndex);
        if (handleIndex <= index) {
            index--;
        }
        index = Math.min(Math.max(index, 0), ordered.size());
    }

    @Override
    public void onAction(final BattleParticipantHandle source, final BattleState state, final BattleTransactionContext context, final BattleTracer.Span<?> trace) {
        delta(context, new State(new ObjectArrayList<>(ordered), new ObjectOpenHashSet<>(containing), index));
        if (ordered.isEmpty()) {
            index = 0;
        } else {
            index = (index + 1) % ordered.size();
        }
    }

    @Override
    protected void revertDelta(final State delta) {
        ordered = delta.ordered;
        containing = delta.containing;
        index = delta.index;
    }

    public record State(List<BattleParticipantHandle> ordered, Set<BattleParticipantHandle> containing, int index) {
    }
}
