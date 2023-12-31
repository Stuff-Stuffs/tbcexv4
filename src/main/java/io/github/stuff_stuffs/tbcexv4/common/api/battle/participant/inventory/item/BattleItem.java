package io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.inventory.item;

import com.mojang.serialization.Codec;
import io.github.stuff_stuffs.tbcexv4.common.api.Tbcexv4Registries;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.BattleCodecContext;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.inventory.equipment.EquipmentSlot;
import net.minecraft.text.Text;

import java.util.Map;
import java.util.Optional;

public interface BattleItem {
    BattleItemType<?> type();

    Text name();

    boolean matches(BattleItem other);

    Optional<BattleItemStack> merge(BattleItemStack first, BattleItemStack second);

    BattleItemRarity rarity();

    static Codec<BattleItem> codec(final BattleCodecContext codecContext) {
        return Tbcexv4Registries.ItemTypes.CODEC.dispatchStable(BattleItem::type, type -> type.codec(codecContext));
    }
}
