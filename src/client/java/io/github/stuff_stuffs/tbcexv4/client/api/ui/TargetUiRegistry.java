package io.github.stuff_stuffs.tbcexv4.client.api.ui;

import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.plan.target.Target;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.plan.target.TargetType;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public final class TargetUiRegistry {
    private static final Map<TargetType<?>, TargetUi<?>> REGISTRY = new Reference2ObjectOpenHashMap<>();

    public static <T extends Target> void register(final TargetType<T> type, final TargetUi<T> ui) {
        if (REGISTRY.putIfAbsent(type, ui) != null) {
            throw new RuntimeException();
        }
    }

    public static <T extends Target> @Nullable TargetUi<T> get(final TargetType<T> type) {
        //noinspection unchecked
        return (TargetUi<T>) REGISTRY.get(type);
    }
}
