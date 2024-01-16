package io.github.stuff_stuffs.tbcexv4.common.api.util;

import org.jetbrains.annotations.Nullable;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public interface WeakEvent<T> {
    T invoker();

    void register(T listener);

    static <T> WeakEvent<T> create(final Class<T> clazz, final Function<WeakReference<T>[], T> invokerFactory) {
        return new WeakEvent<>() {
            private final List<WeakReference<T>> events = new ArrayList<>();
            private @Nullable T invoker = null;

            @Override
            public T invoker() {
                if (events.removeIf(ref -> ref.get() == null)) {
                    invoker = null;
                }
                if (invoker == null) {
                    final WeakReference<T>[] array = events.toArray(WeakReference[]::new);
                    invoker = invokerFactory.apply(array);
                }
                return invoker;
            }

            @Override
            public void register(final T listener) {
                events.removeIf(ref -> ref.get() == null);
                events.add(new WeakReference<>(listener));
                invoker = null;
            }
        };
    }
}
