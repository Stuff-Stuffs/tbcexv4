package io.github.stuff_stuffs.tbcexv4.common.api.battle;

import com.mojang.serialization.Codec;
import net.minecraft.util.Uuids;

import java.util.UUID;

public record BattleHandle(UUID id) {
    public static final Codec<BattleHandle> CODEC = Uuids.STRING_CODEC.xmap(BattleHandle::new, BattleHandle::id);

    public String toFileName() {
        return id.toString() + ".battle";
    }
}
