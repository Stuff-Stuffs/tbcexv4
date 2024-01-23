package io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.inventory.item;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import io.github.stuff_stuffs.tbcexv4.common.api.util.Tbcexv4Util;
import io.github.stuff_stuffs.tbcexv4.common.impl.battle.participant.inventory.item.BattleItemRarityImpl;
import net.minecraft.text.Text;

public interface BattleItemRarity {
    Codec<BattleItemRarity> CODEC = Tbcexv4Util.implCodec(BattleItemRarityImpl.CODEC, BattleItemRarityImpl.class);

    RarityClass rarity();

    int level();

    Text asText();

    default BattleItemRarity withLevel(final int level) {
        if (level == level()) {
            return this;
        }
        return of(rarity(), level);
    }

    static BattleItemRarity of(final RarityClass rarity, final int level) {
        return new BattleItemRarityImpl(rarity, level);
    }

    enum RarityClass {
        JUNK(0, 0x696A6A),//grey
        COMMON(100.0, 0xFFFFFF),//white
        UNCOMMON(10_000.0, 0xd18f9c),//pink
        RARE(1_000_000.0, 0x00cc66),//green
        EPIC(100_000_000.0, 0xE80000),//red
        LEGENDARY(10_000_000_000.0, 0xFBF236);//gold

        public static final Codec<RarityClass> CODEC = Codec.STRING.comapFlatMap(RarityClass::fromDynamic, Enum::name);
        private final double start;
        private final int color;

        RarityClass(final double start, final int color) {
            this.start = start;
            this.color = color;
        }

        public double start() {
            return start;
        }

        public int color() {
            return color;
        }

        private static DataResult<RarityClass> fromDynamic(final String s) {
            return switch (s) {
                case "JUNK" -> DataResult.success(JUNK);
                case "COMMON" -> DataResult.success(COMMON);
                case "UNCOMMON" -> DataResult.success(UNCOMMON);
                case "RARE" -> DataResult.success(RARE);
                case "EPIC" -> DataResult.success(EPIC);
                case "LEGENDARY" -> DataResult.success(LEGENDARY);
                default -> DataResult.error(() -> "No RarityClass with name: " + s);
            };
        }
    }
}
