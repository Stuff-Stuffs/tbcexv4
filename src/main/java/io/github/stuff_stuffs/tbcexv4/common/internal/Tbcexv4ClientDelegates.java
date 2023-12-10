package io.github.stuff_stuffs.tbcexv4.common.internal;

import io.github.stuff_stuffs.tbcexv4.common.api.battle.state.env.BattleEnvironment;
import io.github.stuff_stuffs.tbcexv4.common.internal.world.BattleEnvironmentInitialState;

import java.util.function.BiConsumer;

public final class Tbcexv4ClientDelegates {
    public static BiConsumer<BattleEnvironmentInitialState, BattleEnvironment> SETUP_CLIENT_ENV_DELEGATE = (action, env) -> {
    };

    private Tbcexv4ClientDelegates() {
    }
}
