package io.github.stuff_stuffs.tbcexv4.common.api.battle.participant;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record BattleParticipantBounds(double width, double height) {
    public static final Codec<BattleParticipantBounds> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.doubleRange(Double.MIN_NORMAL, 16.0).fieldOf("width").forGetter(BattleParticipantBounds::width),
            Codec.doubleRange(Double.MIN_NORMAL, 16.0).fieldOf("height").forGetter(BattleParticipantBounds::height)
    ).apply(
            instance,
            BattleParticipantBounds::new
    ));

    public BattleParticipantBounds {
        if (!check(width, height)) {
            throw new RuntimeException();
        }
    }

    private boolean check(final double width, final double height) {
        return width >= Double.MIN_NORMAL && height >= Double.MIN_NORMAL && width <= 16 && height <= 16;
    }
}
