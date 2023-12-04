package io.github.stuff_stuffs.tbcexv4.common.api.battle;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record BattlePos(int x, int y, int z) {
    public static final Codec<BattlePos> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.intRange(0, Integer.MAX_VALUE).fieldOf("x").forGetter(BattlePos::x),
            Codec.intRange(0, Integer.MAX_VALUE).fieldOf("y").forGetter(BattlePos::y),
            Codec.intRange(0, Integer.MAX_VALUE).fieldOf("z").forGetter(BattlePos::z)
    ).apply(
            instance,
            BattlePos::new
    ));
}
