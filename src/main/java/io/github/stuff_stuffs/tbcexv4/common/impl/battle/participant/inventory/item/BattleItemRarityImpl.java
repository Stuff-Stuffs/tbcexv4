package io.github.stuff_stuffs.tbcexv4.common.impl.battle.participant.inventory.item;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.inventory.item.BattleItemRarity;
import net.minecraft.text.Style;
import net.minecraft.text.Text;

public class BattleItemRarityImpl implements BattleItemRarity {
    public static final Codec<BattleItemRarityImpl> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            RarityClass.CODEC.fieldOf("rarity").forGetter(BattleItemRarityImpl::rarity),
            Codec.intRange(0, Integer.MAX_VALUE).fieldOf("level").forGetter(BattleItemRarityImpl::level)
    ).apply(instance, BattleItemRarityImpl::new));
    private final RarityClass rarityClass;
    private final int level;

    public BattleItemRarityImpl(final RarityClass rarity, final int level) {
        rarityClass = rarity;
        this.level = level;
    }

    @Override
    public RarityClass rarity() {
        return rarityClass;
    }

    @Override
    public int level() {
        return level;
    }

    @Override
    public Text asText() {
        return Text.literal(rarityClass.name() + '(' + level + ')').setStyle(Style.EMPTY.withColor(rarityClass.color() | 0xFF000000));
    }
}
