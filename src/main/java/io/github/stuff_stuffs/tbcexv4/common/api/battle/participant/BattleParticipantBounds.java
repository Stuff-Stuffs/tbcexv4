package io.github.stuff_stuffs.tbcexv4.common.api.battle.participant;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.BattlePos;
import net.minecraft.util.math.Box;

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

    public Box asBox(final double x, final double y, final double z) {
        return new Box(
                x + 0.5 - width * 0.5,
                y,
                z + 0.5 - width * 0.5,
                x + 0.5 + width * 0.5,
                y + height,
                z + 0.5 + width * 0.5
        );
    }

    private boolean check(final double width, final double height) {
        return width >= Double.MIN_NORMAL && height >= Double.MIN_NORMAL && width <= 16 && height <= 16;
    }

    public static double distance(final BattleParticipantBounds firstBounds, final BattlePos firstPos, final BattleParticipantBounds secondBounds, final BattlePos secondPos) {
        return Math.sqrt(distance2(firstBounds, firstPos, secondBounds, secondPos));
    }

    public static double distance2(final BattleParticipantBounds firstBounds, final BattlePos firstPos, final BattleParticipantBounds secondBounds, final BattlePos secondPos) {
        final double dx;
        if (firstPos.x() < secondPos.x()) {
            dx = Math.max((secondPos.x() + 0.5 - 0.5 * secondBounds.width()) - (firstPos.x() + 0.5 + 0.5 * firstBounds.width()), 0);
        } else if (firstPos.x() > secondPos.x()) {
            dx = Math.max((firstPos.x() + 0.5 - 0.5 * firstBounds.width()) - (secondPos.x() + 0.5 + 0.5 * secondBounds.width()), 0);
        } else {
            dx = 0;
        }

        final double dz;
        if (firstPos.z() < secondPos.z()) {
            dz = Math.max((secondPos.z() + 0.5 - 0.5 * secondBounds.width()) - (firstPos.z() + 0.5 + 0.5 * firstBounds.width()), 0);
        } else if (firstPos.z() > secondPos.z()) {
            dz = Math.max((firstPos.z() + 0.5 - 0.5 * firstBounds.width()) - (secondPos.z() + 0.5 + 0.5 * secondBounds.width()), 0);
        } else {
            dz = 0;
        }

        final double dy;
        if (firstPos.y() < secondPos.y()) {
            dy = Math.max(secondPos.y() - (firstPos.y() + firstBounds.height()), 0);
        } else if (firstPos.y() > secondPos.y()) {
            dy = Math.max(firstPos.y() - (secondPos.y() + secondBounds.height()), 0);
        } else {
            dy = 0;
        }
        return dx * dx + dy * dy + dz * dz;
    }

}
