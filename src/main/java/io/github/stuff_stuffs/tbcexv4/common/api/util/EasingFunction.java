package io.github.stuff_stuffs.tbcexv4.common.api.util;

public enum EasingFunction {
    LINEAR {
        @Override
        public double remap(final double t) {
            return t;
        }
    };

    public abstract double remap(double t);
}
