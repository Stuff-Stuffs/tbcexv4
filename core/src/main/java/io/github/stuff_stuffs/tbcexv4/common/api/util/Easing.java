package io.github.stuff_stuffs.tbcexv4.common.api.util;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public interface Easing {
    Easing CONSTANT_0 = constant(0);
    Easing CONSTANT_1 = constant(1);

    double ease(double time);

    default Easing delay(final double offset) {
        return time -> ease(time - offset);
    }

    final class SimpleEasing implements Easing {
        public static final Codec<SimpleEasing> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                EasingFunction.CODEC.fieldOf("ease").forGetter(o -> o.ease),
                Codec.DOUBLE.fieldOf("start").forGetter(o -> o.start),
                Codec.DOUBLE.fieldOf("end").forGetter(o -> o.end),
                Codec.BOOL.optionalFieldOf("reversed", false).forGetter(o -> o.reversed)
        ).apply(instance, SimpleEasing::new));
        private final EasingFunction ease;
        private final double start;
        private final double end;
        private final boolean reversed;

        public SimpleEasing(final EasingFunction ease, final double start, final double end, final boolean reversed) {
            this.ease = ease;
            this.start = start;
            this.end = end;
            this.reversed = reversed;
        }

        @Override
        public double ease(final double time) {
            double t = (time - start) / (end - start);
            if (reversed) {
                t = t - 1;
            }
            return ease.remap(t);
        }

        @Override
        public SimpleEasing delay(final double offset) {
            return new SimpleEasing(ease, start + offset, end + offset, reversed);
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof final SimpleEasing easing)) {
                return false;
            }

            if (Double.compare(start, easing.start) != 0) {
                return false;
            }
            if (Double.compare(end, easing.end) != 0) {
                return false;
            }
            if (reversed != easing.reversed) {
                return false;
            }
            return ease == easing.ease;
        }

        @Override
        public int hashCode() {
            int result;
            long temp;
            result = ease.hashCode();
            temp = Double.doubleToLongBits(start);
            result = 31 * result + (int) (temp ^ (temp >>> 32));
            temp = Double.doubleToLongBits(end);
            result = 31 * result + (int) (temp ^ (temp >>> 32));
            result = 31 * result + (reversed ? 1 : 0);
            return result;
        }

        @Override
        public String toString() {
            return "SimpleEasing{" +
                    "ease=" + ease +
                    ", start=" + start +
                    ", end=" + end +
                    ", reversed=" + reversed +
                    '}';
        }
    }

    static Easing constant(final double constant) {
        return t -> constant;
    }

    static Easing in(final EasingFunction function, final double start, final double end) {
        if (end <= start) {
            return CONSTANT_1;
        }
        return new SimpleEasing(function, start, end, false);
    }

    static Easing out(final EasingFunction function, final double start, final double end) {
        if (end <= start) {
            return CONSTANT_0;
        }
        return new SimpleEasing(function, start, end, true);
    }
}
