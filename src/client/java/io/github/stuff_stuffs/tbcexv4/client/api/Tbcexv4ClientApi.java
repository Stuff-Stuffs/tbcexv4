package io.github.stuff_stuffs.tbcexv4.client.api;

import io.github.stuff_stuffs.tbcexv4.client.internal.Tbcexv4Client;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.BattleHandle;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.action.request.BattleActionRequest;
import net.minecraft.text.Text;
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

    public static DelayedResponse<RequestResult> sendRequest(final BattleActionRequest request) {
        return Tbcexv4Client.sendRequest(request);
    }

    public sealed interface RequestResult {
        Text description();

        boolean success();
    }

    public record SuccessfulRequestResult(Text description) implements RequestResult {
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
