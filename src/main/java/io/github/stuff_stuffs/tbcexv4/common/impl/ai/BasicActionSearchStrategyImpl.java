package io.github.stuff_stuffs.tbcexv4.common.impl.ai;

import io.github.stuff_stuffs.tbcexv4.common.api.Tbcexv4Registries;
import io.github.stuff_stuffs.tbcexv4.common.api.ai.ActionSearchStrategy;
import io.github.stuff_stuffs.tbcexv4.common.api.ai.Scorer;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.action.BattleAction;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.action.request.BattleActionRequest;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.action.request.BattleActionRequestType;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.BattleParticipant;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.inventory.InventoryView;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.inventory.item.BattleItemStack;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.plan.Plan;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.plan.TargetType;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.state.BattleState;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.tracer.BattleTracer;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.transaction.BattleTransaction;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.transaction.BattleTransactionContext;
import it.unimi.dsi.fastutil.HashCommon;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.math.random.Xoroshiro128PlusPlusRandom;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

public class BasicActionSearchStrategyImpl implements ActionSearchStrategy {
    private static final int MAX_DEPTH = 4;
    private static final double EPS = 0.000001;
    private final double temperature;

    public BasicActionSearchStrategyImpl(final double temperature) {
        this.temperature = temperature;
    }

    @Override
    public Optional<BattleActionRequest> search(final BattleParticipant participant, final Scorer scorer, final BattleTracer tracer, final BattleTransactionContext context, final long seed) {
        final BattleState state = participant.battleState();
        final double base = scorer.score(participant.battleState(), tracer);
        final Random random = new Xoroshiro128PlusPlusRandom(seed, HashCommon.murmurHash3(seed + HashCommon.murmurHash3(seed)));
        final BattleActionRequest[] requests = iter0(participant, scorer, random, state, context, tracer, base);
        BattleActionRequest chosen = null;
        double wSum = 0;
        for (final BattleActionRequest request : requests) {
            try (final BattleTransaction transaction = context.openNested()) {
                final BattleAction extracted = extract(request.type(), request);
                extracted.apply(state, transaction, tracer);
                final double score = iter(participant, scorer, random, state, transaction, tracer, base, 1);
                transaction.abort();
                final double weight = Math.exp(-(1 / score) / temperature) + EPS;
                if (chosen == null) {
                    chosen = request;
                    wSum = weight;
                } else {
                    wSum = wSum + weight;
                    final double fraction = weight / wSum;
                    if (random.nextDouble() <= fraction) {
                        chosen = request;
                    }
                }
            }

        }
        return Optional.ofNullable(chosen);
    }

    private double iter(final BattleParticipant participant, final Scorer scorer, final Random random, final BattleState state, final BattleTransactionContext context, final BattleTracer tracer, final double baseScore, final int depth) {
        if (depth >= MAX_DEPTH) {
            return baseScore;
        }
        final BattleActionRequest[] requests = iter0(participant, scorer, random, state, context, tracer, baseScore);
        double best = 0;
        for (final BattleActionRequest request : requests) {
            try (final BattleTransaction transaction = context.openNested()) {
                final BattleAction extracted = extract(request.type(), request);
                extracted.apply(state, transaction, tracer);
                final double score = scorer.score(state, tracer);
                best = Math.max(best, iter(participant, scorer, random, state, context, tracer, score, depth + 1));
                transaction.abort();
            }
        }
        return best;
    }

    private BattleActionRequest[] iter0(final BattleParticipant participant, final Scorer scorer, final Random random, final BattleState state, final BattleTransactionContext context, final BattleTracer tracer, final double baseScore) {
        final List<BattleActionRequest> requests = new ArrayList<>();
        gather(participant, plan -> {
            for (int i = 0; i < 8; i++) {
                int tries = 0;
                while (!plan.canBuild() && tries++ < 8) {
                    double weightSum = 0;
                    for (final TargetType<?> type : plan.targetTypes()) {
                        weightSum = weightSum + plan.ofType(type).weight();
                    }
                    final double chosen = random.nextDouble() * weightSum;
                    double s = 0;
                    TargetType<?> chosenType = null;
                    for (final Iterator<TargetType<?>> iterator = plan.targetTypes().iterator(); iterator.hasNext(); ) {
                        final TargetType<?> type = iterator.next();
                        s = s + plan.ofType(type).weight();
                        if (chosen < s || !iterator.hasNext()) {
                            chosenType = type;
                        }
                    }
                    plan = plan.addTarget(plan.ofType(chosenType).choose(temperature, random, context));
                }
                if (plan.canBuild()) {
                    requests.add(plan.build());
                }
            }
        });
        if (requests.isEmpty()) {
            return new BattleActionRequest[0];
        }
        final int size = requests.size();
        final int chosenCount = Math.min(size, 3);
        final BattleActionRequest[] chosenActions = new BattleActionRequest[chosenCount];
        double wSum = 0;
        int idx = 0;
        for (int i = 0; i < size; i++) {
            final BattleActionRequest request = requests.get(i);
            try (final BattleTransaction transaction = context.openNested()) {
                final BattleAction extracted = extract(request.type(), request);
                extracted.apply(state, transaction, tracer);
                final double score = scorer.score(state, tracer);
                transaction.abort();
                final double weight = Math.exp(-1 / (score - baseScore) / temperature) + EPS;
                if (idx < chosenCount) {
                    chosenActions[idx++] = requests.get(i);
                    wSum = wSum + weight;
                } else {
                    wSum = wSum + weight;
                    final double fraction = weight / wSum;
                    if (random.nextDouble() <= fraction) {
                        chosenActions[random.nextInt(chosenCount)] = requests.get(i);
                    }
                }
            }
        }
        return chosenActions;
    }

    private static <T extends BattleActionRequest> BattleAction extract(final BattleActionRequestType<T> type, final BattleActionRequest request) {
        return type.extract((T) request);
    }

    private void gather(final BattleParticipant participant, final Consumer<Plan> consumer) {
        Tbcexv4Registries.DefaultPlans.forEach(participant, consumer);
        for (final InventoryView.InventoryEntryView entry : participant.inventory().entries()) {
            final Optional<BattleItemStack> stack = entry.stack();
            if (stack.isPresent()) {
                stack.get().item().actions(participant, entry.handle(), consumer);
            }
        }
    }
}
