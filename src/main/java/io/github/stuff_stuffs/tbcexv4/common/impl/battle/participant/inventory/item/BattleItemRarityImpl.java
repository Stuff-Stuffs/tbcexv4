package io.github.stuff_stuffs.tbcexv4.common.impl.battle.participant.inventory.item;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.inventory.item.BattleItemRarity;
import net.minecraft.text.Style;
import net.minecraft.text.Text;

public class BattleItemRarityImpl implements BattleItemRarity {
    private static final RarityClass[] RARITY_CLASSES = RarityClass.values();
    public static final Codec<BattleItemRarityImpl> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.pair(RarityClass.CODEC, Codec.doubleRange(0.0, Double.MAX_VALUE)).comapFlatMap(pair -> {
                if (check(pair.getFirst(), pair.getSecond())) {
                    return DataResult.success(pair);
                }
                return DataResult.error(() -> "Invalid BattleItemRarity class/progress pair!");
            }, pair -> pair).fieldOf("classAndProgress").forGetter(rarity -> Pair.of(rarity.rarityClass(), rarity.progress)),
            Codec.intRange(0, Integer.MAX_VALUE).fieldOf("level").forGetter(BattleItemRarityImpl::level)
    ).apply(instance, BattleItemRarityImpl::new));
    private final RarityClass rarityClass;
    private final double progress;
    private final int level;

    public BattleItemRarityImpl(final Pair<RarityClass, Double> pair, final int level) {
        rarityClass = pair.getFirst();
        progress = pair.getSecond();
        if (!check(rarityClass, progress)) {
            throw new RuntimeException();
        }
        this.level = level;
    }

    public BattleItemRarityImpl(final RarityClass rarityClass, final double progress, final int level) {
        this.rarityClass = rarityClass;
        this.progress = progress;
        if (!check(rarityClass, progress)) {
            throw new RuntimeException();
        }
        this.level = level;
    }

    public static BattleItemRarity of(final double rarity, final int level) {
        if (rarity < 0) {
            throw new RuntimeException("Negative rarity!");
        }
        for (int i = RARITY_CLASSES.length - 1; i >= 0; i--) {
            if (RARITY_CLASSES[i].start() <= rarity) {
                if (i != RARITY_CLASSES.length - 1) {
                    final double progress = (rarity - RARITY_CLASSES[i].start()) / (RARITY_CLASSES[i + 1].start() - RARITY_CLASSES[i].start());
                    return new BattleItemRarityImpl(RARITY_CLASSES[i], Math.min(Math.max(progress, 0.0), Math.nextDown(1.0)), level);
                } else {
                    final double progress = (rarity - RARITY_CLASSES[i].start()) / RARITY_CLASSES[i].start();
                    return new BattleItemRarityImpl(RARITY_CLASSES[i], progress, level);
                }
            }
        }
        throw new AssertionError();
    }

    private static boolean check(final RarityClass rarityClass, final double progress) {
        return progress >= 0 && !(rarityClass != RarityClass.LEGENDARY && progress >= 1.0);
    }

    @Override
    public RarityClass rarityClass() {
        return rarityClass;
    }

    @Override
    public double progressToNextClass() {
        return progress;
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
