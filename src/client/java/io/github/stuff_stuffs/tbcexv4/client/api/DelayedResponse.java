package io.github.stuff_stuffs.tbcexv4.client.api;

import io.github.stuff_stuffs.tbcexv4.client.impl.DelayedResponseImpl;

public interface DelayedResponse<T> {
    boolean done();

    T value();

    static <T> boolean tryComplete(final DelayedResponse<T> response, final T val) {
        return ((DelayedResponseImpl<T>) response).tryComplete(val);
    }

    static <T> DelayedResponse<T> create() {
        return new DelayedResponseImpl<>();
    }
}
