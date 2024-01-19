package io.github.stuff_stuffs.tbcexv4.client.api.render.animation.state;

import io.github.stuff_stuffs.tbcexv4.client.api.render.animation.AnimationContext;

import java.util.Optional;

public interface RenderState {
    String LOCK_ID = "lock";

    <T> Optional<Property<T>> getProperty(String id, PropertyType<T> type);

    <T> Property<T> getOrCreateProperty(String id, PropertyType<T> type, T defaultValue);

    void addEvent(Event event, double time, AnimationContext context);

    void clearEvents(AnimationContext context);

    interface LiftingPredicate<T extends RenderState, I> {
        boolean test(T state, I id, AnimationContext context);
    }

    interface Event {
        void apply();

        void undo();
    }
}
