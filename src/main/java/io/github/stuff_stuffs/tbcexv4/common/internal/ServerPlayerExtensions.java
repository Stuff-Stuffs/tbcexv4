package io.github.stuff_stuffs.tbcexv4.common.internal;

import io.github.stuff_stuffs.tbcexv4.common.api.battle.BattleHandle;
import org.jetbrains.annotations.Nullable;

public interface ServerPlayerExtensions {
    @Nullable BattleHandle tbcexv4$watching();

    void tbcev4$setWatching(@Nullable BattleHandle handle);
}
