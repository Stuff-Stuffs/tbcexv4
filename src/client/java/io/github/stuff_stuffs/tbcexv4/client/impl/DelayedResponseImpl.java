package io.github.stuff_stuffs.tbcexv4.client.impl;

import io.github.stuff_stuffs.tbcexv4.client.api.DelayedResponse;

import java.util.concurrent.atomic.AtomicReference;

public class DelayedResponseImpl<T> implements DelayedResponse<T> {
    private final AtomicReference<T> result;

    public DelayedResponseImpl() {
        result = new AtomicReference<>();
    }

    @Override
    public boolean done() {
        return result.getAcquire() != null;
    }

    @Override
    public T value() {
        final T res = result.getAcquire();
        if (res == null) {
            throw new RuntimeException();
        }
        return res;
    }

    public boolean tryComplete(final T val) {
        if (val == null) {
            throw new RuntimeException();
        }
        return result.compareAndExchangeRelease(null, val) == null;
    }
}
