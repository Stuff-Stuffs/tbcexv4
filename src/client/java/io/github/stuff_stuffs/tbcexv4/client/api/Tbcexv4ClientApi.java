package io.github.stuff_stuffs.tbcexv4.client.api;

import io.github.stuff_stuffs.tbcexv4.client.internal.Tbcexv4Client;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.BattleHandle;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.BattleView;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.action.BattleAction;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.BattleParticipantHandle;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.Set;

public final class Tbcexv4ClientApi {
    public static Optional<BattleHandle> watching() {
        return Optional.ofNullable(Tbcexv4Client.watching());
    }

    public static Optional<BattleParticipantHandle> controlling() {
        return Optional.ofNullable(Tbcexv4Client.controlling());
    }

    public static Set<BattleParticipantHandle> possibleControlling() {
        return Tbcexv4Client.possibleControlling();
    }

    public static boolean tryControl(final BattleParticipantHandle handle) {
        return Tbcexv4Client.tryControl(handle);
    }

    public static Optional<BattleView> watched() {
        return Optional.ofNullable(Tbcexv4Client.watched());
    }

    public static void requestWatch(@Nullable final BattleHandle handle) {
        Tbcexv4Client.requestWatching(handle);
    }

    public static Set<BattleHandle> possibleWatching() {
        return Tbcexv4Client.possibleWatching();
    }

    public static DelayedResponse<RequestResult> sendRequest(final BattleAction request) {
        return Tbcexv4Client.sendRequest(request);
    }

    public sealed interface RequestResult {
        boolean success();
    }

    public record SuccessfulRequestResult() implements RequestResult {
        @Override
        public boolean success() {
            return true;
        }
    }

    public record FailedRequestResult(Text description) implements RequestResult {
        @Override
        public boolean success() {
            return false;
        }
    }

    private Tbcexv4ClientApi() {
    }
}
