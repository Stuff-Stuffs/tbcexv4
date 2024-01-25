package io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.plan;

import io.github.stuff_stuffs.tbcexv4.common.api.Tbcexv4Registries;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.BattlePos;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.BattleParticipantView;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.pathing.Pather;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.plan.target.AbstractTargetChooser;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.plan.target.PathTarget;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.plan.target.TargetType;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.state.BattleStateView;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.transaction.BattleTransactionContext;
import it.unimi.dsi.fastutil.ints.Int2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import net.minecraft.util.math.random.Random;

import java.util.Iterator;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.ToDoubleFunction;

public class Plans {
    public static void acceptPlayer(final BattleParticipantView participant, final Plan plan, final Consumer<Plan> consumer) {
        if (participant.attachmentView(Tbcexv4Registries.BattleParticipantAttachmentTypes.PLAYER_CONTROLLED).isPresent()) {
            consumer.accept(plan);
        }
    }

    public static void acceptAi(final BattleParticipantView participant, final Plan plan, final Consumer<Plan> consumer) {
        if (participant.attachmentView(Tbcexv4Registries.BattleParticipantAttachmentTypes.AI_CONTROLLER).isPresent()) {
            consumer.accept(plan);
        }
    }

    public static <O> Optional<Plan> pathPrefix(final BattleStateView state, final Pather.Paths cache, final Function<Pather.PathNode, Set<O>> acceptablePositions, final BiFunction<Pather.PathNode, Set<O>, Plan> factory, final ToDoubleFunction<O> weights, final double planWeight, final PlanType type) {
        final Int2ObjectMap<Set<O>> map = new Int2ObjectLinkedOpenHashMap<>();
        final Iterator<? extends Pather.PathNode> iterator = cache.all().iterator();
        while (iterator.hasNext()) {
            final Pather.PathNode next = iterator.next();
            final Set<O> set = acceptablePositions.apply(next);
            if (set != null && !set.isEmpty()) {
                BattlePos pos = next.pos();
                map.put(Pather.Paths.pack(pos.x(), pos.y(), pos.z()), set);
            }
        }
        final IntSet terminals = new IntOpenHashSet(map.size() / 4 + 4);
        final Iterator<? extends Pather.PathNode> tIterator = cache.terminal().iterator();
        while (tIterator.hasNext()) {
            final Pather.PathNode next = tIterator.next();
            BattlePos pos = next.pos();
            final int key = Pather.Paths.pack(pos.x(), pos.y(), pos.z());
            if (map.containsKey(key)) {
                terminals.add(key);
            }
        }

        if (map.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new PrefixPlan<>(new AbstractTargetChooser<Pather.PathNode, PathTarget>(state) {
            @Override
            public TargetType<PathTarget> type() {
                return Tbcexv4Registries.TargetTypes.PATH_TARGET;
            }

            @Override
            public double weight() {
                return planWeight;
            }

            @Override
            protected Iterator<Pather.PathNode> iterator() {
                return map.keySet().intStream().mapToObj(i -> cache.get(Pather.Paths.unpackX(i), Pather.Paths.unpackY(i), Pather.Paths.unpackZ(i))).iterator();
            }

            @Override
            protected double weight0(final Pather.PathNode obj, final double temperature, final Random random, final BattleTransactionContext context) {
                BattlePos pos = obj.pos();
                final int key = Pather.Paths.pack(pos.x(), pos.y(), pos.z());
                final Set<O> set = map.get(key);
                if (set == null || set.isEmpty()) {
                    return 0;
                }
                double s = 0;
                for (final O o : set) {
                    s += weights.applyAsDouble(o);
                }
                return s;
            }

            @Override
            protected PathTarget create(final Pather.PathNode obj) {
                BattlePos pos = obj.pos();
                final int key = Pather.Paths.pack(pos.x(), pos.y(), pos.z());
                return new PathTarget(obj, terminals.contains(key));
            }
        }, target -> {
            final BattlePos pos = target.node().pos();
            final Set<O> set = map.get(Pather.Paths.pack(pos.x(), pos.y(), pos.z()));
            if (set == null || set.isEmpty()) {
                throw new RuntimeException();
            }
            return factory.apply(target.node(), set);
        }, type));
    }
}
