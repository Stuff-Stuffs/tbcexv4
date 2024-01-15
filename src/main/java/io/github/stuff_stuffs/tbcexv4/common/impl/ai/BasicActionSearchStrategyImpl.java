package io.github.stuff_stuffs.tbcexv4.common.impl.ai;

import com.mojang.datafixers.util.Unit;
import io.github.stuff_stuffs.tbcexv4.common.api.Tbcexv4Registries;
import io.github.stuff_stuffs.tbcexv4.common.api.ai.ActionSearchStrategy;
import io.github.stuff_stuffs.tbcexv4.common.api.ai.Scorer;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.BattlePhase;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.action.BattleAction;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.log.BattleLogContext;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.BattleParticipant;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.BattleParticipantPhase;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.inventory.InventoryView;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.inventory.equipment.Equipment;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.inventory.equipment.EquipmentSlot;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.inventory.item.BattleItemStack;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.plan.Plan;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.plan.Target;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.plan.TargetType;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.state.BattleState;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.tracer.BattleTracer;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.transaction.BattleTransaction;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.transaction.BattleTransactionContext;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.turn.TurnManager;
import it.unimi.dsi.fastutil.HashCommon;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.math.random.Xoroshiro128PlusPlusRandom;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class BasicActionSearchStrategyImpl implements ActionSearchStrategy {
    private static final int MAX_DEPTH = 4;
    private static final double EPS = 0.000001;
    private final double temperature;

    public BasicActionSearchStrategyImpl(final double temperature) {
        this.temperature = temperature;
    }

    @Override
    public Optional<List<BattleAction>> search(final TurnManager turnManager, final BattleParticipant participant, final Scorer scorer, final BattleTracer tracer, final BattleTransactionContext context, final long seed, final CompletableFuture<CompletableFuture<Unit>> cancellation) {
        CompletableFuture<Unit> cancellationFuture = cancellation.getNow(null);
        if (cancellationFuture != null) {
            cancellationFuture.complete(Unit.INSTANCE);
            return Optional.empty();
        }
        final BattleState state = participant.battleState();
        final double base = scorer.score(participant.battleState(), tracer);
        final Random random = new Xoroshiro128PlusPlusRandom(seed, HashCommon.murmurHash3(seed + HashCommon.murmurHash3(seed + 1)));
        final List<BattleAction>[] actions = iter0(participant, scorer, random, state, context, tracer, base);
        List<BattleAction> chosen = null;
        double wSum = 0;
        for (final List<BattleAction> list : actions) {
            if (!turnManager.checkAi(list)) {
                continue;
            }
            try (final BattleTransaction transaction = context.openNested()) {
                boolean failed = false;
                for (final BattleAction action : list) {
                    if (!action.apply(state, transaction, tracer, BattleLogContext.DISABLED)) {
                        failed = true;
                        break;
                    }
                }
                if(failed) {
                    continue;
                }
                final double score = iter(participant, scorer, random, state, transaction, tracer, base, 1, cancellation);
                cancellationFuture = cancellation.getNow(null);
                if (cancellationFuture != null) {
                    cancellationFuture.complete(Unit.INSTANCE);
                    return Optional.empty();
                }
                transaction.abort();
                final double weight = Math.exp(score / temperature) + EPS;
                if (chosen == null) {
                    chosen = list;
                    wSum = weight;
                } else {
                    wSum = wSum + weight;
                    final double fraction = weight / wSum;
                    if (random.nextDouble() <= fraction) {
                        chosen = list;
                    }
                }
            }

        }
        return Optional.ofNullable(chosen);
    }

    private double iter(final BattleParticipant participant, final Scorer scorer, final Random random, final BattleState state, final BattleTransactionContext context, final BattleTracer tracer, final double baseScore, final int depth, final CompletableFuture<CompletableFuture<Unit>> cancellation) {
        if (depth >= MAX_DEPTH) {
            return baseScore;
        }
        if (cancellation.isDone()) {
            return 0;
        }
        final List<BattleAction>[] requests = iter0(participant, scorer, random, state, context, tracer, baseScore);
        double best = 0;
        for (final List<BattleAction> list : requests) {
            try (final BattleTransaction transaction = context.openNested()) {
                boolean failed = false;
                for (final BattleAction action : list) {
                    if (!action.apply(state, transaction, tracer, BattleLogContext.DISABLED)) {
                        failed = true;
                        break;
                    }
                }
                if(failed) {
                    continue;
                }
                final double score = scorer.score(state, tracer);
                best = Math.max(best, iter(participant, scorer, random, state, transaction, tracer, score, depth + 1, cancellation));
                transaction.abort();
            }
        }
        return best;
    }

    private List<BattleAction>[] iter0(final BattleParticipant participant, final Scorer scorer, final Random random, final BattleState state, final BattleTransactionContext context, final BattleTracer tracer, final double baseScore) {
        if (participant.phase() == BattleParticipantPhase.FINISHED || participant.battleState().phase() == BattlePhase.FINISHED) {
            return new List[0];
        }
        final List<List<BattleAction>> requests = new ArrayList<>();
        gather(participant, plan -> {
            final Node root = new Node(null);
            final List<Target> stack = new ArrayList<>(8);
            for (int i = 0; i < 8; i++) {
                stack.clear();
                int tries = 0;
                Node current = root;
                boolean lastFound = false;
                Plan localPlan = plan;
                while (!localPlan.canBuild() && tries++ < 8) {
                    double weightSum = 0;
                    final Set<TargetType<?>> types = localPlan.targetTypes();
                    if (types.isEmpty()) {
                        break;
                    }
                    for (final TargetType<?> type : types) {
                        weightSum = weightSum + localPlan.ofType(type).weight();
                    }
                    final double chosen = random.nextDouble() * weightSum;
                    double s = 0;
                    TargetType<?> chosenType = null;
                    for (final Iterator<TargetType<?>> iterator = types.iterator(); iterator.hasNext(); ) {
                        final TargetType<?> type = iterator.next();
                        s = s + localPlan.ofType(type).weight();
                        if (chosen < s || !iterator.hasNext()) {
                            chosenType = type;
                        }
                    }
                    final Target chosenTarget = localPlan.ofType(chosenType).choose(temperature, random, context);
                    lastFound = false;
                    for (final Node child : current.children) {
                        if (child.target.equals(chosenTarget)) {
                            current = child;
                            lastFound = true;
                            break;
                        }
                    }
                    if (!lastFound) {
                        final Node e = new Node(chosenTarget);
                        current.children.add(e);
                        current = e;
                    }
                    localPlan = localPlan.addTarget(chosenTarget);
                }
                if (localPlan.canBuild() && !lastFound) {
                    requests.add(localPlan.build());
                }
            }
        });
        if (requests.isEmpty()) {
            return new List[0];
        }
        final int size = requests.size();
        final int chosenCount = Math.min(size, 3);
        final List<BattleAction>[] chosenActions = new List[chosenCount];
        double wSum = 0;
        int idx = 0;
        for (int i = 0; i < size; i++) {
            try (final BattleTransaction transaction = context.openNested()) {
                final List<BattleAction> actions = requests.get(i);
                boolean failed = false;
                for (final BattleAction extracted : actions) {
                    if (!extracted.apply(state, transaction, tracer, BattleLogContext.DISABLED)) {
                        failed = true;
                        break;
                    }
                }
                if(failed) {
                    continue;
                }
                final double score = scorer.score(state, tracer);
                transaction.abort();
                final double weight = Math.exp((score - baseScore) / temperature) + EPS - 1;
                if (weight <= 0.0) {
                    continue;
                }
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
        if (idx != chosenActions.length) {
            return Arrays.copyOf(chosenActions, idx);
        }
        return chosenActions;
    }

    private void gather(final BattleParticipant participant, final Consumer<Plan> consumer) {
        Tbcexv4Registries.DefaultPlans.forEach(participant, consumer);
        for (final InventoryView.InventoryEntryView entry : participant.inventory().entries()) {
            final Optional<BattleItemStack> stack = entry.stack();
            if (stack.isPresent()) {
                stack.get().item().actions(participant, entry.handle(), consumer);
            }
        }
        for (final EquipmentSlot slot : Tbcexv4Registries.EquipmentSlots.REGISTRY) {
            final Optional<? extends Equipment> equipment = participant.inventory().equipment(slot);
            if (equipment.isPresent()) {
                equipment.get().actions(participant, slot, consumer);
            }
        }
    }

    private static final class Node {
        private final Target target;
        private final List<Node> children;

        private Node(final Target target) {
            this.target = target;
            children = new ArrayList<>();
        }
    }
}
