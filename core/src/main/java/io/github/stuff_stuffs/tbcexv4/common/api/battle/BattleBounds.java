package io.github.stuff_stuffs.tbcexv4.common.api.battle;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.BattleParticipantBounds;
import net.minecraft.util.Util;

import java.util.Arrays;
import java.util.stream.IntStream;

public record BattleBounds(int x0, int y0, int z0, int x1, int y1, int z1) {
    public static final Codec<BattleBounds> CODEC = Codec.INT_STREAM.comapFlatMap(s -> Util.decodeFixedLengthArray(s, 6).flatMap(BattleBounds::tryCreate), bounds -> IntStream.of(bounds.x0, bounds.y0, bounds.z0, bounds.x1, bounds.y1, bounds.z1));

    public BattleBounds(final int x0, final int y0, final int z0, final int x1, final int y1, final int z1) {
        if (!check(x0, y0, z0, x1, y1, z1)) {
            throw new RuntimeException();
        }
        this.x0 = Math.min(x0, x1);
        this.y0 = Math.min(y0, y1);
        this.z0 = Math.min(z0, z1);
        this.x1 = Math.max(x0, x1);
        this.y1 = Math.max(y0, y1);
        this.z1 = Math.max(z0, z1);
    }

    public boolean check(final BattleParticipantBounds bounds, final BattlePos pos) {
        if (pos.x() + 0.5 - bounds.width() * 0.5 < x0 || pos.y() < y0 || pos.z() + 0.5 - bounds.width() * 0.5 < z0) {
            return false;
        }
        if (pos.x() + 0.5 + bounds.width() * 0.5 > (x1 - 1) || pos.y() + bounds.height() > (y1 - 1) || pos.z() + 0.5 + bounds.width() * 0.5 > (z1 - 1)) {
            return false;
        }
        return true;
    }

    private static DataResult<BattleBounds> tryCreate(final int[] arr) {
        if (check(arr[0], arr[1], arr[2], arr[3], arr[4], arr[5])) {
            return DataResult.success(new BattleBounds(arr[0], arr[1], arr[2], arr[3], arr[4], arr[5]));
        }
        return DataResult.error(() -> "Invalid BattleBounds! " + Arrays.toString(arr));
    }

    public static boolean check(final int x0, final int y0, final int z0, final int x1, final int y1, final int z1) {
        if (x0 == x1 || y0 == y1 || z0 == z1) {
            return false;
        }
        return x0 >= 0 && x1 >= 0 && y0 >= 0 && y1 >= 0 && z0 >= 0 && z1 >= 0;
    }
}
