package io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.inventory;

import com.mojang.serialization.Codec;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.BattleParticipantHandle;
import io.github.stuff_stuffs.tbcexv4.common.api.util.Tbcexv4Util;
import io.github.stuff_stuffs.tbcexv4.common.impl.battle.participant.inventory.InventoryImpl;

public interface InventoryHandle {
    Codec<InventoryHandle> CODEC = Tbcexv4Util.implCodec(InventoryImpl.InventoryHandleImpl.CODEC, InventoryImpl.InventoryHandleImpl.class);

    BattleParticipantHandle parent();
}
