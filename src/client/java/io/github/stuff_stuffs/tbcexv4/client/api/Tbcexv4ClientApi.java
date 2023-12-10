package io.github.stuff_stuffs.tbcexv4.client.api;

import io.github.stuff_stuffs.tbcexv4.client.internal.Tbcexv4Client;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.BattleHandle;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public final class Tbcexv4ClientApi {
    public static Optional<BattleHandle> watching() {
        return Optional.ofNullable(Tbcexv4Client.WATCHING);
    }

    public static void requestWatch(@Nullable final BattleHandle handle) {
        Tbcexv4Client.requestWatching(handle);
    }

    private Tbcexv4ClientApi() {
    }
}
