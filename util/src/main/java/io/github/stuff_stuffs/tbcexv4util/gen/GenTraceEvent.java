package io.github.stuff_stuffs.tbcexv4util.gen;

import io.github.stuff_stuffs.tbcexv4util.log.BattleLogLevel;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
public @interface GenTraceEvent {
    BattleLogLevel level() default BattleLogLevel.DEBUG;

    String format();
}
