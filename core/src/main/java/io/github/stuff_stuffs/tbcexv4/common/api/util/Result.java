package io.github.stuff_stuffs.tbcexv4.common.api.util;

import com.mojang.datafixers.util.Unit;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

public sealed interface Result<T, E> {
    record Success<T, E>(T val) implements Result<T, E> {
    }

    record Failure<T, E>(E error) implements Result<T, E> {
    }

    static <T, E> Success<T, E> success(final T val) {
        return new Success<>(val);
    }

    static <T, E> Failure<T, E> failure(final E error) {
        return new Failure<>(error);
    }

    default <T0> Result<T0, E> mapSuccess(final Function<T, T0> function) {
        if (this instanceof final Result.Success<T, E> success) {
            return success(function.apply(success.val));
        } else {
            //noinspection unchecked
            return (Result<T0, E>) this;
        }
    }

    default <E0> Result<T, E0> mapFailure(final Function<E, E0> function) {
        if (this instanceof final Result.Failure<T, E> failure) {
            return failure(function.apply(failure.error));
        } else {
            //noinspection unchecked
            return (Result<T, E0>) this;
        }
    }

    default <T0, E0> Result<T0, E0> map(final Function<T, T0> successMap, final Function<E, E0> failureMap) {
        return mapSuccess(successMap).mapFailure(failureMap);
    }

    default <T0> Result<T0, E> flatmapSuccess(final Function<T, Result<T0, E>> function) {
        if (this instanceof final Result.Success<T, E> success) {
            return function.apply(success.val);
        }
        //noinspection unchecked
        return (Result<T0, E>) this;
    }

    default <E0> Result<T, E0> flatmapFailure(final Function<E, Result<T, E0>> function) {
        if (this instanceof final Result.Failure<T, E> failure) {
            return function.apply(failure.error);
        }
        //noinspection unchecked
        return (Result<T, E0>) this;
    }

    default <T0, E0> Result<T0, E0> flatmap(final Function<T, Result<T0, E0>> successMap, final Function<E, Result<T0, E0>> failureMap) {
        if (this instanceof final Result.Success<T, E> success) {
            return successMap.apply(success.val);
        }
        final Failure<T, E> failure = (Failure<T, E>) this;
        return failureMap.apply(failure.error);
    }

    default <T0, T1> Result<T1, E> mergeSuccess(final Result<T0, E> other, final BiFunction<T, T0, T1> combiner, final BiFunction<E, E, E> errorCombiner) {
        if (this instanceof final Result.Success<T, E> s0) {
            if (other instanceof final Result.Success<T0, E> s1) {
                return success(combiner.apply(s0.val, s1.val));
            }
            return failure(((Failure<T0, E>) other).error);
        } else {
            final E e0 = ((Failure<T, E>) this).error;
            if (other instanceof Result.Success<T0, E>) {
                return failure(e0);
            }
            return failure(errorCombiner.apply(e0, ((Failure<T0, E>) other).error));
        }
    }

    default <T0> Result<T, E> fold(final Result<T0, E> other, final BiFunction<T, T0, T> combiner, final BiFunction<E, E, E> errorCombiner) {
        if (this instanceof final Result.Success<T, E> s0) {
            if (other instanceof final Result.Success<T0, E> s1) {
                return success(combiner.apply(s0.val, s1.val));
            }
            return failure(((Failure<T0, E>) other).error);
        } else {
            final E e0 = ((Failure<T, E>) this).error;
            if (other instanceof Result.Success<T0, E>) {
                return failure(e0);
            }
            return failure(errorCombiner.apply(e0, ((Failure<T0, E>) other).error));
        }
    }

    default <T0> Folder<T, T0, E> folder(final BiFunction<T, T0, T> combiner, final BiFunction<E, E, E> errorCombiner) {
        return new Folder<>(combiner, errorCombiner, this);
    }

    static <T> Folder<List<T>, T, Unit> folder(final Result<List<T>, Unit> container) {
        return container.folder((ts, t) -> {
            final List<T> newList = new ArrayList<>(ts.size() + 1);
            newList.addAll(ts);
            newList.add(t);
            return newList;
        }, (u0, u1) -> Unit.INSTANCE);
    }

    static <T> Folder<List<T>, T, Unit> mutableFold() {
        return Result.<List<T>, Unit>success(new ArrayList<>()).folder((ts, t) -> {
            ts.add(t);
            return ts;
        }, (u0, u1) -> Unit.INSTANCE);
    }

    static <T> Folder<List<T>, List<T>, Unit> mutableListFold() {
        return Result.<List<T>, Unit>success(new ArrayList<>()).folder((ts, t) -> {
            ts.addAll(t);
            return ts;
        }, (u0, u1) -> Unit.INSTANCE);
    }

    final class Folder<T, T0, E> {
        private final BiFunction<T, T0, T> combiner;
        private final BiFunction<E, E, E> errorCombiner;
        private Result<T, E> result;

        private Folder(final BiFunction<T, T0, T> combiner, final BiFunction<E, E, E> errorCombiner, final Result<T, E> result) {
            this.combiner = combiner;
            this.errorCombiner = errorCombiner;
            this.result = result;
        }

        public Folder<T, T0, E> accept(final Result<T0, E> result) {
            this.result = this.result.fold(result, combiner, errorCombiner);
            return this;
        }

        public Folder<T, T0, E> acceptRaw(T0 val) {
            this.result = this.result.fold(success(val), combiner, errorCombiner);
            return this;
        }

        public Result<T, E> get() {
            return result;
        }
    }
}
