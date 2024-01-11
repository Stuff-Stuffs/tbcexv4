package io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.plan;

import net.minecraft.text.Text;

public interface TargetType<T extends Target> {
    Text name();

    Text description(T target);
}
