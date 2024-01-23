package io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.damage;

import io.github.stuff_stuffs.tbcexv4.common.internal.Tbcexv4;
import net.minecraft.registry.Registry;

import java.util.Set;

public interface DamageTypeGraph {
    Node get(DamageType type);

    interface Node {
        DamageType delegate();

        boolean isChildOf(Node node);

        boolean isParentOf(Node node);

        Set<Node> children();

        Set<Node> parents();
    }

    static DamageTypeGraph get(final Registry<DamageType> registry) {
        return Tbcexv4.getCachedDamageGraph(registry);
    }
}
