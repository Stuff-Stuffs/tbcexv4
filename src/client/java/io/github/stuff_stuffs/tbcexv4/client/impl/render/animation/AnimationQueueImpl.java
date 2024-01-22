package io.github.stuff_stuffs.tbcexv4.client.impl.render.animation;

import com.mojang.datafixers.util.Unit;
import io.github.stuff_stuffs.tbcexv4.client.api.render.animation.Animation;
import io.github.stuff_stuffs.tbcexv4.client.api.render.animation.AnimationContext;
import io.github.stuff_stuffs.tbcexv4.client.api.render.animation.AnimationQueue;
import io.github.stuff_stuffs.tbcexv4.client.api.render.animation.state.BattleRenderState;
import io.github.stuff_stuffs.tbcexv4.client.impl.render.animation.state.BattleRenderStateImpl;
import io.github.stuff_stuffs.tbcexv4.common.api.util.Result;
import it.unimi.dsi.fastutil.objects.ObjectAVLTreeSet;

import java.util.List;
import java.util.SortedSet;

public class AnimationQueueImpl implements AnimationQueue {
    private final BattleRenderStateImpl state;
    private final SortedSet<EventEntry> events;
    private long nextId = 0;

    public AnimationQueueImpl() {
        state = new BattleRenderStateImpl();
        events = new ObjectAVLTreeSet<>();
    }

    @Override
    public double enqueue(final Animation<BattleRenderState> animation, final double minTime, final double cutoff) {
        final EventEntry entry = new EventEntry(minTime);
        final SortedSet<EventEntry> tailSet = events.tailSet(entry);
        final AnimationContext context = new AnimationContextImpl(nextId++, cutoff);
        if (tailSet.isEmpty()) {
            final Result<List<Animation.TimedEvent>, Unit> setup = animation.animate(minTime, state, context);
            if (setup instanceof Result.Failure<List<Animation.TimedEvent>, Unit>) {
                state.cleanup(context, minTime);
                return Double.NaN;
            }
            final Result.Success<List<Animation.TimedEvent>, Unit> success = (Result.Success<List<Animation.TimedEvent>, Unit>) setup;
            for (final Animation.TimedEvent modifier : success.val()) {
                events.add(new EventEntry(modifier.start()));
                if (modifier.end() != modifier.start()) {
                    events.add(new EventEntry(modifier.end()));
                }
            }
            return minTime;
        } else {
            for (final EventEntry eventEntry : tailSet) {
                final double t = eventEntry.time;
                final Result<List<Animation.TimedEvent>, Unit> setup = animation.animate(t, state, context);
                if (setup instanceof final Result.Success<List<Animation.TimedEvent>, Unit> success) {
                    for (final Animation.TimedEvent modifier : success.val()) {
                        events.add(new EventEntry(modifier.start()));
                        if (modifier.end() != modifier.start()) {
                            events.add(new EventEntry(modifier.end()));
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

    private record EventEntry(double time) implements Comparable<EventEntry> {
        @Override
        public int compareTo(final EventEntry o) {
            return Double.compare(time, o.time);
        }
    }

    private record AnimationContextImpl(long id, double cutoff) implements AnimationContext {
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof AnimationContextImpl context)) return false;

            return id == context.id;
        }

        @Override
        public int hashCode() {
            return (int) (id ^ (id >>> 32));
        }
    }
}
