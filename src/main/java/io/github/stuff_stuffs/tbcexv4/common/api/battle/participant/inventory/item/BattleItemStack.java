package io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.inventory.item;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.BattleCodecContext;

public record BattleItemStack(BattleItem item, int count) {
    public BattleItemStack {
        if (count < 1) {
            throw new RuntimeException();
        }
    }

    public static Codec<BattleItemStack> codec(final BattleCodecContext context) {
        return RecordCodecBuilder.create(instance -> instance.group(
                BattleItem.codec(context).fieldOf("item").forGetter(BattleItemStack::item),
                Codec.intRange(1, Integer.MAX_VALUE).fieldOf("count").forGetter(BattleItemStack::count)
        ).apply(instance, BattleItemStack::new));
    }
}
