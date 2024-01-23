package io.github.stuff_stuffs.tbcexv4.common.api.util;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;

import java.lang.ref.WeakReference;
import java.lang.reflect.Array;
import java.util.function.Function;

public final class EventPair<T> {
    private final Class<T> clazz;
    private final Function<T[], T> invokerFactory;
    private final Event<T> event;
    private final WeakEvent<T> weakEvent;

    public EventPair(final Class<T> clazz, final Function<T[], T> invokerFactory, final Function<WeakReference<T>[], T> weakInvokerFactory) {
        this.clazz = clazz;
        this.invokerFactory = invokerFactory;
        event = EventFactory.createArrayBacked(clazz, invokerFactory);
        weakEvent = WeakEvent.create(clazz, weakInvokerFactory);
    }

    public void register(final T listener) {
        event.register(listener);
    }

    public void registerWeak(final T listener) {
        weakEvent.register(listener);
    }

    public T invoker() {
        //noinspection unchecked
        final T[] arr = (T[]) Array.newInstance(clazz, 2);
        arr[0] = event.invoker();
        arr[1] = weakEvent.invoker();
        return invokerFactory.apply(arr);
    }
}
