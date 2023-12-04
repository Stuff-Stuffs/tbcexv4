package io.github.stuff_stuffs.tbcexv4.common.impl.battle.participant.inventory.item;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.stuff_stuffs.tbcexv4.common.api.Tbcexv4Registries;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.BattleCodecContext;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.inventory.item.BattleItem;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.inventory.item.BattleItemRarity;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.inventory.item.BattleItemStack;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.inventory.item.BattleItemType;
import io.github.stuff_stuffs.tbcexv4.common.api.util.Tbcexv4Util;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

public class UnknownBattleItem implements BattleItem {
    private static final Codec<UnknownBattleItem> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Registries.ITEM.getCodec().fieldOf("item").forGetter(item -> item.item),
            Tbcexv4Util.NBT_COMPOUND_CODEC.optionalFieldOf("nbt").forGetter(item -> item.nbt),
            Codec.intRange(0, Integer.MAX_VALUE).listOf().xmap(IntOpenHashSet::new, List::copyOf).fieldOf("sourceSlot").forGetter(item -> item.sourceSlots)
    ).apply(instance, UnknownBattleItem::new));
    public static final Function<BattleCodecContext, Codec<UnknownBattleItem>> CODEC_FACTORY = context -> CODEC;
    private final Item item;
    private final Optional<NbtCompound> nbt;
    private final IntOpenHashSet sourceSlots;

    public UnknownBattleItem(final Item item, final Optional<NbtCompound> nbt, final IntOpenHashSet sourceSlots) {
        this.item = item;
        this.nbt = nbt;
        this.sourceSlots = sourceSlots;
    }

    @Override
    public BattleItemType<?> type() {
        return Tbcexv4Registries.ItemTypes.UNKNOWN_BATTLE_ITEM_TYPE;
    }

    @Override
    public Text name() {
        final ItemStack stack = new ItemStack(item, 1);
        nbt.ifPresent(stack::setNbt);
        return item.getName(stack);
    }

    @Override
    public boolean matches(final BattleItem other) {
        if (other instanceof UnknownBattleItem unknown) {
            return unknown.item == item && unknown.nbt.equals(nbt);
        }
        return false;
    }

    @Override
    public Optional<BattleItemStack> merge(final BattleItemStack first, final BattleItemStack second) {
        if (matches(first.item()) && matches(second.item())) {
            final UnknownBattleItem firstItem = (UnknownBattleItem) first.item();
            final UnknownBattleItem secondItem = (UnknownBattleItem) second.item();
            final IntOpenHashSet set = new IntOpenHashSet();
            set.addAll(firstItem.sourceSlots);
            set.addAll(secondItem.sourceSlots);
            return Optional.of(new BattleItemStack(new UnknownBattleItem(firstItem.item, firstItem.nbt, set), first.count() + second.count()));
        }
        return Optional.empty();
    }

    @Override
    public BattleItemRarity rarity() {
        return BattleItemRarity.of(0.0, 0);
    }
}
