package io.github.stuff_stuffs.tbcexv4.common.api.util;

public sealed interface Result<T, E> {
    record Success<T, E>(T val) implements Result<T, E> {
    }

    record Failure<T, E>(E error) implements Result<T, E> {
    }
}
