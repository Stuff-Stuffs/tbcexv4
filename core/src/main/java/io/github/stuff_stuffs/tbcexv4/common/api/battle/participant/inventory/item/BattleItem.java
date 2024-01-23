package io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.inventory.item;

import com.mojang.serialization.Codec;
import io.github.stuff_stuffs.tbcexv4.common.api.Tbcexv4Registries;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.BattleCodecContext;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.BattleParticipantView;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.inventory.InventoryHandle;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.inventory.equipment.Equipment;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.inventory.equipment.EquipmentSlot;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.plan.Plan;
import net.minecraft.text.Text;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

public interface BattleItem {
    BattleItemType<?> type();

    Text name();

    BattleItemRarity rarity();

    List<Text> description();

    default void actions(final BattleParticipantView participant, final InventoryHandle handle, final Consumer<Plan> consumer) {
    }

    default Optional<Equipment> equipmentForSlot(final EquipmentSlot slot, final BattleParticipantView participant) {
        return Optional.empty();
    }

    static Codec<BattleItem> codec(final BattleCodecContext codecContext) {
        return Tbcexv4Registries.ItemTypes.CODEC.dispatchStable(BattleItem::type, type -> type.codec(codecContext));
    }
}
