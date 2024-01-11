package io.github.stuff_stuffs.tbcexv4.common.impl.battle.participant.inventory.item;

import com.mojang.datafixers.FunctionType;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.stuff_stuffs.tbcexv4.common.api.Tbcexv4Registries;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.BattleCodecContext;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.inventory.item.BattleItem;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.inventory.item.BattleItemRarity;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.inventory.item.BattleItemType;
import io.github.stuff_stuffs.tbcexv4.common.api.util.Tbcexv4Util;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.ints.IntSets;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

public class UnknownBattleItem implements BattleItem {
    private static final Codec<UnknownBattleItem> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Registries.ITEM.getCodec().fieldOf("item").forGetter(item -> item.item),
            Tbcexv4Util.NBT_COMPOUND_CODEC.optionalFieldOf("nbt").forGetter(item -> item.nbt),
            Codec.intRange(0, Integer.MAX_VALUE).listOf().xmap((FunctionType<List<Integer>, IntSet>) IntOpenHashSet::new, List::copyOf).fieldOf("sourceSlot").forGetter(item -> item.sourceSlots)
    ).apply(instance, UnknownBattleItem::new));
    public static final Function<BattleCodecContext, Codec<UnknownBattleItem>> CODEC_FACTORY = context -> CODEC;
    public final Item item;
    public final Optional<NbtCompound> nbt;
    public final IntSet sourceSlots;
    private @Nullable ItemStack renderStack = null;

    public UnknownBattleItem(final Item item, final Optional<NbtCompound> nbt, final IntSet sourceSlots) {
        this.item = item;
        this.nbt = nbt;
        this.sourceSlots = IntSets.unmodifiable(sourceSlots);
    }

    public ItemStack renderStack() {
        if (renderStack == null) {
            renderStack = new ItemStack(item);
            nbt.ifPresent(nbt -> renderStack.setNbt(nbt));
        }
        return renderStack;
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
    public BattleItemRarity rarity() {
        return BattleItemRarity.of(BattleItemRarity.RarityClass.JUNK, 0);
    }

    @Override
    public List<Text> description() {
        return List.of(Text.of("Unknown or unrecognized item!"));
    }
}
