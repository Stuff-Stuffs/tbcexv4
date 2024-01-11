package io.github.stuff_stuffs.tbcexv4.common.api.battle.state.attachment;

import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.BattleParticipantView;
import net.minecraft.text.Text;

import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Function;

public final class BattleAttachmentType<V, T extends BattleAttachment> {
    private final BiFunction<BattleParticipantView, T, Text> name;
    private final BiPredicate<BattleParticipantView, T> visible;
    private final Function<T, ? extends V> viewExtractor;

    public BattleAttachmentType(final BiFunction<BattleParticipantView, T, Text> name, final BiPredicate<BattleParticipantView, T> visible, final Function<T, ? extends V> viewExtractor) {
        this.name = name;
        this.visible = visible;
        this.viewExtractor = viewExtractor;
    }

    public Text name(final BattleParticipantView participant, final T value) {
        return name.apply(participant, value);
    }

    public boolean visible(final BattleParticipantView participant, final T value) {
        return visible.test(participant, value);
    }

    public V view(final T mut) {
        return viewExtractor.apply(mut);
    }
}
