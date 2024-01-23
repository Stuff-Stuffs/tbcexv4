package io.github.stuff_stuffs.tbcexv4.common.api.battle;

import net.minecraft.registry.DynamicRegistryManager;

public interface BattleCodecContext {
    DynamicRegistryManager dynamicRegistryManager();

    static BattleCodecContext create(final DynamicRegistryManager manager) {
        return () -> manager;
    }
}
