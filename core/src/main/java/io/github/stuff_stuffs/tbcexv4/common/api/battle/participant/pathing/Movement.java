package io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.pathing;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringIdentifiable;

import java.util.Locale;

public enum Movement implements StringIdentifiable {
    WALK,
    JUMP,
    FLY,
    SWIM,
    CRAWL,
    TELEPORT,
    FALL;
    public static final Codec<Movement> CODEC = StringIdentifiable.createCodec(Movement::values, id -> id.toLowerCase(Locale.ROOT));

    @Override
    public String asString() {
        return name().toLowerCase(Locale.ROOT);
    }
}
