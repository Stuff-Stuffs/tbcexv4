package io.github.stuff_stuffs.tbcexv4.common.impl.battle.participant.inventory.equipment;

import io.github.stuff_stuffs.tbcexv4.common.api.Tbcexv4Registries;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.inventory.equipment.EquipmentSlot;
import io.github.stuff_stuffs.tbcexv4.common.internal.Tbcexv4;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class EquipmentSlotImpl implements EquipmentSlot {
    private final Text name;

    public EquipmentSlotImpl(final Text name) {
        this.name = name;
    }

    @Override
    public Text name() {
        return name;
    }

    @Override
    public TagKey<EquipmentSlot> blockedBy() {
        final Identifier id = Tbcexv4Registries.EquipmentSlots.REGISTRY.getId(this);
        if (id == null) {
            throw new RuntimeException("Null equipment slot id! Either unregistered or accessed to early!");
        }
        return TagKey.of(Tbcexv4Registries.EquipmentSlots.KEY, Tbcexv4.id("equipment_slots_blocked_by/" + permute(id)));
    }

    @Override
    public TagKey<EquipmentSlot> blocks() {
        final Identifier id = Tbcexv4Registries.EquipmentSlots.REGISTRY.getId(this);
        if (id == null) {
            throw new RuntimeException("Null equipment slot id! Either unregistered or accessed to early!");
        }
        return TagKey.of(Tbcexv4Registries.EquipmentSlots.KEY, Tbcexv4.id("equipment_slots_blocks/" + permute(id)));
    }

    private static String permute(final Identifier id) {
        return id.getNamespace() + "/" + id.getPath();
    }
}
