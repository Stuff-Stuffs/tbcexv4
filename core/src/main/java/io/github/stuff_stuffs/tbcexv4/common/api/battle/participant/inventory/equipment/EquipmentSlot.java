package io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.inventory.equipment;

import io.github.stuff_stuffs.tbcexv4.common.impl.battle.participant.inventory.equipment.EquipmentSlotImpl;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.text.Text;

public interface EquipmentSlot {
    Text name();

    TagKey<EquipmentSlot> blockedBy();

    TagKey<EquipmentSlot> blocks();

    static EquipmentSlot create(final Text name) {
        return new EquipmentSlotImpl(name);
    }
}
