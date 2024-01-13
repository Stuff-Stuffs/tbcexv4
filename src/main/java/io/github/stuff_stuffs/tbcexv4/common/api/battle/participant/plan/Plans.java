package io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.plan;

import io.github.stuff_stuffs.tbcexv4.common.api.Tbcexv4Registries;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.BattlePos;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.pathing.Pather;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.state.BattleStateView;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.transaction.BattleTransactionContext;
import it.unimi.dsi.fastutil.ints.Int2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.util.math.random.Random;

import java.util.Iterator;
import java.util.Set;
import java.util.function.Function;

public class Plans {
    public static <M, O> Plan walkPrefix(final BattleStateView state, final Pather.PathCache<M> cache, final Function<Pather.PathNode<M>, Set<O>> acceptablePositions, final Function<Set<O>, Plan> factory, double planWeight) {
        final Int2ObjectMap<Set<O>> map = new Int2ObjectLinkedOpenHashMap<>();
        final Iterator<? extends Pather.PathNode<M>> iterator = cache.all().iterator();
        while (iterator.hasNext()) {
            final Pather.PathNode<M> next = iterator.next();
            final Set<O> set = acceptablePositions.apply(next);
            if (!set.isEmpty()) {
                map.put(Pather.PathCache.pack(next.x(), next.y(), next.z()), set);
            }
        }
        return new PrefixPlan<>(new AbstractTargetChooser<Pather.PathNode<M>, PosTarget>(state) {
            @Override
            public TargetType<PosTarget> type() {
                return Tbcexv4Registries.TargetTypes.POS_TARGET;
            }

            @Override
            public double weight() {
                return 1;
            }

            @Override
            protected Pather.PathNode<M> extract(final PosTarget target) {
                final BattlePos pos = target.pos();
                return cache.get(pos.x(), pos.y(), pos.z());
            }

            @Override
            protected Iterator<Pather.PathNode<M>> iterator() {
                return map.keySet().intStream().mapToObj(i -> cache.get(Pather.PathCache.unpackX(i), Pather.PathCache.unpackY(i), Pather.PathCache.unpackZ(i))).iterator();
            }

            @Override
            protected double weight0(final Pather.PathNode<M> obj, final double temperature, final Random random, final BattleTransactionContext context) {
                return 1 / (obj.cost() * 0.01 + 1);
            }

            @Override
            protected PosTarget create(final Pather.PathNode<M> obj) {
                return new PosTarget(new BattlePos(obj.x(), obj.y(), obj.z()));
            }
        }, target -> {
            final BattlePos pos = target.pos();
            final Set<O> set = map.get(Pather.PathCache.pack(pos.x(), pos.y(), pos.z()));
            if (set == null || set.isEmpty()) {
                throw new RuntimeException();
            }
            return factory.apply(set);
        });
    }
}
