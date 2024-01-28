package io.github.stuff_stuffs.tbcexv4util.gen;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

@Target({ElementType.TYPE, ElementType.METHOD})
public @interface TracePackage {
    String value();
}
