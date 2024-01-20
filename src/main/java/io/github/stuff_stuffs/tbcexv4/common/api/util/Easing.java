package io.github.stuff_stuffs.tbcexv4.common.api.util;

public interface Easing {
    static Easing CONSTANT_1 = constant(1);

    double ease(double time);

    default Easing delay(final double offset) {
        return time -> ease(time - offset);
    }

    static Easing constant(final double constant) {
        return t -> constant;
    }

    static Easing from(final EasingFunction function, final double start, final double end) {
        if (end <= start) {
            return CONSTANT_1;
        }
        final double invLength = 1 / (end - start);
        return time -> {
            if (time >= end) {
                return 1;
            }
            if (time <= start) {
                return 0;
            }
            final double t = (time - start) * invLength;
            return function.remap(t);
        };
    }
}
