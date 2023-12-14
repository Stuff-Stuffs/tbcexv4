package io.github.stuff_stuffs.tbcexv4.client.api;

import io.github.stuff_stuffs.tbcexv4.client.internal.Tbcexv4Client;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.BattleHandle;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.Set;

public final class Tbcexv4ClientApi {
    public static Optional<BattleHandle> watching() {
        return Optional.ofNullable(Tbcexv4Client.watching());
    }

    public static void requestWatch(@Nullable final BattleHandle handle) {
        Tbcexv4Client.requestWatching(handle);
    }

    public static Set<BattleHandle> possibleControlling() {
        return Tbcexv4Client.possibleControlling();
    }

    private Tbcexv4ClientApi() {
    }
}
