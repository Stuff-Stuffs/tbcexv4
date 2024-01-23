package io.github.stuff_stuffs.tbcexv4.common.api.util;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringIdentifiable;

import java.util.Locale;

public enum EasingFunction implements StringIdentifiable {
    LINEAR {
        @Override
        public double remap(final double t) {
            return t;
        }
    },
    IN_QUADRATIC {
        @Override
        public double remap(final double t) {
            return t * t;
        }
    },
    OUT_QUADRATIC {
        @Override
        public double remap(final double t) {
            final double t1 = 1 - t;
            return 1 - t1 * t1;
        }
    },
    IN_OUT_QUADRATIC {
        @Override
        public double remap(final double t) {
            if (t < 0.5) {
                return t * t * 2;
            }
            final double t1 = 1 - t;
            return 1 - (2 * t1 * t1);
        }
    },
    STEP {
        @Override
        public double remap(final double t) {
            return t > 0.5 ? 1 : 0;
        }
    };
    public static final Codec<EasingFunction> CODEC = StringIdentifiable.createCodec(EasingFunction::values);

    public abstract double remap(double t);

    @Override
    public String asString() {
        return name().toLowerCase(Locale.ROOT);
    }
}
