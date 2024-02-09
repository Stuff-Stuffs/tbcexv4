package io.github.stuff_stuffs.tbcexv4.client.impl.render.animation;

import com.mojang.datafixers.util.Unit;
import io.github.stuff_stuffs.tbcexv4.client.api.render.animation.Animation;
import io.github.stuff_stuffs.tbcexv4.client.api.render.animation.AnimationContext;
import io.github.stuff_stuffs.tbcexv4.client.api.render.animation.AnimationQueue;
import io.github.stuff_stuffs.tbcexv4.client.api.render.animation.state.BattleRenderState;
import io.github.stuff_stuffs.tbcexv4.client.impl.render.animation.state.BattleRenderStateImpl;
import io.github.stuff_stuffs.tbcexv4.common.api.util.Result;
import it.unimi.dsi.fastutil.doubles.DoubleAVLTreeSet;
import it.unimi.dsi.fastutil.doubles.DoubleBidirectionalIterator;
import it.unimi.dsi.fastutil.doubles.DoubleSortedSet;

import java.util.List;

public class AnimationQueueImpl implements AnimationQueue {
    private final BattleRenderStateImpl state;
    private final DoubleSortedSet events;
    private long nextId = 0;

    public AnimationQueueImpl() {
        state = new BattleRenderStateImpl();
        events = new DoubleAVLTreeSet();
    }

    public void checkpoint(final double time) {
        events.headSet(time).clear();
        events.add(time);
        state.checkpoint();
    }

    @Override
    public void update(final double time) {
        state.update(time);
    }

    @Override
    public double enqueue(final Animation<BattleRenderState> animation, final double minTime, final double cutoff) {
        final DoubleSortedSet tailSet = events.tailSet(minTime);
        final AnimationContext context = new AnimationContextImpl(nextId++, cutoff);
        if (tailSet.isEmpty()) {
            final Result<List<Animation.TimedEvent>, Unit> setup = animation.animate(minTime, state, context);
            if (setup instanceof Result.Failure<List<Animation.TimedEvent>, Unit>) {
                state.cleanup(context, minTime);
                return Double.NaN;
            }
            final Result.Success<List<Animation.TimedEvent>, Unit> success = (Result.Success<List<Animation.TimedEvent>, Unit>) setup;
            for (final Animation.TimedEvent modifier : success.val()) {
                events.add(modifier.start());
                if (modifier.end() != modifier.start()) {
                    events.add(modifier.end());
                }
            }
            return minTime;
        } else {
            final DoubleBidirectionalIterator iterator = tailSet.iterator();
            while (iterator.hasNext()) {
                final double t = iterator.nextDouble();
                final Result<List<Animation.TimedEvent>, Unit> setup = animation.animate(t, state, context);
                if (setup instanceof final Result.Success<List<Animation.TimedEvent>, Unit> success) {
                    for (final Animation.TimedEvent modifier : success.val()) {
                        events.add(modifier.start());
                        if (modifier.end() != modifier.start()) {
                            events.add(modifier.end());
                        }
                    }
                    return t;
                } else {
                    state.cleanup(context, t);
                }
            }
            return Double.NaN;
        }
    }

    @Override
    public BattleRenderState state() {
        return state;
    }

    private record AnimationContextImpl(long id, double cutoff) implements AnimationContext {
        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof final AnimationContextImpl context)) {
                return false;
            }

            return id == context.id;
        }

        @Override
        public int hashCode() {
            return (int) (id ^ (id >>> 32));
        }
    }
}
