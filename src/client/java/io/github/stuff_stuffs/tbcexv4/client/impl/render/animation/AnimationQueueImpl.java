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
        this.state = new BattleRenderStateImpl();
        events = new ObjectAVLTreeSet<>();
    }

    @Override
    public double enqueue(final Animation<BattleRenderState> animation, final double minTime, final double cutoff) {
        final long id = nextId++;
        final EventEntry entry = new EventEntry(minTime, id);
        final SortedSet<EventEntry> tailSet = events.tailSet(entry);
        final AnimationContext context = new AnimationContextImpl(id, cutoff);
        if (tailSet.isEmpty()) {
            final Result<List<Animation.TimedEvent>, Unit> setup = animation.animate(minTime, state, context);
            if (setup instanceof Result.Failure<List<Animation.TimedEvent>, Unit>) {
                state.cleanup(context);
                return Double.NaN;
            }
            final Result.Success<List<Animation.TimedEvent>, Unit> success = (Result.Success<List<Animation.TimedEvent>, Unit>) setup;
            for (final Animation.TimedEvent modifier : success.val()) {
                events.add(new EventEntry(modifier.start(), id));
                events.add(new EventEntry(modifier.end(), id));
            }
            return minTime;
        } else {
            for (final EventEntry eventEntry : tailSet) {
                final double t = eventEntry.time;
                final Result<List<Animation.TimedEvent>, Unit> setup = animation.animate(t, state, context);
                if (setup instanceof final Result.Success<List<Animation.TimedEvent>, Unit> success) {
                    for (final Animation.TimedEvent modifier : success.val()) {
                        events.add(new EventEntry(modifier.start(), id));
                        events.add(new EventEntry(modifier.end(), id));
                    }
                    return t;
                } else {
                    state.cleanup(context);
                }
            }
            return Double.NaN;
        }
    }

    @Override
    public BattleRenderState state() {
        return state;
    }

    private record EventEntry(double time, long id) implements Comparable<EventEntry> {
        @Override
        public int compareTo(final EventEntry o) {
            final int c = Double.compare(time, o.time);
            if(c!=0) {
                return c;
            }
            return Long.compare(id, o.id);
        }
    }

    private record AnimationContextImpl(long id, double cutoff) implements AnimationContext {
    }
}
